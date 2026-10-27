package com.mrworld.yaho.booking.dto;

import com.mrworld.yaho.booking.Booking;
import com.mrworld.yaho.common.enums.BookingStatus;
import com.mrworld.yaho.common.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

// 취소 결과. 결제 완료 예약이면 paymentStatus는 REFUNDED, 결제 전 예약이면 null이다.
@Getter
@Builder
public class BookingCancelResponse {
    private Long bookingId;
    private BookingStatus bookingStatus;
    private PaymentStatus paymentStatus;

    public static BookingCancelResponse of(Booking booking, PaymentStatus paymentStatus) {
        return BookingCancelResponse.builder()
                .bookingId(booking.getBookingId())
                .bookingStatus(booking.getBookingStatus())
                .paymentStatus(paymentStatus)
                .build();
    }
}
