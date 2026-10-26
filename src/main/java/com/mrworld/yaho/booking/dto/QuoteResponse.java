package com.mrworld.yaho.booking.dto;

import lombok.Builder;
import lombok.Getter;

// 모든 금액은 예약 인원 전체 기준이며 totalPrice = basePrice + optionPrice 이다.
@Getter
@Builder
public class QuoteResponse {
    private int basePrice;
    private int optionPrice;
    private int discountRate;
    private int totalPrice;
    private int expectedPaymentAmount;
}
