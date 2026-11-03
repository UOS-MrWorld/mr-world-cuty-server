package com.mrworld.yaho.tour;

import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourLevel;
import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.common.enums.Transport;
import com.mrworld.yaho.common.exception.BadRequestException;
import com.mrworld.yaho.common.exception.ConflictException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TourTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 1, 1);

    @Test
    void 모집_중_투어는_확정하거나_취소할_수_있고_확정_투어는_완료할_수_있다() {
        Tour confirmed = tour(Theme.GOLF, TODAY.plusDays(30));
        confirmed.confirm();
        assertThat(confirmed.getTourStatus()).isEqualTo(TourStatus.CONFIRMED);
        confirmed.complete();
        assertThat(confirmed.getTourStatus()).isEqualTo(TourStatus.COMPLETED);

        Tour cancelled = tour(Theme.GOLF, TODAY.plusDays(30));
        cancelled.cancel();
        assertThat(cancelled.getTourStatus()).isEqualTo(TourStatus.CANCELLED);
    }

    @Test
    void 허용되지_않은_상태_전환은_거부한다() {
        Tour recruiting = tour(Theme.GOLF, TODAY.plusDays(30));
        assertThatThrownBy(recruiting::complete).isInstanceOf(IllegalStateException.class);

        Tour cancelled = tour(Theme.GOLF, TODAY.plusDays(30));
        cancelled.cancel();
        assertThatThrownBy(cancelled::confirm).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(cancelled::complete).isInstanceOf(IllegalStateException.class);
        assertThat(cancelled.getTourStatus()).isEqualTo(TourStatus.CANCELLED);
    }

    @Test
    void 모집_중이_아닌_투어는_예약하거나_수정할_수_없다() {
        Tour tour = tour(Theme.GOLF, TODAY.plusDays(30));
        tour.confirm();

        assertThatThrownBy(() -> tour.validateBookable(TODAY)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(tour::validateUpdatable).isInstanceOf(ConflictException.class);
    }

    @Test
    void 예약은_시작_7일_전_당일까지_가능하다() {
        assertThatCode(() -> tour(Theme.GOLF, TODAY.plusDays(7)).validateBookable(TODAY))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> tour(Theme.GOLF, TODAY.plusDays(6)).validateBookable(TODAY))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void 테마별_선택_가능한_투어_등급을_돌려준다() {
        assertThat(tour(Theme.HONEYMOON, TODAY).availableLevels()).containsExactly(TourLevel.GRAND, TourLevel.PREMIUM);
        assertThat(tour(Theme.HEALING, TODAY).availableLevels()).containsExactly(TourLevel.GRAND, TourLevel.PREMIUM);
        assertThat(tour(Theme.GOLF, TODAY).availableLevels()).containsExactly(TourLevel.values());
        assertThat(tour(Theme.TREKKING, TODAY).availableLevels()).containsExactly(TourLevel.values());
    }

    @Test
    void 인원이_정해진_테마만_예약_인원을_제한한다() {
        assertThat(Theme.HONEYMOON.allowsGuestCount(2)).isTrue();
        assertThat(Theme.HONEYMOON.allowsGuestCount(3)).isFalse();
        assertThat(Theme.GOLF.allowsGuestCount(3)).isTrue();
        assertThat(Theme.HEALING.allowsGuestCount(1)).isTrue();
    }

    private Tour tour(Theme theme, LocalDate startDate) {
        return Tour.create(theme, "투어", null, startDate, startDate.plusDays(3), 500_000, Transport.PREMIUM_VAN_10);
    }
}
