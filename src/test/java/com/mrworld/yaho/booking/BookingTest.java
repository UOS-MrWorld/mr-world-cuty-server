package com.mrworld.yaho.booking;

import com.mrworld.yaho.common.enums.BookingStatus;
import com.mrworld.yaho.common.enums.HotelGrade;
import com.mrworld.yaho.common.enums.MealType;
import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourLevel;
import com.mrworld.yaho.common.enums.Transport;
import com.mrworld.yaho.common.exception.BadRequestException;
import com.mrworld.yaho.common.exception.ConflictException;
import com.mrworld.yaho.tour.Tour;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 1, 1);

    @Test
    void 결제_금액은_할인율을_뺀_금액이고_원_단위_미만은_버린다() {
        assertThat(Booking.calculatePaymentAmount(1_000_000, 10)).isEqualTo(900_000);
        assertThat(Booking.calculatePaymentAmount(1_000_000, 0)).isEqualTo(1_000_000);
        assertThat(Booking.calculatePaymentAmount(999, 10)).isEqualTo(899);
    }

    @Test
    void 저장된_금액과_할인율로_예상_결제_금액을_계산한다() {
        assertThat(booking(TODAY.plusDays(30), 10).expectedPaymentAmount()).isEqualTo(900_000);
    }

    @Test
    void 이미_결제한_예약은_다시_결제할_수_없다() {
        Booking booking = booking(TODAY.plusDays(30), 0);
        booking.pay(0);

        assertThatThrownBy(() -> booking.pay(0))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("결제할 수 없는 예약입니다.");
    }

    @Test
    void 취소한_예약은_결제할_수_없다() {
        Booking booking = booking(TODAY.plusDays(30), 0);
        booking.cancel();

        assertThatThrownBy(() -> booking.pay(0)).isInstanceOf(BadRequestException.class);
        assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void 이미_취소한_예약은_다시_취소할_수_없다() {
        Booking booking = booking(TODAY.plusDays(30), 0);
        booking.cancel();

        assertThatThrownBy(booking::cancel)
                .isInstanceOf(BadRequestException.class)
                .hasMessage("이미 취소된 예약입니다.");
    }

    @Test
    void 결제한_예약은_옵션을_수정할_수_없다() {
        Booking booking = booking(TODAY.plusDays(30), 0);
        booking.pay(0);

        assertThatThrownBy(() -> booking.updateOptions(TourLevel.GRAND, 2, HotelGrade.FOUR_STAR,
                MealType.LOCAL_RESTAURANT, Transport.PREMIUM_VAN_10, 2_000_000))
                .isInstanceOf(ConflictException.class);
        assertThat(booking.getGuestCount()).isEqualTo(1);
    }

    @Test
    void 결제와_취소는_시작_7일_전_당일까지_가능하고_다음_날부터_마감이다() {
        LocalDate lastDayStart = TODAY.plusDays(7);
        assertThatCode(() -> booking(lastDayStart, 0).validatePayable(TODAY)).doesNotThrowAnyException();
        assertThatCode(() -> booking(lastDayStart, 0).validateCancellable(TODAY)).doesNotThrowAnyException();

        LocalDate closedStart = TODAY.plusDays(6);
        assertThatThrownBy(() -> booking(closedStart, 0).validatePayable(TODAY))
                .hasMessage("결제 마감 기간이 지난 투어입니다.");
        assertThatThrownBy(() -> booking(closedStart, 0).validateCancellable(TODAY))
                .hasMessage("취소 마감 기간이 지난 투어입니다.");
    }

    private Booking booking(LocalDate tourStartDate, int discountRate) {
        Tour tour = Tour.create(Theme.GOLF, "골프 투어", null, tourStartDate, tourStartDate.plusDays(3),
                500_000, Transport.PREMIUM_VAN_10);
        return Booking.create(tour, null, TourLevel.CLASSIC, 1, HotelGrade.THREE_STAR,
                MealType.LUNCH_BOX, Transport.PREMIUM_VAN_10, 1_000_000, discountRate);
    }
}
