package com.mrworld.yaho.tour;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TourRepositoryTest {

    private static final List<Tour.Status> VISIBLE = List.of(Tour.Status.RECRUITING, Tour.Status.CONFIRMED);

    @Autowired
    private TourRepository tourRepository;

    @BeforeEach
    void setUp() {
        tourRepository.save(tour(Tour.Theme.GOLF, "골프 챌린지 투어", "유명 골프 리조트", "2026-10-20", 500000));
        tourRepository.save(tour(Tour.Theme.HEALING, "효도 힐링 투어", "제주 온천 휴양", "2026-10-20", 450000));
        tourRepository.save(tour(Tour.Theme.TREKKING, "트레킹 투어", null, "2026-11-20", 300000));

        // 취소 상태는 고객 조회에서 제외되어야 한다. 상태 변경 메서드가 아직 없어 리플렉션으로 설정한다.
        Tour cancelled = tour(Tour.Theme.GOLF, "취소된 골프 투어", null, "2026-10-20", 100000);
        ReflectionTestUtils.setField(cancelled, "tourStatus", Tour.Status.CANCELLED);
        tourRepository.save(cancelled);
    }

    @Test
    void 목록은_노출_가능한_상태의_상품만_조회한다() {
        var result = tourRepository.findByTourStatusIn(VISIBLE, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void 조건이_없으면_노출_가능한_상품을_모두_검색한다() {
        assertThat(search(null, null, null, null)).hasSize(3);
    }

    @Test
    void 키워드는_상품명과_설명에서_검색한다() {
        assertThat(search("제주", null, null, null)).extracting(Tour::getTourName).containsExactly("효도 힐링 투어");
        assertThat(search("골프", null, null, null)).extracting(Tour::getTourName).containsExactly("골프 챌린지 투어");
    }

    @Test
    void 여러_조건은_AND로_적용된다() {
        assertThat(search(null, Tour.Theme.GOLF, 400000, null)).isEmpty();
        assertThat(search(null, Tour.Theme.GOLF, 500000, LocalDate.parse("2026-10-20"))).hasSize(1);
        assertThat(search(null, null, 450000, LocalDate.parse("2026-10-20"))).hasSize(1);
    }

    private List<Tour> search(String keyword, Tour.Theme theme, Integer maxPrice, LocalDate departureDate) {
        return tourRepository.search(VISIBLE, keyword, theme, maxPrice, departureDate, PageRequest.of(0, 20)).getContent();
    }

    private Tour tour(Tour.Theme theme, String name, String description, String start, int price) {
        LocalDate startDate = LocalDate.parse(start);
        return Tour.create(theme, name, description, startDate, startDate.plusDays(3), price, Tour.Transport.PREMIUM_VAN_10);
    }
}
