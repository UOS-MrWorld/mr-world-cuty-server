package com.mrworld.yaho.tour;

import com.mrworld.yaho.common.exception.NotFoundException;
import com.mrworld.yaho.tour.dto.TourDetailResponse;
import com.mrworld.yaho.tour.dto.TourListRequest;
import com.mrworld.yaho.tour.dto.TourListResponse;
import com.mrworld.yaho.tour.dto.TourSearchRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TourServiceTest {

    @Mock
    private TourRepository tourRepository;

    @InjectMocks
    private TourService tourService;

    @Test
    void 존재하지_않는_투어_상세는_예외가_발생한다() {
        when(tourRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tourService.getTourDetails(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void 상세_조회는_테마에_따른_선택_가능_등급을_포함한다() {
        when(tourRepository.findById(1L)).thenReturn(Optional.of(tour(Tour.Theme.HEALING)));

        TourDetailResponse response = tourService.getTourDetails(1L);

        assertThat(response.getTourId()).isEqualTo(1L);
        assertThat(response.getAvailableTourLevels()).containsExactly(Tour.TourLevel.GRAND, Tour.TourLevel.PREMIUM);
    }

    @Test
    void 목록_조회는_요약_정보와_전체_개수를_반환한다() {
        Tour tour = tour(Tour.Theme.GOLF);
        when(tourRepository.findByTourStatusIn(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tour), PageRequest.of(0, 1), 12));

        TourListResponse response = tourService.getTours(new TourListRequest());

        assertThat(response.getTotalElements()).isEqualTo(12);
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getTourName()).isEqualTo("테스트 투어");
    }

    @Test
    void 공백_키워드는_조건_없음으로_검색한다() {
        when(tourRepository.search(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        TourSearchRequest request = new TourSearchRequest();
        request.setKeyword("   ");

        tourService.searchTours(request);

        verify(tourRepository).search(any(), eq(null), eq(null), eq(null), eq(null), any(Pageable.class));
    }

    private Tour tour(Tour.Theme theme) {
        Tour tour = Tour.create(theme, "테스트 투어", null, LocalDate.parse("2026-10-20"),
                LocalDate.parse("2026-10-23"), 500000, Tour.Transport.PREMIUM_VAN_10);
        ReflectionTestUtils.setField(tour, "id", 1L);
        return tour;
    }
}
