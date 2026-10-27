package com.mrworld.yaho.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// 예약 생성 요청. 옵션 항목은 가격 계산(QuoteRequest)과 같고 투어 ID만 본문으로 받는다.
@Getter
@Setter
public class BookingRequest extends QuoteRequest {
    @NotNull(message = "투어 ID는 필수입니다.")
    private Long tourId;
}
