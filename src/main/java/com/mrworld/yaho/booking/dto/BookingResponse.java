package com.mrworld.yaho.booking.dto;

import com.mrworld.yaho.booking.Booking;
import com.mrworld.yaho.common.enums.BookingStatus;
import lombok.Builder;
import lombok.Getter;

// totalPrice는 할인 전 금액, expectedPaymentAmount는 예약 시점 할인율을 적용한 결제 예정 금액이다.
@Getter
@Builder
public class BookingResponse {
    private Long bookingId;
    private BookingStatus bookingStatus;
    private int totalPrice;
    private int discountRate;
    private int expectedPaymentAmount;

    public static BookingResponse from(Booking booking) {
        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .bookingStatus(booking.getBookingStatus())
                .totalPrice(booking.getTotalPrice())
                .discountRate(booking.getDiscountRate())
                .expectedPaymentAmount(booking.expectedPaymentAmount())
                .build();
    }
}
