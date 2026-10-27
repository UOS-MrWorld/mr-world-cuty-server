package com.mrworld.yaho.payment;

import com.mrworld.yaho.common.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByBookingBookingIdAndPaymentStatus(Long bookingId, PaymentStatus paymentStatus);

    Optional<Payment> findByBookingBookingIdAndPaymentStatus(Long bookingId, PaymentStatus paymentStatus);

    // 예약의 가장 최근 결제. 결제 시도가 없으면 비어 있다.
    Optional<Payment> findTopByBookingBookingIdOrderByPaymentIdDesc(Long bookingId);
}
