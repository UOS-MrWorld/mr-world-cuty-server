package com.mrworld.yaho.payment.dto;

import com.mrworld.yaho.common.enums.BookingStatus;
import com.mrworld.yaho.common.enums.PaymentStatus;
import com.mrworld.yaho.payment.Payment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 결제 시도 결과. 거절되면 paymentStatus는 FAILED, bookingStatus는 PENDING 그대로다.
@Getter
@Builder
public class PaymentResponse {
    private Long paymentId;
    private Long bookingId;
    private PaymentStatus paymentStatus;
    private int amount;
    private LocalDateTime paidAt;
    private BookingStatus bookingStatus;

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .bookingId(payment.getBooking().getBookingId())
                .paymentStatus(payment.getPaymentStatus())
                .amount(payment.getAmount())
                .paidAt(payment.getPaidAt())
                .bookingStatus(payment.getBooking().getBookingStatus())
                .build();
    }
}
