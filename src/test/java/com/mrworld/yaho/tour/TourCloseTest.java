package com.mrworld.yaho.tour;

import com.mrworld.yaho.booking.Booking;
import com.mrworld.yaho.booking.BookingRepository;
import com.mrworld.yaho.common.enums.BookingStatus;
import com.mrworld.yaho.common.enums.HotelGrade;
import com.mrworld.yaho.common.enums.LoyaltyGrade;
import com.mrworld.yaho.common.enums.MealType;
import com.mrworld.yaho.common.enums.PaymentStatus;
import com.mrworld.yaho.common.enums.Role;
import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourLevel;
import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.common.enums.Transport;
import com.mrworld.yaho.customer.Customer;
import com.mrworld.yaho.customer.CustomerRepository;
import com.mrworld.yaho.inventory.InventoryItem;
import com.mrworld.yaho.inventory.InventoryItemRepository;
import com.mrworld.yaho.member.Member;
import com.mrworld.yaho.member.MemberRepository;
import com.mrworld.yaho.notification.SmsSender;
import com.mrworld.yaho.payment.Payment;
import com.mrworld.yaho.payment.PaymentRepository;
import com.mrworld.yaho.support.DatabaseCleaner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class TourCloseTest {

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private TourLifecycleService tourLifecycleService;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private SmsSender smsSender;

    private InventoryItem scarf;

    @BeforeEach
    void setUp() {
        cleanUp();
        scarf = inventoryItemRepository.save(InventoryItem.create("스카프", 10));
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    void 결제_인원이_3명_이상이면_확정하고_재고를_차감하고_문자를_보낸다() {
        Tour tour = saveTour(6);
        Booking first = savePaidBooking(tour, saveCustomer("010-1111-2222"), 2);
        Booking second = savePaidBooking(tour, saveCustomer("01033334444"), 1);
        Booking pending = bookingRepository.save(booking(tour, saveCustomer("01055556666"), 1));

        tourLifecycleService.closeRecruitingTours();

        assertThat(tourRepository.findById(tour.getTourId()).orElseThrow().getTourStatus())
                .isEqualTo(TourStatus.CONFIRMED);
        // 1인당 2개 × 확정 3명
        assertThat(inventoryItemRepository.findById(scarf.getItemId()).orElseThrow().getQuantity()).isEqualTo(4);
        assertThat(statusOf(first)).isEqualTo(BookingStatus.PAID);
        assertThat(statusOf(second)).isEqualTo(BookingStatus.PAID);
        assertThat(statusOf(pending)).isEqualTo(BookingStatus.CANCELLED);

        verify(smsSender).send(eq("01011112222"), contains("예약번호: " + first.getBookingId() + " (2명)"));
        verify(smsSender).send(eq("01033334444"), contains("예약번호: " + second.getBookingId() + " (1명)"));
        verify(smsSender, never()).send(eq("01055556666"), anyString());
    }

    @Test
    void 한_고객이_여러_건_예약했으면_예약마다_문자를_보낸다() {
        Tour tour = saveTour(6);
        Customer customer = saveCustomer("01011112222");
        Booking first = savePaidBooking(tour, customer, 2);
        Booking second = savePaidBooking(tour, customer, 1);

        tourLifecycleService.closeRecruitingTours();

        verify(smsSender).send(eq("01011112222"), contains("예약번호: " + first.getBookingId()));
        verify(smsSender).send(eq("01011112222"), contains("예약번호: " + second.getBookingId()));
        verify(smsSender, times(2)).send(eq("01011112222"),
                contains("옵션: 클래식 / 3성급 호텔 / 도시락 / 프리미엄 밴"));
    }

    @Test
    void 결제_인원이_3명_미만이면_취소하고_결제를_환불한다() {
        Tour tour = saveTour(6);
        Booking paid = savePaidBooking(tour, saveCustomer("01011112222"), 2);

        tourLifecycleService.closeRecruitingTours();

        assertThat(tourRepository.findById(tour.getTourId()).orElseThrow().getTourStatus())
                .isEqualTo(TourStatus.CANCELLED);
        assertThat(statusOf(paid)).isEqualTo(BookingStatus.CANCELLED);
        assertThat(paymentRepository.findAll()).extracting(Payment::getPaymentStatus)
                .containsExactly(PaymentStatus.REFUNDED);
        assertThat(inventoryItemRepository.findById(scarf.getItemId()).orElseThrow().getQuantity()).isEqualTo(10);
        verify(smsSender, never()).send(anyString(), anyString());
    }

    @Test
    void 아직_마감되지_않은_투어는_처리하지_않는다() {
        Tour tour = saveTour(7);
        savePaidBooking(tour, saveCustomer("01011112222"), 3);

        tourLifecycleService.closeRecruitingTours();

        assertThat(tourRepository.findById(tour.getTourId()).orElseThrow().getTourStatus())
                .isEqualTo(TourStatus.RECRUITING);
    }

    @Test
    void 기념품_재고가_부족해도_확정하고_재고는_마이너스로_차감한다() {
        Tour tour = saveTour(6);
        savePaidBooking(tour, saveCustomer("01011112222"), 6);

        tourLifecycleService.closeRecruitingTours();

        // 스카프 10개에서 6명 × 2개 = 12개를 빼면 -2 (2개 부족)
        assertThat(tourRepository.findById(tour.getTourId()).orElseThrow().getTourStatus())
                .isEqualTo(TourStatus.CONFIRMED);
        assertThat(inventoryItemRepository.findById(scarf.getItemId()).orElseThrow().getQuantity()).isEqualTo(-2);
        verify(smsSender).send(eq("01011112222"), anyString());
    }

    @Test
    void 여행이_끝난_확정_투어는_완료되고_완료_횟수로_단골_등급을_정한다() {
        Customer customer = saveCustomer("01011112222");
        // 이미 완료된 여행 4번 + 이번에 끝난 여행 1번 = 5번 → VIP
        for (int i = 0; i < 4; i++) {
            Tour past = saveEndedTour(TourStatus.COMPLETED, 10 + i);
            savePaidBooking(past, customer, 1);
        }
        Tour ended = saveEndedTour(TourStatus.CONFIRMED, 1);
        savePaidBooking(ended, customer, 1);
        Tour endsToday = saveEndedTour(TourStatus.CONFIRMED, 0);

        tourLifecycleService.completeEndedTours();

        assertThat(tourRepository.findById(ended.getTourId()).orElseThrow().getTourStatus())
                .isEqualTo(TourStatus.COMPLETED);
        assertThat(tourRepository.findById(endsToday.getTourId()).orElseThrow().getTourStatus())
                .isEqualTo(TourStatus.CONFIRMED);
        Customer updated = customerRepository.findById(customer.getMemberId()).orElseThrow();
        assertThat(updated.getLoyaltyGrade()).isEqualTo(LoyaltyGrade.VIP);
        // 할인은 직원이 등급별 일괄 적용으로 켜기 전까지 꺼져 있다.
        assertThat(updated.isLoyaltyDiscountApplied()).isFalse();
    }

    @Test
    void 완료_횟수가_기준에_못_미치면_일반_등급이다() {
        Customer customer = saveCustomer("01011112222");
        Tour ended = saveEndedTour(TourStatus.CONFIRMED, 1);
        savePaidBooking(ended, customer, 2);

        tourLifecycleService.completeEndedTours();

        assertThat(customerRepository.findById(customer.getMemberId()).orElseThrow().getLoyaltyGrade())
                .isEqualTo(LoyaltyGrade.NORMAL);
    }

    // 종료일이 오늘부터 daysAgo일 전인 투어
    private Tour saveEndedTour(TourStatus tourStatus, int daysAgo) {
        LocalDate endDate = LocalDate.now().minusDays(daysAgo);
        Tour tour = Tour.create(Theme.GOLF, "골프 챌린지 투어", null,
                endDate.minusDays(3), endDate, 500000, Transport.PREMIUM_VAN_10);
        ReflectionTestUtils.setField(tour, "tourStatus", tourStatus);
        return tourRepository.save(tour);
    }

    // 시작일이 오늘부터 daysLater일 뒤이고 1인당 스카프 2개를 주는 투어
    private Tour saveTour(int daysLater) {
        LocalDate startDate = LocalDate.now().plusDays(daysLater);
        Tour tour = Tour.create(Theme.GOLF, "골프 챌린지 투어", null,
                startDate, startDate.plusDays(3), 500000, Transport.PREMIUM_VAN_10);
        tour.addGift(scarf, 2);
        return tourRepository.save(tour);
    }

    private Booking savePaidBooking(Tour tour, Customer customer, int guestCount) {
        Booking booking = booking(tour, customer, guestCount);
        booking.pay(0);
        booking = bookingRepository.save(booking);
        paymentRepository.save(Payment.success(booking, 1_000_000, LocalDateTime.now()));
        return booking;
    }

    private Booking booking(Tour tour, Customer customer, int guestCount) {
        return Booking.create(tour, customer, TourLevel.CLASSIC, guestCount,
                HotelGrade.THREE_STAR, MealType.LUNCH_BOX, Transport.PREMIUM_VAN_10, 1_000_000, 0);
    }

    private Customer saveCustomer(String phoneNum) {
        Member member = memberRepository.save(Member.create(
                "close-" + UUID.randomUUID(), "pw", "고객", phoneNum, "서울", Role.CUSTOMER));
        return customerRepository.save(Customer.create(member));
    }

    private BookingStatus statusOf(Booking booking) {
        return bookingRepository.findById(booking.getBookingId()).orElseThrow().getBookingStatus();
    }

    private void cleanUp() {
        databaseCleaner.clean();
    }
}
