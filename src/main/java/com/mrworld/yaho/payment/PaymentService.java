package com.mrworld.yaho.payment;

import com.mrworld.yaho.booking.Booking;
import com.mrworld.yaho.booking.BookingRepository;
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
@Transactional(readOnly = true)
public class PaymentService {

    // Mock 결제 시스템은 이 카드번호만 승인하고 나머지는 모두 거절한다.
    private static final String APPROVED_CARD_NUMBER = "1";

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    // 승인·거절 모두 결제 기록을 남긴다. 거절되면 예약은 PENDING으로 남아 다시 결제할 수 있다.
    // 같은 예약의 결제·취소 요청은 예약 행을 잠가 차례로 처리한다. 잠그지 않으면 동시 요청이 모두
    // 성공 결제 조회를 통과해 성공 결제가 여러 건 저장된다. (PaymentConcurrencyTest)
    @Transactional
    public PaymentResponse pay(Long customerId, PaymentRequest request) {
        Booking booking = bookingRepository.findByIdForUpdate(request.getBookingId())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 예약입니다."));
        if (!booking.isOwnedBy(customerId)) {
            throw new ForbiddenException("본인의 예약이 아닙니다.");
        }
        if (paymentRepository.existsByBookingBookingIdAndPaymentStatus(booking.getBookingId(), PaymentStatus.SUCCESS)) {
            throw new ConflictException("이미 결제가 완료된 예약입니다.");
        }
        booking.validatePayable(LocalDate.now());

        int discountRate = booking.getCustomer().currentDiscountRate();
        int amount = Booking.calculatePaymentAmount(booking.getTotalPrice(), discountRate);

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
        Payment payment = paymentRepository.findByBookingBookingIdAndPaymentStatus(booking.getBookingId(), PaymentStatus.SUCCESS)
                .orElseThrow(() -> new IllegalStateException("환불할 결제가 없습니다."));
        payment.refund();
    }
}
