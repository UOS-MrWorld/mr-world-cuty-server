package com.mrworld.yaho.customer.dto;

import lombok.Builder;
import lombok.Getter;

// 일괄 적용 결과. targetCount = appliedCount + skippedCount
@Getter
@Builder
public class DiscountResponse {
    private int targetCount;
    private int appliedCount;
    private int skippedCount;

    public static DiscountResponse of(int targetCount, int appliedCount) {
        return DiscountResponse.builder()
                .targetCount(targetCount)
                .appliedCount(appliedCount)
                .skippedCount(targetCount - appliedCount)
                .build();
    }
}
