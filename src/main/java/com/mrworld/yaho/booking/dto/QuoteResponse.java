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

    // optionPrice = totalPrice - basePrice, expectedPaymentAmount = totalPrice × (100 - discountRate) / 100
    public static QuoteResponse of(int basePrice, int totalPrice, int discountRate) {
        return QuoteResponse.builder()
                .basePrice(basePrice)
                .optionPrice(totalPrice - basePrice)
                .discountRate(discountRate)
                .totalPrice(totalPrice)
                .expectedPaymentAmount(totalPrice * (100 - discountRate) / 100)
                .build();
    }
}
