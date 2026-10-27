package com.mrworld.yaho.tour;

import com.mrworld.yaho.common.dto.PageResponse;
import com.mrworld.yaho.common.dto.PagingRequest;
import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourLevel;
import com.mrworld.yaho.common.enums.Transport;
import com.mrworld.yaho.common.exception.NotFoundException;
import com.mrworld.yaho.tour.dto.TourDetailResponse;
import com.mrworld.yaho.tour.dto.TourSearchRequest;
import com.mrworld.yaho.tour.dto.TourSummaryResponse;
import com.mrworld.yaho.tour.dto.VoiceSearchRequest;
import com.mrworld.yaho.tour.dto.VoiceSearchResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import java.util.stream.LongStream;

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

    @Test
    void 공백_키워드는_조건_없음으로_검색한다() {
        when(tourRepository.search(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        TourSearchRequest request = new TourSearchRequest();
        request.setKeyword("   ");

        tourService.searchTours(request);

        verify(tourRepository).search(any(), eq(null), eq(null), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void 음성_문장에_테마_단어가_있으면_그_테마로_검색한다() {
        Tour healing = tour(2L, Theme.HEALING, "효도 힐링 투어", "제주 온천 휴양", "2026-11-20");
        when(tourRepository.search(any(), eq(null), eq(Theme.HEALING), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(healing)));

        VoiceSearchResponse response = tourService.voiceSearch(voiceRequest("  부모님이랑 갈 힐링 여행 찾아줘 ", null, null));

        assertThat(response.getRecognizedText()).isEqualTo("부모님이랑 갈 힐링 여행 찾아줘");
        assertThat(response.getTours()).extracting(TourSummaryResponse::getTourName).containsExactly("효도 힐링 투어");
    }

    @Test
    void 음성_검색의_가격과_날짜_조건은_검색에_그대로_전달된다() {
        when(tourRepository.search(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        LocalDate departureDate = LocalDate.parse("2026-10-20");

        tourService.voiceSearch(voiceRequest("골프 여행 보여줘", 500000, departureDate));

        verify(tourRepository).search(any(), eq(null), eq(Theme.GOLF), eq(500000), eq(departureDate),
                any(Pageable.class));
    }

    @Test
    void 테마_단어가_없으면_문장에_단어가_들어_있는_상품만_반환한다() {
        Tour jeju = tour(2L, Theme.HEALING, "효도 힐링 투어", "제주 온천 휴양", "2026-11-20");
        Tour golf = tour(3L, Theme.GOLF, "골프 챌린지 투어", "유명 골프 리조트", "2026-10-20");
        when(tourRepository.search(any(), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(golf, jeju)));

        VoiceSearchResponse response = tourService.voiceSearch(voiceRequest("제주도를 가고 싶어", null, null));

        assertThat(response.getTours()).extracting(TourSummaryResponse::getTourId).containsExactly(2L);
    }

    @Test
    void 테마_단어가_없으면_후보를_자르지_않고_모두_조회한다() {
        when(tourRepository.search(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        tourService.voiceSearch(voiceRequest("제주도를 가고 싶어", null, null));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(tourRepository).search(any(), eq(null), eq(null), eq(null), eq(null), captor.capture());
        assertThat(captor.getValue().isPaged()).isFalse();
    }

    @Test
    void 테마_단어가_없을_때_일치하는_상품이_많으면_20개까지만_반환한다() {
        List<Tour> jejuTours = LongStream.rangeClosed(1, 25)
                .mapToObj(id -> tour(id, Theme.HEALING, "제주 투어 " + id, null,
                        LocalDate.parse("2026-11-01").plusDays(id).toString()))
                .toList();
        when(tourRepository.search(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(jejuTours));

        VoiceSearchResponse response = tourService.voiceSearch(voiceRequest("제주도를 가고 싶어", null, null));

        assertThat(response.getTours()).hasSize(20);
        assertThat(response.getTours().get(0).getTourId()).isEqualTo(1L);
    }

    @Test
    void 일치하는_상품이_없으면_빈_목록을_반환한다() {
        Tour golf = tour(3L, Theme.GOLF, "골프 챌린지 투어", "유명 골프 리조트", "2026-10-20");
        when(tourRepository.search(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(golf)));

        VoiceSearchResponse response = tourService.voiceSearch(voiceRequest("남극 가고 싶어", null, null));

        assertThat(response.getTours()).isEmpty();
        assertThat(response.getRecognizedText()).isEqualTo("남극 가고 싶어");
    }

    @Test
    void 테마_단어가_여러_개면_각각_검색해_시작일_순으로_합친다() {
        Tour healing = tour(2L, Theme.HEALING, "효도 힐링 투어", "제주 온천 휴양", "2026-11-20");
        Tour golf = tour(3L, Theme.GOLF, "골프 챌린지 투어", "유명 골프 리조트", "2026-10-20");
        when(tourRepository.search(any(), any(), eq(Theme.HEALING), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(healing)));
        when(tourRepository.search(any(), any(), eq(Theme.GOLF), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(golf)));

        VoiceSearchResponse response = tourService.voiceSearch(voiceRequest("골프랑 힐링 둘 다 찾아줘", null, null));

        assertThat(response.getTours()).extracting(TourSummaryResponse::getTourId).containsExactly(3L, 2L);
    }

    @Test
    void 음성_검색_문장이_공백이면_예외가_발생한다() {
        assertThatThrownBy(() -> tourService.voiceSearch(voiceRequest("   ", null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private VoiceSearchRequest voiceRequest(String queryText, Integer maxPrice, LocalDate departureDate) {
        VoiceSearchRequest request = new VoiceSearchRequest();
        request.setQueryText(queryText);
        request.setMaxPrice(maxPrice);
        request.setDepartureDate(departureDate);
        return request;
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
