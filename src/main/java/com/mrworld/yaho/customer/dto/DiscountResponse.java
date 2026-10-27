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
}
