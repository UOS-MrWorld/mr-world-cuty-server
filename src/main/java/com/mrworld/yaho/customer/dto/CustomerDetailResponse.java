package com.mrworld.yaho.customer.dto;

import com.mrworld.yaho.common.enums.LoyaltyGrade;
import com.mrworld.yaho.customer.Customer;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 직원용 고객 상세. loyaltyDiscountAppliedAt은 할인을 적용한 적이 없으면 null이다.
@Getter
@Builder
public class CustomerDetailResponse {
    private Long customerId;
    private String loginId;
    private String name;
    private String phoneNum;
    private String address;
    private long completedTourCount;
    private LoyaltyGrade loyaltyGrade;
    private boolean loyaltyDiscountApplied;
    private LocalDateTime loyaltyDiscountAppliedAt;

    public static CustomerDetailResponse from(Customer customer, long completedTourCount) {
        return CustomerDetailResponse.builder()
                .customerId(customer.getMemberId())
                .loginId(customer.getMember().getLoginId())
                .name(customer.getMember().getName())
                .phoneNum(customer.getMember().getPhoneNum())
                .address(customer.getMember().getAddress())
                .completedTourCount(completedTourCount)
                .loyaltyGrade(customer.getLoyaltyGrade())
                .loyaltyDiscountApplied(customer.isLoyaltyDiscountApplied())
                .loyaltyDiscountAppliedAt(customer.getLoyaltyDiscountAppliedAt())
                .build();
    }
}
