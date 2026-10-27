package com.mrworld.yaho.booking;

import com.mrworld.yaho.booking.dto.BookingDetailResponse;
import com.mrworld.yaho.booking.dto.BookingListRequest;
import com.mrworld.yaho.booking.dto.BookingListResponse;
import com.mrworld.yaho.booking.dto.BookingRequest;
import com.mrworld.yaho.booking.dto.BookingResponse;
import com.mrworld.yaho.booking.dto.BookingSummaryResponse;
import com.mrworld.yaho.booking.dto.BookingUpdateRequest;
import com.mrworld.yaho.booking.dto.BookingUpdateResponse;
import com.mrworld.yaho.booking.dto.QuoteRequest;
import com.mrworld.yaho.booking.dto.QuoteResponse;
import com.mrworld.yaho.common.enums.BookingStatus;
import com.mrworld.yaho.common.enums.PaymentStatus;
import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.common.enums.Transport;
import com.mrworld.yaho.common.exception.ConflictException;
import com.mrworld.yaho.common.exception.ForbiddenException;
import com.mrworld.yaho.common.exception.NotFoundException;
import com.mrworld.yaho.customer.Customer;
import com.mrworld.yaho.customer.CustomerRepository;
import com.mrworld.yaho.inventory.InventoryService;
import com.mrworld.yaho.payment.Payment;
import com.mrworld.yaho.payment.PaymentRepository;
import com.mrworld.yaho.tour.DrinkOption;
import com.mrworld.yaho.tour.DrinkOptionRepository;
import com.mrworld.yaho.tour.Tour;
import com.mrworld.yaho.tour.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 신청 처리 + 여행확정 판정(최소 인원 도달 시 자동 확정) + 재고 차감/알림 발송 호출
// 예약 금액(할인 전 총 금액)과 단골 할인율 계산은 이 서비스가 담당한다.
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final int HONEYMOON_GUEST_COUNT = 2;
    private static final int CAR_2_MAX_GUEST_COUNT = 2;

    private final TourRepository tourRepository;
    private final CustomerRepository customerRepository;
    private final DrinkOptionRepository drinkOptionRepository;
    private final BookingRepository bookingRepository;
    private final InventoryService inventoryService;
    private final PaymentRepository paymentRepository;

    // 선택한 옵션으로 결제 전(PENDING) 예약을 만든다. 금액과 할인율은 예약 시점 값으로 저장한다.
    @Transactional
    public BookingResponse createBooking(Long customerId, BookingRequest request) {
        Tour tour = findBookableTour(request.getTourId());
        validateBookingOptions(tour, request);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 고객입니다."));

        // 결제 완료 인원과 이번 예약 인원을 합쳐 기념품 재고가 충분한지 확인한다.
        int paidGuestCount = bookingRepository.sumGuestCountByTourIdAndBookingStatus(
                tour.getTourId(), BookingStatus.PAID);
        inventoryService.validateStock(tour, paidGuestCount + request.getGuestCount());

        Map<Long, DrinkOption> drinkOptions = findDrinkOptions(request.getDrinkOptions());
        Booking booking = Booking.create(
                tour,
                customer,
                request.getTourLevel(),
                request.getGuestCount(),
                request.getHotelGrade(),
                request.getMealType(),
                request.getTransport(),
                calculateTotalPrice(tour, request, drinkOptions),
                customer.currentDiscountRate()
        );
        if (request.getDrinkOptions() != null) {
            for (QuoteRequest.DrinkOptionItem item : request.getDrinkOptions()) {
                booking.addDrinkOption(drinkOptions.get(item.getOptionId()), item.getQuantity());
            }
        }

        return BookingResponse.from(bookingRepository.save(booking));
    }

    // 로그인 고객의 예약을 상태별로 최근 순 조회한다.
    @Transactional(readOnly = true)
    public BookingListResponse getBookings(Long customerId, BookingListRequest request) {
        Page<Booking> page = bookingRepository.findMyBookings(customerId, request.getStatus(), request.toPageable());

        return BookingListResponse.builder()
                .content(page.getContent().stream().map(BookingSummaryResponse::from).toList())
                .totalElements(page.getTotalElements())
                .build();
    }

    @Transactional(readOnly = true)
    public BookingDetailResponse getBookingDetail(Long customerId, Long bookingId) {
        Booking booking = findMyBooking(customerId, bookingId);
        // 가장 최근 결제 시도의 상태. 결제한 적이 없으면 null이다.
        PaymentStatus paymentStatus = paymentRepository.findTopByBookingBookingIdOrderByPaymentIdDesc(bookingId)
                .map(Payment::getPaymentStatus)
                .orElse(null);
        return BookingDetailResponse.from(booking, paymentStatus);
    }

    // 결제 전(PENDING) 예약의 옵션을 바꾼다. 보내지 않은 옵션은 기존 값을 유지하고 금액은 다시 계산한다.
    @Transactional
    public BookingUpdateResponse updateBookingOptions(Long customerId, Long bookingId, BookingUpdateRequest request) {
        if (request.hasNoUpdate()) {
            throw new IllegalArgumentException("수정할 값이 없습니다.");
        }

        Booking booking = findMyBooking(customerId, bookingId);
        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new ConflictException("결제 전 예약만 수정할 수 있습니다.");
        }

        Tour tour = findBookableTour(booking.getTour().getTourId());
        QuoteRequest merged = mergeOptions(booking, request);
        validateBookingOptions(tour, merged);

        // 인원을 바꾸면 예약 생성과 같은 기준으로 기념품 재고를 다시 확인한다.
        if (request.getGuestCount() != null) {
            int paidGuestCount = bookingRepository.sumGuestCountByTourIdAndBookingStatus(
                    tour.getTourId(), BookingStatus.PAID);
            inventoryService.validateStock(tour, paidGuestCount + merged.getGuestCount());
        }

        Map<Long, DrinkOption> drinkOptions = findDrinkOptions(merged.getDrinkOptions());
        booking.updateOptions(
                merged.getTourLevel(),
                merged.getGuestCount(),
                merged.getHotelGrade(),
                merged.getMealType(),
                merged.getTransport(),
                calculateTotalPrice(tour, merged, drinkOptions)
        );
        if (request.getDrinkOptions() != null) {
            booking.clearDrinkOptions();
            for (QuoteRequest.DrinkOptionItem item : request.getDrinkOptions()) {
                booking.addDrinkOption(drinkOptions.get(item.getOptionId()), item.getQuantity());
            }
        }

        return BookingUpdateResponse.from(booking);
    }

    // 예약을 만들지 않고 선택한 옵션의 예상 금액과 단골 할인 적용 금액만 계산한다.
    @Transactional(readOnly = true)
    public QuoteResponse getQuote(Long tourId, Long customerId, QuoteRequest request) {
        Tour tour = findBookableTour(tourId);
        validateBookingOptions(tour, request);

        int totalPrice = calculateTotalPrice(tour, request, findDrinkOptions(request.getDrinkOptions()));
        int basePrice = tour.getPrice() * request.getGuestCount();

        int discountRate = customerRepository.findById(customerId)
                .map(Customer::currentDiscountRate)
                .orElse(0);

        return QuoteResponse.builder()
                .basePrice(basePrice)
                .optionPrice(totalPrice - basePrice)
                .discountRate(discountRate)
                .totalPrice(totalPrice)
                .expectedPaymentAmount(totalPrice * (100 - discountRate) / 100)
                .build();
    }

    private Booking findMyBooking(Long customerId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 예약입니다."));
        if (!booking.isOwnedBy(customerId)) {
            throw new ForbiddenException("본인의 예약이 아닙니다.");
        }
        return booking;
    }

    // 수정 요청에 없는 옵션은 기존 예약 값으로 채워, 생성·가격 계산과 같은 검증과 계산을 쓴다.
    private QuoteRequest mergeOptions(Booking booking, BookingUpdateRequest request) {
        QuoteRequest merged = new QuoteRequest();
        merged.setTourLevel(request.getTourLevel() != null ? request.getTourLevel() : booking.getTourLevel());
        merged.setGuestCount(request.getGuestCount() != null ? request.getGuestCount() : booking.getGuestCount());
        merged.setHotelGrade(request.getHotelGrade() != null ? request.getHotelGrade() : booking.getHotelGrade());
        merged.setMealType(request.getMealType() != null ? request.getMealType() : booking.getMealType());
        merged.setTransport(request.getTransport() != null ? request.getTransport() : booking.getTransport());
        merged.setDrinkOptions(request.getDrinkOptions() != null
                ? request.getDrinkOptions()
                : booking.getBookingDrinkOptions().stream().map(BookingService::toDrinkOptionItem).toList());
        return merged;
    }

    private static QuoteRequest.DrinkOptionItem toDrinkOptionItem(BookingDrinkOption bookingDrinkOption) {
        QuoteRequest.DrinkOptionItem item = new QuoteRequest.DrinkOptionItem();
        item.setOptionId(bookingDrinkOption.getDrinkOption().getOptionId());
        item.setQuantity(bookingDrinkOption.getQuantity());
        return item;
    }

    // 모집 중이고 예약 마감(시작 7일 전) 전인 투어만 예약·수정·가격 계산할 수 있다.
    private Tour findBookableTour(Long tourId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 투어입니다."));

        if (tour.getTourStatus() != TourStatus.RECRUITING) {
            throw new ConflictException("모집 중인 투어가 아닙니다.");
        }
        if (tour.isBookingClosed(LocalDate.now())) {
            throw new IllegalArgumentException("예약 마감 기간이 지난 투어입니다.");
        }
        return tour;
    }

    private void validateBookingOptions(Tour tour, QuoteRequest request) {
        if (!tour.availableLevels().contains(request.getTourLevel())) {
            throw new IllegalArgumentException("선택할 수 없는 투어 등급입니다.");
        }

        int guestCount = request.getGuestCount();
        if (tour.getTheme() == Theme.HONEYMOON && guestCount != HONEYMOON_GUEST_COUNT) {
            throw new IllegalArgumentException("허니문 투어는 2명만 예약할 수 있습니다.");
        }
        if (request.getTransport() == Transport.PREMIUM_CAR_2 && guestCount > CAR_2_MAX_GUEST_COUNT) {
            throw new IllegalArgumentException("2인 차량은 3명 이상 예약에 선택할 수 없습니다.");
        }
    }

    // 할인 전 총 금액 = (1인 기본가 + 호텔 + 식사 + 교통) × 인원 + 음료
    // 음료 수량은 예약 전체 기준 개수라서 인원수를 곱하지 않는다.
    private int calculateTotalPrice(Tour tour, QuoteRequest request, Map<Long, DrinkOption> drinkOptions) {
        int perGuestPrice = tour.getPrice()
                + request.getHotelGrade().getPrice()
                + request.getMealType().getPrice()
                + request.getTransport().getPrice();

        int drinkPrice = 0;
        if (request.getDrinkOptions() != null) {
            for (QuoteRequest.DrinkOptionItem item : request.getDrinkOptions()) {
                drinkPrice += drinkOptions.get(item.getOptionId()).getDrinkPrice() * item.getQuantity();
            }
        }
        return perGuestPrice * request.getGuestCount() + drinkPrice;
    }

    // 요청한 음료 옵션을 ID별로 조회한다. 하나라도 없으면 잘못된 옵션으로 본다.
    private Map<Long, DrinkOption> findDrinkOptions(List<QuoteRequest.DrinkOptionItem> drinkOptions) {
        if (drinkOptions == null || drinkOptions.isEmpty()) {
            return Map.of();
        }

        Map<Long, DrinkOption> found = drinkOptionRepository
                .findAllById(drinkOptions.stream().map(QuoteRequest.DrinkOptionItem::getOptionId).toList())
                .stream()
                .collect(Collectors.toMap(DrinkOption::getOptionId, Function.identity()));

        for (QuoteRequest.DrinkOptionItem item : drinkOptions) {
            if (!found.containsKey(item.getOptionId())) {
                throw new IllegalArgumentException("존재하지 않는 음료 옵션입니다.");
            }
        }
        return found;
    }
}
