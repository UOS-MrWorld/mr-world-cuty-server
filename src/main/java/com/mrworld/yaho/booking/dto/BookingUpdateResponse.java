package com.mrworld.yaho.booking.dto;

import com.mrworld.yaho.booking.Booking;
import com.mrworld.yaho.common.enums.BookingStatus;
import com.mrworld.yaho.common.enums.HotelGrade;
import com.mrworld.yaho.common.enums.MealType;
import com.mrworld.yaho.common.enums.TourLevel;
import com.mrworld.yaho.common.enums.Transport;
import lombok.Builder;
import lombok.Getter;

// 수정 후 옵션과 다시 계산한 금액. discountRate는 예약 생성 시점 값을 유지한다.
@Getter
@Builder
public class BookingUpdateResponse {
    private Long bookingId;
    private BookingStatus bookingStatus;
    private TourLevel tourLevel;
    private int guestCount;
    private HotelGrade hotelGrade;
    private MealType mealType;
    private Transport transport;
    private int totalPrice;
    private int discountRate;
    private int expectedPaymentAmount;

    public static BookingUpdateResponse from(Booking booking) {
        return BookingUpdateResponse.builder()
                .bookingId(booking.getBookingId())
                .bookingStatus(booking.getBookingStatus())
                .tourLevel(booking.getTourLevel())
                .guestCount(booking.getGuestCount())
                .hotelGrade(booking.getHotelGrade())
                .mealType(booking.getMealType())
                .transport(booking.getTransport())
                .totalPrice(booking.getTotalPrice())
                .discountRate(booking.getDiscountRate())
                .expectedPaymentAmount(booking.expectedPaymentAmount())
                .build();
    }
}
