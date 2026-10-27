package com.mrworld.yaho.tour;

import com.mrworld.yaho.common.dto.PageResponse;
import com.mrworld.yaho.common.dto.PagingRequest;
import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourLevel;
import com.mrworld.yaho.common.enums.Transport;
import com.mrworld.yaho.common.exception.NotFoundException;
import com.mrworld.yaho.tour.dto.TourDetailResponse;
import com.mrworld.yaho.tour.dto.TourSummaryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TourServiceTest {

    @Mock
    private TourRepository tourRepository;

    @Mock
    private DrinkOptionRepository drinkOptionRepository;

    @InjectMocks
    private TourService tourService;

    @Test
    void 존재하지_않는_투어_상세는_예외가_발생한다() {
        when(tourRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tourService.getTourDetail(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void 상세_조회는_선택_가능한_음료_옵션을_포함한다() {
        when(tourRepository.findById(1L)).thenReturn(Optional.of(tour(Theme.GOLF)));
        DrinkOption coffee = DrinkOption.create("COFFEE", 5_000);
        ReflectionTestUtils.setField(coffee, "optionId", 3L);
        when(drinkOptionRepository.findAll(any(Sort.class))).thenReturn(List.of(coffee));

        TourDetailResponse response = tourService.getTourDetail(1L);

        assertThat(response.getDrinkOptions()).hasSize(1);
        assertThat(response.getDrinkOptions().get(0).getOptionId()).isEqualTo(3L);
        assertThat(response.getDrinkOptions().get(0).getDrinkName()).isEqualTo("COFFEE");
        assertThat(response.getDrinkOptions().get(0).getDrinkPrice()).isEqualTo(5_000);
    }

    @Test
    void 상세_조회는_테마에_따른_선택_가능_등급을_포함한다() {
        when(tourRepository.findById(1L)).thenReturn(Optional.of(tour(Theme.HEALING)));

        TourDetailResponse response = tourService.getTourDetail(1L);

        assertThat(response.getTourId()).isEqualTo(1L);
        assertThat(response.getAvailableTourLevels()).containsExactly(TourLevel.GRAND, TourLevel.PREMIUM);
    }

    @Test
    void 목록_조회는_요약_정보와_전체_개수를_반환한다() {
        Tour tour = tour(Theme.GOLF);
        when(tourRepository.findByTourStatusIn(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tour), PageRequest.of(0, 1), 12));

        PageResponse<TourSummaryResponse> response = tourService.getTours(new PagingRequest());

        assertThat(response.getTotalElements()).isEqualTo(12);
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getTourName()).isEqualTo("테스트 투어");
    }

    private Tour tour(Long tourId, Theme theme, String tourName, String description, String startDate) {
        LocalDate start = LocalDate.parse(startDate);
        Tour tour = Tour.create(theme, tourName, description, start, start.plusDays(3), 500000,
                Transport.PREMIUM_VAN_10);
        ReflectionTestUtils.setField(tour, "tourId", tourId);
        return tour;
    }

    private Tour tour(Theme theme) {
        Tour tour = Tour.create(theme, "테스트 투어", null, LocalDate.parse("2026-10-20"),
                LocalDate.parse("2026-10-23"), 500000, Transport.PREMIUM_VAN_10);
        ReflectionTestUtils.setField(tour, "tourId", 1L);
        return tour;
    }

}
