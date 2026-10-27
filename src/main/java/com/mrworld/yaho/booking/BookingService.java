package com.mrworld.yaho.booking;

import com.mrworld.yaho.booking.dto.BookingRequest;
import com.mrworld.yaho.booking.dto.BookingResponse;
import com.mrworld.yaho.booking.dto.QuoteRequest;
import com.mrworld.yaho.booking.dto.QuoteResponse;
import com.mrworld.yaho.common.enums.BookingStatus;
import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.common.enums.Transport;
import com.mrworld.yaho.common.exception.ConflictException;
import com.mrworld.yaho.common.exception.NotFoundException;
import com.mrworld.yaho.customer.Customer;
import com.mrworld.yaho.customer.CustomerRepository;
import com.mrworld.yaho.inventory.InventoryService;
import com.mrworld.yaho.tour.DrinkOption;
import com.mrworld.yaho.tour.DrinkOptionRepository;
import com.mrworld.yaho.tour.Tour;
import com.mrworld.yaho.tour.TourRepository;
import lombok.RequiredArgsConstructor;
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

    // 투어 시작 7일 전까지만 예약할 수 있다.
    private static final int BOOKING_CLOSE_DAYS_BEFORE_START = 7;
    private static final int HONEYMOON_GUEST_COUNT = 2;
    private static final int CAR_2_MAX_GUEST_COUNT = 2;

    private final TourRepository tourRepository;
    private final CustomerRepository customerRepository;
    private final DrinkOptionRepository drinkOptionRepository;
    private final BookingRepository bookingRepository;
    private final InventoryService inventoryService;

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

    // 모집 중이고 예약 마감(시작 7일 전) 전인 투어만 예약·가격 계산할 수 있다.
    private Tour findBookableTour(Long tourId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 투어입니다."));

        if (tour.getTourStatus() != TourStatus.RECRUITING) {
            throw new ConflictException("모집 중인 투어가 아닙니다.");
        }
        if (LocalDate.now().isAfter(tour.getStartDate().minusDays(BOOKING_CLOSE_DAYS_BEFORE_START))) {
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
