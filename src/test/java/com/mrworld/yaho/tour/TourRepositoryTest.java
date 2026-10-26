package com.mrworld.yaho.tour;

import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.common.enums.Transport;
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

    private static final List<TourStatus> VISIBLE = List.of(TourStatus.RECRUITING, TourStatus.CONFIRMED);

    @Autowired
    private TourRepository tourRepository;

    @BeforeEach
    void setUp() {
        tourRepository.save(tour(Theme.GOLF, "골프 챌린지 투어", "유명 골프 리조트", "2026-10-20", 500000));
        tourRepository.save(tour(Theme.HEALING, "효도 힐링 투어", "제주 온천 휴양", "2026-10-20", 450000));
        tourRepository.save(tour(Theme.TREKKING, "트레킹 투어", null, "2026-11-20", 300000));

        // 취소 상태는 고객 조회에서 제외되어야 한다. 상태 변경 메서드가 아직 없어 리플렉션으로 설정한다.
        Tour cancelled = tour(Theme.GOLF, "취소된 골프 투어", null, "2026-10-20", 100000);
        ReflectionTestUtils.setField(cancelled, "tourStatus", TourStatus.CANCELLED);
        tourRepository.save(cancelled);
    }

    @Test
    void 목록은_노출_가능한_상태의_상품만_조회한다() {
        var result = tourRepository.findByTourStatusIn(VISIBLE, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void 상태와_시작일이_모두_일치하는_투어를_조회한다() {
        assertThat(tourRepository.findAllByTourStatusAndStartDate(TourStatus.RECRUITING, LocalDate.parse("2026-10-20")))
                .extracting(Tour::getTourName).containsExactlyInAnyOrder("골프 챌린지 투어", "효도 힐링 투어");
        assertThat(tourRepository.findAllByTourStatusAndStartDate(TourStatus.CANCELLED, LocalDate.parse("2026-10-20")))
                .extracting(Tour::getTourName).containsExactly("취소된 골프 투어");
        assertThat(tourRepository.findAllByTourStatusAndStartDate(TourStatus.RECRUITING, LocalDate.parse("2026-12-01")))
                .isEmpty();
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
        assertThat(search(null, Theme.GOLF, 400000, null)).isEmpty();
        assertThat(search(null, Theme.GOLF, 500000, LocalDate.parse("2026-10-20"))).hasSize(1);
        assertThat(search(null, null, 450000, LocalDate.parse("2026-10-20"))).hasSize(1);
    }

    private List<Tour> search(String keyword, Theme theme, Integer maxPrice, LocalDate departureDate) {
        return tourRepository.search(VISIBLE, keyword, theme, maxPrice, departureDate, PageRequest.of(0, 20)).getContent();
    }

    private Tour tour(Theme theme, String name, String description, String start, int price) {
        LocalDate startDate = LocalDate.parse(start);
        return Tour.create(theme, name, description, startDate, startDate.plusDays(3), price, Transport.PREMIUM_VAN_10);
    }
}
