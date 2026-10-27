package com.mrworld.yaho.payment;

import com.mrworld.yaho.common.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("""
            select count(p) > 0 from Payment p
            where p.booking.bookingId = :bookingId and p.paymentStatus = :status
            """)
    boolean existsByBookingIdAndPaymentStatus(@Param("bookingId") Long bookingId, @Param("status") PaymentStatus status);

    @Query("""
            select p from Payment p
            where p.booking.bookingId = :bookingId and p.paymentStatus = :status
            """)
    Optional<Payment> findByBookingIdAndPaymentStatus(@Param("bookingId") Long bookingId, @Param("status") PaymentStatus status);

    // 예약의 가장 최근 결제. 결제 시도가 없으면 비어 있다.
    Optional<Payment> findTopByBookingBookingIdOrderByPaymentIdDesc(Long bookingId);
}
