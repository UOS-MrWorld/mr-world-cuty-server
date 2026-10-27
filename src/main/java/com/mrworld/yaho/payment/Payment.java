package com.mrworld.yaho.payment;

import com.mrworld.yaho.booking.Booking;
import com.mrworld.yaho.common.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;

import java.time.LocalDateTime;

// 예약에 대한 결제 시도. 실패 후 다시 결제할 수 있어 한 예약에 여러 건이 쌓이지만, SUCCESS는 최대 하나다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    // 할인을 적용한 결제 금액
    @Column(nullable = false)
    private int amount;

    // 결제 완료 일시. 실패한 결제는 null이다.
    private LocalDateTime paidAt;

    public static Payment success(Booking booking, int amount, LocalDateTime paidAt) {
        return Payment.builder()
                .booking(booking)
                .paymentStatus(PaymentStatus.SUCCESS)
                .amount(amount)
                .paidAt(paidAt)
                .build();
    }

    public static Payment failed(Booking booking, int amount) {
        return Payment.builder()
                .booking(booking)
                .paymentStatus(PaymentStatus.FAILED)
                .amount(amount)
                .build();
    }
}
