package com.mrworld.yaho.booking.dto;

import com.mrworld.yaho.booking.Booking;
import com.mrworld.yaho.common.enums.BookingStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

// 내 예약 목록의 한 항목
@Getter
@Builder
public class BookingSummaryResponse {
    private Long bookingId;
    private Long tourId;
    private String tourName;
    private LocalDate startDate;
    private BookingStatus bookingStatus;
    private int totalPrice;
    private int discountRate;

    public static BookingSummaryResponse from(Booking booking) {
        return BookingSummaryResponse.builder()
                .bookingId(booking.getBookingId())
                .tourId(booking.getTour().getTourId())
                .tourName(booking.getTour().getTourName())
                .startDate(booking.getTour().getStartDate())
                .bookingStatus(booking.getBookingStatus())
                .totalPrice(booking.getTotalPrice())
                .discountRate(booking.getDiscountRate())
                .build();
    }
}
