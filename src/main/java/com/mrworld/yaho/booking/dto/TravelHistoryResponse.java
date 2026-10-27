package com.mrworld.yaho.booking.dto;

import com.mrworld.yaho.booking.Booking;
import com.mrworld.yaho.common.enums.TourLevel;
import com.mrworld.yaho.common.enums.TourStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

// 여행 내역의 한 항목. tourStatus로 예약한 여행(RECRUITING·CONFIRMED)과 완료된 여행(COMPLETED)을 구분한다.
@Getter
@Builder
public class TravelHistoryResponse {
    private Long bookingId;
    private String tourName;
    private LocalDate startDate;
    private LocalDate endDate;
    private TourLevel tourLevel;
    private int paidAmount;
    private TourStatus tourStatus;

    // 결제 완료 예약의 할인율은 결제에 적용한 값이라, 예약 금액으로 계산한 값이 실제 결제 금액과 같다.
    public static TravelHistoryResponse from(Booking booking) {
        return TravelHistoryResponse.builder()
                .bookingId(booking.getBookingId())
                .tourName(booking.getTour().getTourName())
                .startDate(booking.getTour().getStartDate())
                .endDate(booking.getTour().getEndDate())
                .tourLevel(booking.getTourLevel())
                .paidAmount(booking.expectedPaymentAmount())
                .tourStatus(booking.getTour().getTourStatus())
                .build();
    }
}
