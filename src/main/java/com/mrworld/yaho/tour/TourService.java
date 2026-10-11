package com.mrworld.yaho.tour;

import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.common.enums.Transport;
import com.mrworld.yaho.common.exception.ConflictException;
import com.mrworld.yaho.common.exception.NotFoundException;
import com.mrworld.yaho.customer.Customer;
import com.mrworld.yaho.customer.CustomerRepository;
import com.mrworld.yaho.tour.dto.TourDetailResponse;
import com.mrworld.yaho.tour.dto.TourListRequest;
import com.mrworld.yaho.tour.dto.TourListResponse;
import com.mrworld.yaho.tour.dto.QuoteRequest;
import com.mrworld.yaho.tour.dto.QuoteResponse;
import com.mrworld.yaho.tour.dto.TourSearchRequest;
import com.mrworld.yaho.tour.dto.TourSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TourService {

    // 고객에게는 모집 중이거나 확정된 투어만 노출한다.
    private static final List<TourStatus> VISIBLE_STATUSES =
            List.of(TourStatus.RECRUITING, TourStatus.CONFIRMED);

    // 투어 시작 7일 전까지만 예약할 수 있다.
    private static final int BOOKING_CLOSE_DAYS_BEFORE_START = 7;
    private static final int HONEYMOON_GUEST_COUNT = 2;
    private static final int CAR_2_MAX_GUEST_COUNT = 2;

    private final TourRepository tourRepository;
    private final DrinkOptionRepository drinkOptionRepository;
    private final CustomerRepository customerRepository;

    public TourListResponse getTours(TourListRequest request) {
        return toListResponse(tourRepository.findByTourStatusIn(VISIBLE_STATUSES, request.toPageable()));
    }

    public TourDetailResponse getTourDetails(Long tourId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 투어입니다."));

        return TourDetailResponse.builder()
                .tourId(tour.getId())
                .theme(tour.getTheme())
                .tourName(tour.getTourName())
                .description(tour.getDescription())
                .startDate(tour.getStartDate())
                .endDate(tour.getEndDate())
                .basePrice(tour.getBasePrice())
                .defaultTransport(tour.getDefaultTransport())
                .tourStatus(tour.getTourStatus())
                .availableTourLevels(tour.availableLevels())
                .build();
    }

    public TourListResponse searchTours(TourSearchRequest request) {
        String keyword = request.getKeyword() == null || request.getKeyword().isBlank()
                ? null
                : request.getKeyword().trim();

        return toListResponse(tourRepository.search(
                VISIBLE_STATUSES,
                keyword,
                request.getTheme(),
                request.getMaxPrice(),
                request.getDepartureDate(),
                request.toPageable()
        ));
    }

    // 예약을 만들지 않고 선택한 옵션의 예상 금액과 단골 할인 적용 금액만 계산한다.
    public QuoteResponse getQuote(Long tourId, Long customerId, QuoteRequest request) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 투어입니다."));

        if (tour.getTourStatus() != TourStatus.RECRUITING) {
            throw new ConflictException("모집 중인 투어가 아닙니다.");
        }
        if (LocalDate.now().isAfter(tour.getStartDate().minusDays(BOOKING_CLOSE_DAYS_BEFORE_START))) {
            throw new IllegalArgumentException("예약 마감 기간이 지난 투어입니다.");
        }
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

        int totalPrice = calculateTotalPrice(tour, request);
        int basePrice = tour.getBasePrice() * guestCount;

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

    // 할인 전 총 금액 = (1인 기본가 + 호텔 + 식사 + 교통) × 인원 + 음료
    // 음료 수량은 예약 전체 기준 개수라서 인원수를 곱하지 않는다.
    private int calculateTotalPrice(Tour tour, QuoteRequest request) {
        int perGuestPrice = tour.getBasePrice()
                + request.getHotelGrade().getPrice()
                + request.getMealType().getPrice()
                + request.getTransport().getPrice();

        return perGuestPrice * request.getGuestCount() + calculateDrinkPrice(request.getDrinkOptions());
    }

    private int calculateDrinkPrice(List<QuoteRequest.DrinkOptionItem> drinkOptions) {
        if (drinkOptions == null || drinkOptions.isEmpty()) {
            return 0;
        }

        Map<Long, DrinkOption> found = drinkOptionRepository
                .findAllById(drinkOptions.stream().map(QuoteRequest.DrinkOptionItem::getOptionId).toList())
                .stream()
                .collect(Collectors.toMap(DrinkOption::getId, Function.identity()));

        int total = 0;
        for (QuoteRequest.DrinkOptionItem item : drinkOptions) {
            DrinkOption drinkOption = found.get(item.getOptionId());
            if (drinkOption == null) {
                throw new IllegalArgumentException("존재하지 않는 음료 옵션입니다.");
            }
            total += drinkOption.getDrinkPrice() * item.getQuantity();
        }
        return total;
    }

    // 목록 조회와 검색이 같은 응답 형식을 쓴다.
    private TourListResponse toListResponse(Page<Tour> page) {
        return TourListResponse.builder()
                .content(page.getContent().stream().map(this::toSummary).toList())
                .totalElements(page.getTotalElements())
                .build();
    }

    private TourSummaryResponse toSummary(Tour tour) {
        return TourSummaryResponse.builder()
                .tourId(tour.getId())
                .theme(tour.getTheme())
                .tourName(tour.getTourName())
                .startDate(tour.getStartDate())
                .endDate(tour.getEndDate())
                .basePrice(tour.getBasePrice())
                .tourStatus(tour.getTourStatus())
                .build();
    }
}
