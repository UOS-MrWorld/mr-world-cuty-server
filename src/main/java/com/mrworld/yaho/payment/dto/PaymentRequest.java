package com.mrworld.yaho.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRequest {
    @NotNull(message = "예약 ID는 필수입니다.")
    private Long bookingId;

    // Mock 결제용 값. "1"이면 승인, 그 외는 거절된다. 저장하지 않는다.
    @NotBlank(message = "카드번호는 필수입니다.")
    private String cardNumber;
}
