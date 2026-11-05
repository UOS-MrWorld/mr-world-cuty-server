package com.mrworld.yaho.customer.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LoyaltyGradeBatchUpdateResponse {
    private int updatedCount;
    private List<CustomerSummaryResponse> customers;

    public static LoyaltyGradeBatchUpdateResponse of(List<CustomerSummaryResponse> customers) {
        return LoyaltyGradeBatchUpdateResponse.builder()
                .updatedCount(customers.size())
                .customers(customers)
                .build();
    }
}
