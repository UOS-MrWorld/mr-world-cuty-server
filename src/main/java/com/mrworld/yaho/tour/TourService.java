package com.mrworld.yaho.tour;

import com.mrworld.yaho.booking.Booking;
import com.mrworld.yaho.booking.BookingRepository;
import com.mrworld.yaho.common.dto.PageResponse;
import com.mrworld.yaho.common.dto.PagingRequest;
import com.mrworld.yaho.common.enums.BookingStatus;
import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.common.exception.ConflictException;
import com.mrworld.yaho.common.exception.NotFoundException;
import com.mrworld.yaho.inventory.InventoryItem;
import com.mrworld.yaho.inventory.InventoryItemRepository;
import com.mrworld.yaho.inventory.InventoryService;
import com.mrworld.yaho.notification.NotificationService;
import com.mrworld.yaho.payment.PaymentService;
import com.mrworld.yaho.tour.dto.DrinkOptionResponse;
import com.mrworld.yaho.tour.dto.StaffTourDetailResponse;
import com.mrworld.yaho.tour.dto.StaffTourListRequest;
import com.mrworld.yaho.tour.dto.StaffTourSummaryResponse;
import com.mrworld.yaho.tour.dto.TourCreateRequest;
import com.mrworld.yaho.tour.dto.TourDetailResponse;
import com.mrworld.yaho.tour.dto.TourGiftResponse;
import com.mrworld.yaho.tour.dto.TourResponse;
import com.mrworld.yaho.tour.dto.TourSearchRequest;
import com.mrworld.yaho.tour.dto.TourSummaryResponse;
import com.mrworld.yaho.tour.dto.TourUpdateRequest;
import com.mrworld.yaho.tour.dto.VoiceSearchRequest;
import com.mrworld.yaho.tour.dto.VoiceSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TourService {

    // 고객에게는 모집 중이거나 확정된 투어만 노출한다.
    private static final List<TourStatus> VISIBLE_STATUSES =
            List.of(TourStatus.RECRUITING, TourStatus.CONFIRMED);

    // 투어 목록(고객용 목록·검색, 직원용 현황)은 출발이 가까운 투어부터 보여준다.
    private static final Sort TOUR_SORT = Sort.by("startDate", "tourId");

    // 마감 시점 결제 완료 인원이 이 수 이상이면 투어를 확정한다.
    // 허니문은 예약이 2명 단위라 결제 인원이 항상 짝수이므로, 실제로는 커플 2쌍(4명)부터 확정된다.
    private static final int MIN_CONFIRM_GUEST_COUNT = 3;

    // 음성 검색 결과는 한 번에 최대 이만큼만 돌려준다.
    private static final int VOICE_SEARCH_LIMIT = 20;

    // 음성 문장에서 테마를 찾기 위한 단어 목록
    private static final Map<Theme, List<String>> THEME_KEYWORDS = new EnumMap<>(Map.of(
            Theme.HONEYMOON, List.of("허니문", "신혼"),
            Theme.HEALING, List.of("힐링", "효도", "부모님", "휴양", "온천"),
            Theme.GOLF, List.of("골프"),
            Theme.TREKKING, List.of("트레킹", "등산")
    ));

    // 모든 상품에 들어 있어 검색 단서가 되지 않는 단어
    private static final Set<String> GENERIC_WORDS = Set.of("투어", "여행", "상품");

    private final TourRepository tourRepository;
    private final DrinkOptionRepository drinkOptionRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final BookingRepository bookingRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final PlatformTransactionManager transactionManager;

    public PageResponse<TourSummaryResponse> getTours(PagingRequest request) {
        return PageResponse.of(
                tourRepository.findByTourStatusIn(VISIBLE_STATUSES, request.toPageable(TOUR_SORT)), this::toSummary);
    }

    public TourDetailResponse getTourDetail(Long tourId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 투어입니다."));

        return TourDetailResponse.builder()
                .tourId(tour.getTourId())
                .theme(tour.getTheme())
                .tourName(tour.getTourName())
                .description(tour.getDescription())
                .startDate(tour.getStartDate())
                .endDate(tour.getEndDate())
                .basePrice(tour.getPrice())
                .defaultTransport(tour.getDefaultTransport())
                .tourStatus(tour.getTourStatus())
                .availableTourLevels(tour.availableLevels())
                .drinkOptions(drinkOptionRepository.findAll(Sort.by("optionId")).stream()
                        .map(drinkOption -> DrinkOptionResponse.builder()
                                .optionId(drinkOption.getOptionId())
                                .drinkName(drinkOption.getDrinkName())
                                .drinkPrice(drinkOption.getDrinkPrice())
                                .build())
                        .toList())
                .gifts(tour.getTourGifts().stream()
                        .map(gift -> TourGiftResponse.builder()
                                .itemId(gift.getInventoryItem().getItemId())
                                .itemName(gift.getInventoryItem().getItemName())
                                .quantityPerGuest(gift.getQuantityPerGuest())
                                .build())
                        .toList())
                .build();
    }

    public PageResponse<TourSummaryResponse> searchTours(TourSearchRequest request) {
        String keyword = request.getKeyword() == null || request.getKeyword().isBlank()
                ? null
                : request.getKeyword().trim();

        return PageResponse.of(tourRepository.search(
                VISIBLE_STATUSES,
                keyword,
                request.getTheme(),
                request.getMaxPrice(),
                request.getDepartureDate(),
                request.toPageable(TOUR_SORT)
        ), this::toSummary);
    }

    // 음성 인식 문장에서 테마를 찾아 필터로 쓰고, 테마가 없으면 문장에 상품명·설명의 단어가 들어 있는 상품을 찾는다.
    public VoiceSearchResponse voiceSearch(VoiceSearchRequest request) {
        String queryText = request.getQueryText() == null ? "" : request.getQueryText().trim();
        if (queryText.isEmpty()) {
            throw new IllegalArgumentException("검색 문장이 비어 있습니다.");
        }

        List<Theme> themes = detectThemes(queryText);
        // 테마가 없으면 조건(가격·날짜)만으로 후보를 모은 뒤 문장과 비교한다.
        List<Theme> searchThemes = themes.isEmpty() ? Collections.singletonList(null) : themes;
        // 문장 비교는 조회 후에 하므로, 테마가 없을 때는 후보를 자르지 않고 모두 가져온 뒤 마지막에 개수를 제한한다.
        Sort sort = Sort.by("startDate", "tourId");
        Pageable pageable = themes.isEmpty()
                ? Pageable.unpaged(sort)
                : PageRequest.of(0, VOICE_SEARCH_LIMIT, sort);

        Map<Long, Tour> found = new LinkedHashMap<>();
        for (Theme theme : searchThemes) {
            tourRepository.search(VISIBLE_STATUSES, null, theme, request.getMaxPrice(),
                            request.getDepartureDate(), pageable)
                    .forEach(tour -> found.putIfAbsent(tour.getTourId(), tour));
        }

        List<TourSummaryResponse> tours = found.values().stream()
                .filter(tour -> !themes.isEmpty() || mentions(queryText, tour))
                .sorted(Comparator.comparing(Tour::getStartDate).thenComparing(Tour::getTourId))
                .limit(VOICE_SEARCH_LIMIT)
                .map(this::toSummary)
                .toList();

        return VoiceSearchResponse.builder()
                .recognizedText(queryText)
                .tours(tours)
                .build();
    }

    // 직원이 등록한 상품은 모집 중 상태로 시작하고, 기념품 구성(TourGift)도 함께 저장한다.
    @Transactional
    public TourResponse createTour(TourCreateRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
        }

        List<TourCreateRequest.GiftItem> gifts = request.getGifts() == null ? List.of() : request.getGifts();
        Map<Long, InventoryItem> items = findGiftItems(gifts);

        Tour tour = Tour.create(
                request.getTheme(),
                request.getTourName().trim(),
                request.getDescription(),
                request.getStartDate(),
                request.getEndDate(),
                request.getBasePrice(),
                request.getDefaultTransport()
        );
        gifts.forEach(gift -> tour.addGift(items.get(gift.getItemId()), gift.getQuantityPerGuest()));

        return TourResponse.from(tourRepository.save(tour));
    }

    // 직원용 상품 현황 목록. 투어마다 결제 완료 인원 합을 함께 보여준다.
    public PageResponse<StaffTourSummaryResponse> getStaffTours(StaffTourListRequest request) {
        if (request.getStartDateFrom() != null && request.getStartDateTo() != null
                && request.getStartDateFrom().isAfter(request.getStartDateTo())) {
            throw new IllegalArgumentException("시작일 하한은 상한보다 늦을 수 없습니다.");
        }

        Page<Tour> page = tourRepository.searchForStaff(
                request.getStatus(), request.getStartDateFrom(), request.getStartDateTo(), request.toPageable(TOUR_SORT));
        List<Long> tourIds = page.getContent().stream().map(Tour::getTourId).toList();
        Map<Long, Long> paidGuestCounts = tourIds.isEmpty() ? Map.of()
                : bookingRepository.sumPaidGuestCountByTourIds(tourIds).stream()
                .collect(Collectors.toMap(BookingRepository.TourGuestCount::getTourId,
                        BookingRepository.TourGuestCount::getGuestCount));

        return PageResponse.of(page, tour -> StaffTourSummaryResponse.builder()
                .tourId(tour.getTourId())
                .tourName(tour.getTourName())
                .theme(tour.getTheme())
                .startDate(tour.getStartDate())
                .tourStatus(tour.getTourStatus())
                .paidGuestCount(paidGuestCounts.getOrDefault(tour.getTourId(), 0L).intValue())
                .build());
    }

    // 직원용 상품 상세 현황: 투어 정보, 결제 완료 인원, 모든 예약 현황, 기념품 구성과 현재 재고
    public StaffTourDetailResponse getStaffTourDetail(Long tourId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 투어입니다."));

        return StaffTourDetailResponse.builder()
                .tour(StaffTourDetailResponse.TourInfo.from(tour))
                .paidGuestCount(bookingRepository.sumGuestCountByTourIdAndBookingStatus(tourId, BookingStatus.PAID))
                .bookings(bookingRepository.findAllByTourIdWithCustomer(tourId).stream()
                        .map(StaffTourDetailResponse.BookingInfo::from)
                        .toList())
                .gifts(tour.getTourGifts().stream().map(StaffTourDetailResponse.GiftInfo::from).toList())
                .build();
    }

    // 모집 중인 상품만 수정할 수 있다. 보내지 않은 값은 그대로 두고, gifts를 보내면 기념품 구성 전체를 교체한다.
    @Transactional
    public TourResponse updateTour(Long tourId, TourUpdateRequest request) {
        if (request.hasNoUpdate()) {
            throw new IllegalArgumentException("수정할 값이 없습니다.");
        }

        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 투어입니다."));
        if (tour.getTourStatus() != TourStatus.RECRUITING) {
            throw new ConflictException("모집 중인 상품만 수정할 수 있습니다.");
        }

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : tour.getStartDate();
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : tour.getEndDate();
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
        }

        Map<Long, InventoryItem> items = request.getGifts() == null ? Map.of() : findGiftItems(request.getGifts());
        tour.update(
                request.getTourName() == null ? null : request.getTourName().trim(),
                request.getDescription(),
                request.getStartDate(),
                request.getEndDate(),
                request.getBasePrice(),
                request.getDefaultTransport()
        );
        if (request.getGifts() != null) {
            tour.clearGifts();
            // 기존 구성 삭제가 먼저 반영되도록 내보낸 뒤 새 구성을 추가한다.
            tourRepository.flush();
            request.getGifts().forEach(gift ->
                    tour.addGift(items.get(gift.getItemId()), gift.getQuantityPerGuest()));
        }

        return TourResponse.from(tour);
    }

    // 같은 기념품을 두 번 넣거나 없는 재고를 지정하면 등록하지 않는다.
    private Map<Long, InventoryItem> findGiftItems(List<TourCreateRequest.GiftItem> gifts) {
        List<Long> itemIds = gifts.stream().map(TourCreateRequest.GiftItem::getItemId).toList();
        if (itemIds.stream().distinct().count() != itemIds.size()) {
            throw new IllegalArgumentException("같은 기념품을 중복해서 지정할 수 없습니다.");
        }

        Map<Long, InventoryItem> items = inventoryItemRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(InventoryItem::getItemId, Function.identity()));
        if (items.size() != itemIds.size()) {
            throw new NotFoundException("존재하지 않는 기념품 재고입니다.");
        }
        return items;
    }

    private List<Theme> detectThemes(String queryText) {
        return THEME_KEYWORDS.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(queryText::contains))
                .map(Map.Entry::getKey)
                .toList();
    }

    // 문장이 상품명·설명의 의미 있는 단어(2자 이상, 공통 단어 제외)를 하나라도 포함하는지 확인한다.
    private boolean mentions(String queryText, Tour tour) {
        String source = tour.getTourName() + " " + (tour.getDescription() == null ? "" : tour.getDescription());
        return Arrays.stream(source.split("[^\\p{L}\\p{N}]+"))
                .filter(word -> word.length() >= 2 && !GENERIC_WORDS.contains(word))
                .anyMatch(queryText::contains);
    }

    private TourSummaryResponse toSummary(Tour tour) {
        return TourSummaryResponse.builder()
                .tourId(tour.getTourId())
                .theme(tour.getTheme())
                .tourName(tour.getTourName())
                .startDate(tour.getStartDate())
                .endDate(tour.getEndDate())
                .basePrice(tour.getPrice())
                .tourStatus(tour.getTourStatus())
                .build();
    }

    // 예약 마감(시작 7일 전)이 지난 모집 중 투어를 확정하거나 취소한다. TourScheduler가 매일 호출한다.
    // 투어마다 별도 트랜잭션으로 처리해, 한 투어가 실패해도 나머지 투어는 처리되고 실패한 투어는 다음 실행에 다시 시도한다.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void closeRecruitingTours() {
        LocalDate closedBefore = LocalDate.now().plusDays(Tour.BOOKING_CLOSE_DAYS_BEFORE_START);
        List<Long> tourIds = tourRepository.findAllByTourStatusAndStartDateBefore(TourStatus.RECRUITING, closedBefore)
                .stream()
                .map(Tour::getTourId)
                .toList();

        processEachTour(tourIds, this::closeTour, "투어 마감 처리 실패");
    }

    // 여행 기간이 끝난 확정 투어를 완료 처리하고, 참가 고객의 단골 등급을 완료 여행 횟수로 다시 정한다.
    // TourScheduler가 매일 호출하며, 마감 처리와 같이 투어마다 별도 트랜잭션으로 처리한다.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void completeEndedTours() {
        List<Long> tourIds = tourRepository.findAllByTourStatusAndEndDateBefore(TourStatus.CONFIRMED, LocalDate.now())
                .stream()
                .map(Tour::getTourId)
                .toList();

        processEachTour(tourIds, this::completeTour, "투어 완료 처리 실패");
    }

    // 투어마다 새 트랜잭션에서 처리한다. 실패한 투어는 로그만 남기고 다음 투어로 넘어간다.
    private void processEachTour(List<Long> tourIds, Consumer<Long> action, String failMessage) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        for (Long tourId : tourIds) {
            try {
                transaction.executeWithoutResult(status -> action.accept(tourId));
            } catch (Exception e) {
                log.error("{} tourId={}", failMessage, tourId, e);
            }
        }
    }

    private void completeTour(Long tourId) {
        Tour tour = tourRepository.findById(tourId).orElseThrow();
        tour.complete();

        // 완료 처리가 반영된 뒤 횟수를 세도록, 등급 계산 쿼리 전에 상태 변경을 DB에 내보낸다.
        tourRepository.flush();
        bookingRepository.findAllByTourIdAndBookingStatus(tourId, BookingStatus.PAID).stream()
                .map(Booking::getCustomer)
                .distinct()
                .forEach(customer -> customer.updateLoyaltyGrade(
                        bookingRepository.countCompletedToursByCustomerId(customer.getMemberId())));
    }

    private void closeTour(Long tourId) {
        Tour tour = tourRepository.findById(tourId).orElseThrow();
        List<Booking> paidBookings = bookingRepository.findAllByTourIdAndBookingStatus(tourId, BookingStatus.PAID);
        int paidGuestCount = paidBookings.stream().mapToInt(Booking::getGuestCount).sum();

        if (paidGuestCount >= MIN_CONFIRM_GUEST_COUNT) {
            confirmTour(tour, paidBookings);
        } else {
            cancelTour(tour, paidBookings);
        }

        // 마감 후에는 결제할 수 없으므로 결제 전(PENDING) 예약은 모두 취소한다.
        bookingRepository.findAllByTourIdAndBookingStatus(tourId, BookingStatus.PENDING)
                .forEach(Booking::cancel);
    }

    // 확정 인원만큼 기념품 재고를 차감하고, 결제 완료 고객에게 확정 문자를 보낸다.
    private void confirmTour(Tour tour, List<Booking> paidBookings) {
        int confirmedGuestCount = paidBookings.stream().mapToInt(Booking::getGuestCount).sum();
        tour.confirm();
        inventoryService.deductStock(tour, confirmedGuestCount);
        notificationService.sendTourConfirmedSms(tour, paidBookings);
    }

    // 인원 미달이면 결제 완료 예약을 모두 환불하고 취소한다.
    private void cancelTour(Tour tour, List<Booking> paidBookings) {
        tour.cancel();
        for (Booking booking : paidBookings) {
            paymentService.refund(booking);
            booking.cancel();
        }
    }
}
