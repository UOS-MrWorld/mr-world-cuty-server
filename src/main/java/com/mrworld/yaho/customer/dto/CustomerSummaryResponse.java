package com.mrworld.yaho.customer.dto;

import com.mrworld.yaho.common.enums.LoyaltyGrade;
import com.mrworld.yaho.customer.Customer;
import lombok.Builder;
import lombok.Getter;

// 직원용 고객 목록의 한 항목
@Getter
@Builder
public class CustomerSummaryResponse {
    private Long customerId;
    private String name;
    private String phoneNum;
    private long completedTourCount;
    private LoyaltyGrade loyaltyGrade;
    private boolean loyaltyDiscountApplied;

    public static CustomerSummaryResponse from(Customer customer, long completedTourCount) {
        return CustomerSummaryResponse.builder()
                .customerId(customer.getMemberId())
                .name(customer.getMember().getName())
                .phoneNum(customer.getMember().getPhoneNum())
                .completedTourCount(completedTourCount)
                .loyaltyGrade(customer.getLoyaltyGrade())
                .loyaltyDiscountApplied(customer.isLoyaltyDiscountApplied())
                .build();
    }
}
