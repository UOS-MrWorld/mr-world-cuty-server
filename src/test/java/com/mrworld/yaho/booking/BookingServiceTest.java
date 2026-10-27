package com.mrworld.yaho.booking;

import com.mrworld.yaho.booking.dto.BookingRequest;
import com.mrworld.yaho.booking.dto.BookingResponse;
import com.mrworld.yaho.booking.dto.QuoteRequest;
import com.mrworld.yaho.booking.dto.QuoteResponse;
import com.mrworld.yaho.common.enums.BookingStatus;
import com.mrworld.yaho.common.enums.HotelGrade;
import com.mrworld.yaho.common.enums.LoyaltyGrade;
import com.mrworld.yaho.common.enums.MealType;
import com.mrworld.yaho.common.enums.Role;
import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourLevel;
import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.common.enums.Transport;
import com.mrworld.yaho.common.exception.ConflictException;
import com.mrworld.yaho.common.exception.NotFoundException;
import com.mrworld.yaho.customer.Customer;
import com.mrworld.yaho.customer.CustomerRepository;
import com.mrworld.yaho.member.Member;
import com.mrworld.yaho.tour.DrinkOption;
import com.mrworld.yaho.tour.DrinkOptionRepository;
import com.mrworld.yaho.tour.Tour;
import com.mrworld.yaho.tour.TourRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private TourRepository tourRepository;

    @Mock
    private DrinkOptionRepository drinkOptionRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void 가격_계산은_기본가와_옵션_추가금을_인원만큼_곱해_합산한다() {
        when(tourRepository.findById(1L)).thenReturn(Optional.of(openTour(Theme.GOLF)));
        when(customerRepository.findById(7L)).thenReturn(Optional.empty());

        QuoteRequest request = quoteRequest(TourLevel.GRAND, 2, Transport.PREMIUM_VAN_10);
        request.setHotelGrade(HotelGrade.FOUR_STAR);
        request.setMealType(MealType.LOCAL_RESTAURANT);

        QuoteResponse response = bookingService.getQuote(1L, 7L, request);

        // 옵션 1인 추가금 = 호텔 100,000 + 식사 50,000 + 교통 50,000
        assertThat(response.getBasePrice()).isEqualTo(1_000_000);
        assertThat(response.getOptionPrice()).isEqualTo(400_000);
        assertThat(response.getTotalPrice()).isEqualTo(1_400_000);
        assertThat(response.getDiscountRate()).isZero();
        assertThat(response.getExpectedPaymentAmount()).isEqualTo(1_400_000);
    }

    @Test
    void 음료는_수량만큼만_더하고_인원수는_곱하지_않는다() {
        when(tourRepository.findById(1L)).thenReturn(Optional.of(openTour(Theme.GOLF)));
        when(customerRepository.findById(7L)).thenReturn(Optional.empty());
        when(drinkOptionRepository.findAllById(List.of(3L))).thenReturn(List.of(drinkOption(3L, 5_000)));

        QuoteRequest request = quoteRequest(TourLevel.CLASSIC, 2, Transport.PREMIUM_VAN_10);
        request.setDrinkOptions(List.of(drinkItem(3L, 4)));

        QuoteResponse response = bookingService.getQuote(1L, 7L, request);

        // 옵션 = (0 + 0 + 50,000) * 2 + 5,000 * 4
        assertThat(response.getOptionPrice()).isEqualTo(120_000);
    }

    @Test
    void 할인이_적용된_고객은_등급_할인율로_예상_결제_금액을_계산한다() {
        when(tourRepository.findById(1L)).thenReturn(Optional.of(openTour(Theme.GOLF)));
        Customer customer = Customer.create(member(7L));
        ReflectionTestUtils.setField(customer, "loyaltyGrade", LoyaltyGrade.VIP);
        ReflectionTestUtils.setField(customer, "loyaltyDiscountApplied", true);
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer));

        QuoteResponse response = bookingService.getQuote(
                1L, 7L, quoteRequest(TourLevel.CLASSIC, 2, Transport.PREMIUM_VAN_10));

        assertThat(response.getTotalPrice()).isEqualTo(1_100_000);
        assertThat(response.getDiscountRate()).isEqualTo(10);
        assertThat(response.getExpectedPaymentAmount()).isEqualTo(990_000);
    }

    @Test
    void 할인이_적용되지_않은_고객은_할인율이_0이다() {
        when(tourRepository.findById(1L)).thenReturn(Optional.of(openTour(Theme.GOLF)));
        Customer customer = Customer.create(member(7L));
        ReflectionTestUtils.setField(customer, "loyaltyGrade", LoyaltyGrade.VIP);
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer));

        QuoteResponse response = bookingService.getQuote(
                1L, 7L, quoteRequest(TourLevel.CLASSIC, 2, Transport.PREMIUM_VAN_10));

        assertThat(response.getDiscountRate()).isZero();
    }

    @Test
    void 가격_계산_대상_투어가_없으면_예외가_발생한다() {
        when(tourRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getQuote(
                99L, 7L, quoteRequest(TourLevel.CLASSIC, 2, Transport.PREMIUM_VAN_10)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void 모집_중이_아닌_투어는_가격_계산할_수_없다() {
        Tour tour = openTour(Theme.GOLF);
        ReflectionTestUtils.setField(tour, "tourStatus", TourStatus.CONFIRMED);
        when(tourRepository.findById(1L)).thenReturn(Optional.of(tour));

        assertThatThrownBy(() -> bookingService.getQuote(
                1L, 7L, quoteRequest(TourLevel.CLASSIC, 2, Transport.PREMIUM_VAN_10)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void 시작_7일_전_마감일은_예약_가능하고_이후는_마감이다() {
        Tour lastDay = openTour(Theme.GOLF);
        ReflectionTestUtils.setField(lastDay, "startDate", LocalDate.now().plusDays(7));
        when(tourRepository.findById(1L)).thenReturn(Optional.of(lastDay));
        when(customerRepository.findById(7L)).thenReturn(Optional.empty());

        QuoteRequest request = quoteRequest(TourLevel.CLASSIC, 2, Transport.PREMIUM_VAN_10);
        assertThat(bookingService.getQuote(1L, 7L, request).getTotalPrice()).isPositive();

        Tour closed = openTour(Theme.GOLF);
        ReflectionTestUtils.setField(closed, "startDate", LocalDate.now().plusDays(6));
        when(tourRepository.findById(2L)).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> bookingService.getQuote(2L, 7L, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 허니문_힐링은_클래식_등급을_선택할_수_없다() {
        when(tourRepository.findById(1L)).thenReturn(Optional.of(openTour(Theme.HEALING)));

        assertThatThrownBy(() -> bookingService.getQuote(
                1L, 7L, quoteRequest(TourLevel.CLASSIC, 2, Transport.PREMIUM_VAN_10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 허니문은_2명만_가능하다() {
        when(tourRepository.findById(1L)).thenReturn(Optional.of(openTour(Theme.HONEYMOON)));

        assertThatThrownBy(() -> bookingService.getQuote(
                1L, 7L, quoteRequest(TourLevel.GRAND, 4, Transport.PREMIUM_VAN_10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 세_명_이상은_2인_차량을_선택할_수_없다() {
        when(tourRepository.findById(1L)).thenReturn(Optional.of(openTour(Theme.GOLF)));

        assertThatThrownBy(() -> bookingService.getQuote(
                1L, 7L, quoteRequest(TourLevel.CLASSIC, 3, Transport.PREMIUM_CAR_2)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 존재하지_않는_음료_옵션은_예외가_발생한다() {
        when(tourRepository.findById(1L)).thenReturn(Optional.of(openTour(Theme.GOLF)));
        when(drinkOptionRepository.findAllById(List.of(99L))).thenReturn(List.of());

        QuoteRequest request = quoteRequest(TourLevel.CLASSIC, 2, Transport.PREMIUM_VAN_10);
        request.setDrinkOptions(List.of(drinkItem(99L, 1)));

        assertThatThrownBy(() -> bookingService.getQuote(1L, 7L, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 예약은_PENDING으로_저장되고_금액과_할인율을_스냅샷한다() {
        Tour tour = openTour(Theme.GOLF);
        when(tourRepository.findById(1L)).thenReturn(Optional.of(tour));
        Customer customer = Customer.create(member(7L));
        ReflectionTestUtils.setField(customer, "loyaltyGrade", LoyaltyGrade.VIP);
        ReflectionTestUtils.setField(customer, "loyaltyDiscountApplied", true);
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer));
        when(drinkOptionRepository.findAllById(List.of(3L))).thenReturn(List.of(drinkOption(3L, 5_000)));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "bookingId", 101L);
            return saved;
        });

        BookingRequest request = bookingRequest(1L, TourLevel.CLASSIC, 2, Transport.PREMIUM_VAN_10);
        request.setDrinkOptions(List.of(drinkItem(3L, 2)));

        BookingResponse response = bookingService.createBooking(7L, request);

        // 총액 = (500,000 + 50,000) * 2 + 5,000 * 2 = 1,110,000, VIP 10% 할인
        assertThat(response.getBookingId()).isEqualTo(101L);
        assertThat(response.getBookingStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(response.getTotalPrice()).isEqualTo(1_110_000);
        assertThat(response.getDiscountRate()).isEqualTo(10);
        assertThat(response.getExpectedPaymentAmount()).isEqualTo(999_000);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        Booking saved = captor.getValue();
        assertThat(saved.getTour()).isSameAs(tour);
        assertThat(saved.getCustomer()).isSameAs(customer);
        assertThat(saved.getBookingDrinkOptions()).hasSize(1);
        assertThat(saved.getBookingDrinkOptions().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void 예약도_가격_계산과_같은_옵션_규칙을_검사한다() {
        when(tourRepository.findById(1L)).thenReturn(Optional.of(openTour(Theme.HONEYMOON)));

        assertThatThrownBy(() -> bookingService.createBooking(
                7L, bookingRequest(1L, TourLevel.GRAND, 3, Transport.PREMIUM_VAN_10)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void 고객_정보가_없으면_예약할_수_없다() {
        when(tourRepository.findById(1L)).thenReturn(Optional.of(openTour(Theme.GOLF)));
        when(customerRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(
                7L, bookingRequest(1L, TourLevel.CLASSIC, 2, Transport.PREMIUM_VAN_10)))
                .isInstanceOf(NotFoundException.class);
    }

    private Member member(Long memberId) {
        Member member = Member.create("user" + memberId, "pw", "테스터", "010-0000-0000", "서울", Role.CUSTOMER);
        ReflectionTestUtils.setField(member, "memberId", memberId);
        return member;
    }

    // 예약 마감 전(시작 30일 전)인 투어. 기본가는 1인 500,000원이다.
    private Tour openTour(Theme theme) {
        LocalDate startDate = LocalDate.now().plusDays(30);
        Tour tour = Tour.create(theme, "테스트 투어", null, startDate, startDate.plusDays(3),
                500000, Transport.PREMIUM_VAN_10);
        ReflectionTestUtils.setField(tour, "tourId", 1L);
        return tour;
    }

    // 호텔 3성(0원), 식사 도시락(0원)인 기본 요청
    private QuoteRequest quoteRequest(TourLevel level, int guestCount, Transport transport) {
        QuoteRequest request = new QuoteRequest();
        request.setTourLevel(level);
        request.setGuestCount(guestCount);
        request.setHotelGrade(HotelGrade.THREE_STAR);
        request.setMealType(MealType.LUNCH_BOX);
        request.setTransport(transport);
        return request;
    }

    private BookingRequest bookingRequest(Long tourId, TourLevel level, int guestCount, Transport transport) {
        BookingRequest request = new BookingRequest();
        request.setTourId(tourId);
        request.setTourLevel(level);
        request.setGuestCount(guestCount);
        request.setHotelGrade(HotelGrade.THREE_STAR);
        request.setMealType(MealType.LUNCH_BOX);
        request.setTransport(transport);
        return request;
    }

    private QuoteRequest.DrinkOptionItem drinkItem(Long optionId, int quantity) {
        QuoteRequest.DrinkOptionItem item = new QuoteRequest.DrinkOptionItem();
        item.setOptionId(optionId);
        item.setQuantity(quantity);
        return item;
    }

    private DrinkOption drinkOption(Long id, int price) {
        DrinkOption drinkOption = DrinkOption.create("COFFEE", price);
        ReflectionTestUtils.setField(drinkOption, "optionId", id);
        return drinkOption;
    }
}
