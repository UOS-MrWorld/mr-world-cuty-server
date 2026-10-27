package com.mrworld.yaho.customer.dto;

import com.mrworld.yaho.common.enums.LoyaltyGrade;
import lombok.Builder;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

// 코드에 고정된 단골 등급 기준. 직원이 수정할 수 없다.
@Getter
@Builder
public class LoyaltyPolicyResponse {
    private List<Policy> policies;

    public static LoyaltyPolicyResponse from(LoyaltyGrade[] grades) {
        return LoyaltyPolicyResponse.builder()
                .policies(Arrays.stream(grades)
                        .map(grade -> Policy.builder()
                                .grade(grade)
                                .requiredTourCount(grade.getRequiredTourCount())
                                .discountRate(grade.getDiscountRate())
                                .build())
                        .toList())
                .build();
    }

    @Getter
    @Builder
    public static class Policy {
        private LoyaltyGrade grade;
        private int requiredTourCount;
        private int discountRate;
    }
}
