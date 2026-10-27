package com.mrworld.yaho.tour;

import com.mrworld.yaho.booking.BookingRepository;
import com.mrworld.yaho.common.dto.IdTotal;
import com.mrworld.yaho.common.dto.PageResponse;
import com.mrworld.yaho.common.dto.PagingRequest;
import com.mrworld.yaho.common.enums.BookingStatus;
import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.common.exception.BadRequestException;
import com.mrworld.yaho.common.exception.ConflictException;
import com.mrworld.yaho.common.exception.NotFoundException;
import com.mrworld.yaho.inventory.InventoryItem;
import com.mrworld.yaho.inventory.InventoryItemRepository;
import com.mrworld.yaho.tour.dto.DrinkOptionResponse;
import com.mrworld.yaho.tour.dto.StaffTourDetailResponse;
import com.mrworld.yaho.tour.dto.StaffTourListRequest;
import com.mrworld.yaho.tour.dto.StaffTourSummaryResponse;
import com.mrworld.yaho.tour.dto.TourCreateRequest;
import com.mrworld.yaho.tour.dto.TourDetailResponse;
import com.mrworld.yaho.tour.dto.TourResponse;
import com.mrworld.yaho.tour.dto.TourSummaryResponse;
import com.mrworld.yaho.tour.dto.TourUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 여행 상품 조회·등록·수정. 검색은 TourSearchService, 마감·완료 처리는 TourLifecycleService가 맡는다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TourService {

    private final TourRepository tourRepository;
    private final DrinkOptionRepository drinkOptionRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final BookingRepository bookingRepository;

    public PageResponse<TourSummaryResponse> getTours(PagingRequest request) {
        return PageResponse.of(
                tourRepository.findByTourStatusIn(Tour.CUSTOMER_VISIBLE_STATUSES, request.toPageable(Tour.DEFAULT_SORT)),
                TourSummaryResponse::from);
    }

    public TourDetailResponse getTourDetail(Long tourId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 투어입니다."));

        List<DrinkOptionResponse> drinkOptions = drinkOptionRepository.findAll(Sort.by("optionId")).stream()
                .map(DrinkOptionResponse::from)
                .toList();
        return TourDetailResponse.from(tour, drinkOptions);
    }

    // 직원이 등록한 상품은 모집 중 상태로 시작하고, 기념품 구성(TourGift)도 함께 저장한다.
    @Transactional
    public TourResponse createTour(TourCreateRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("종료일은 시작일보다 빠를 수 없습니다.");
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
            throw new BadRequestException("시작일 하한은 상한보다 늦을 수 없습니다.");
        }

        Page<Tour> page = tourRepository.searchForStaff(
                request.getStatus(), request.getStartDateFrom(), request.getStartDateTo(), request.toPageable(Tour.DEFAULT_SORT));
        List<Long> tourIds = page.getContent().stream().map(Tour::getTourId).toList();
        Map<Long, Long> paidGuestCounts = tourIds.isEmpty() ? Map.of()
                : IdTotal.toMap(bookingRepository.sumPaidGuestCountByTourIds(tourIds));

        return PageResponse.of(page, tour -> StaffTourSummaryResponse.from(
                tour, paidGuestCounts.getOrDefault(tour.getTourId(), 0L).intValue()));
    }

    // 직원용 상품 상세 현황: 투어 정보, 결제 완료 인원, 모든 예약 현황, 기념품 구성과 현재 재고
    public StaffTourDetailResponse getStaffTourDetail(Long tourId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 투어입니다."));

        return StaffTourDetailResponse.of(
                tour,
                bookingRepository.sumGuestCountByTourIdAndBookingStatus(tourId, BookingStatus.PAID),
                bookingRepository.findAllByTourIdWithCustomer(tourId));
    }

    // 모집 중인 상품만 수정할 수 있다. 보내지 않은 값은 그대로 두고, gifts를 보내면 기념품 구성 전체를 교체한다.
    @Transactional
    public TourResponse updateTour(Long tourId, TourUpdateRequest request) {
        if (request.hasNoUpdate()) {
            throw new BadRequestException("수정할 값이 없습니다.");
        }

        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 투어입니다."));
        if (tour.getTourStatus() != TourStatus.RECRUITING) {
            throw new ConflictException("모집 중인 상품만 수정할 수 있습니다.");
        }

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : tour.getStartDate();
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : tour.getEndDate();
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("종료일은 시작일보다 빠를 수 없습니다.");
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
            throw new BadRequestException("같은 기념품을 중복해서 지정할 수 없습니다.");
        }

        Map<Long, InventoryItem> items = inventoryItemRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(InventoryItem::getItemId, Function.identity()));
        if (items.size() != itemIds.size()) {
            throw new NotFoundException("존재하지 않는 기념품 재고입니다.");
        }
        return items;
    }
}
