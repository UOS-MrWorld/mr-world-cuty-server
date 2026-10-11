package com.mrworld.yaho.tour;

import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.common.exception.NotFoundException;
import com.mrworld.yaho.tour.dto.DrinkOptionResponse;
import com.mrworld.yaho.tour.dto.TourDetailResponse;
import com.mrworld.yaho.tour.dto.TourListRequest;
import com.mrworld.yaho.tour.dto.TourListResponse;
import com.mrworld.yaho.tour.dto.TourSearchRequest;
import com.mrworld.yaho.tour.dto.TourSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TourService {

    // 고객에게는 모집 중이거나 확정된 투어만 노출한다.
    private static final List<TourStatus> VISIBLE_STATUSES =
            List.of(TourStatus.RECRUITING, TourStatus.CONFIRMED);

    private final TourRepository tourRepository;
    private final DrinkOptionRepository drinkOptionRepository;

    public TourListResponse getTours(TourListRequest request) {
        return toListResponse(tourRepository.findByTourStatusIn(VISIBLE_STATUSES, request.toPageable()));
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

    // 목록 조회와 검색이 같은 응답 형식을 쓴다.
    private TourListResponse toListResponse(Page<Tour> page) {
        return TourListResponse.builder()
                .content(page.getContent().stream().map(this::toSummary).toList())
                .totalElements(page.getTotalElements())
                .build();
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
}
