package com.mrworld.yaho.payment;

import com.mrworld.yaho.booking.Booking;
import com.mrworld.yaho.booking.BookingRepository;
import com.mrworld.yaho.common.enums.BookingStatus;
import com.mrworld.yaho.common.enums.PaymentStatus;
import com.mrworld.yaho.common.exception.ConflictException;
import com.mrworld.yaho.common.exception.ForbiddenException;
import com.mrworld.yaho.common.exception.NotFoundException;
import com.mrworld.yaho.payment.dto.PaymentRequest;
import com.mrworld.yaho.payment.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 결제 전(PENDING) 예약의 Mock 결제. 금액은 결제 시점 고객의 단골 할인율로 다시 계산한다.
@Service
@RequiredArgsConstructor
public class PaymentService {

    // Mock 결제 시스템은 이 카드번호만 승인하고 나머지는 모두 거절한다.
    private static final String APPROVED_CARD_NUMBER = "1";

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    // 승인·거절 모두 결제 기록을 남긴다. 거절되면 예약은 PENDING으로 남아 다시 결제할 수 있다.
    @Transactional
    public PaymentResponse pay(Long customerId, PaymentRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 예약입니다."));
        if (!booking.isOwnedBy(customerId)) {
            throw new ForbiddenException("본인의 예약이 아닙니다.");
        }
        if (paymentRepository.existsByBookingIdAndPaymentStatus(booking.getBookingId(), PaymentStatus.SUCCESS)) {
            throw new ConflictException("이미 결제가 완료된 예약입니다.");
        }
        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("결제할 수 없는 예약입니다.");
        }
        if (booking.getTour().isBookingClosed(LocalDate.now())) {
            throw new IllegalArgumentException("결제 마감 기간이 지난 투어입니다.");
        }

        int discountRate = booking.getCustomer().currentDiscountRate();
        int amount = booking.getTotalPrice() * (100 - discountRate) / 100;

        if (!APPROVED_CARD_NUMBER.equals(request.getCardNumber())) {
            return PaymentResponse.from(paymentRepository.save(Payment.failed(booking, amount)));
        }

        Payment payment = paymentRepository.save(Payment.success(booking, amount, LocalDateTime.now()));
        booking.pay(discountRate);
        return PaymentResponse.from(payment);
    }

    // 예약의 성공 결제를 환불한다. 고객의 예약 취소와 인원 미달로 인한 투어 취소에서 함께 쓴다.
    @Transactional
    public void refund(Booking booking) {
        Payment payment = paymentRepository.findByBookingIdAndPaymentStatus(booking.getBookingId(), PaymentStatus.SUCCESS)
                .orElseThrow(() -> new IllegalStateException("환불할 결제가 없습니다."));
        payment.refund();
    }
}
