package com.mrworld.yaho.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Customer {

    // 등급 기준 횟수와 할인율은 직원이 수정할 수 없는 고정값이며, 팀 확정 전까지 임시 값이다.
    public enum LoyaltyGrade {
        NORMAL(0, 0),
        VIP(5, 10);

        private final int requiredTourCount;
        private final int discountRate;

        LoyaltyGrade(int requiredTourCount, int discountRate) {
            this.requiredTourCount = requiredTourCount;
            this.discountRate = discountRate;
        }

        public int getRequiredTourCount() {
            return requiredTourCount;
        }

        public int getDiscountRate() {
            return discountRate;
        }
    }

    // Member의 id를 그대로 식별자로 사용한다.
    @Id
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoyaltyGrade loyaltyGrade;

    @Column(nullable = false)
    private boolean loyaltyDiscountApplied;

    private LocalDateTime loyaltyDiscountAppliedAt;

    public static Customer create(Long memberId) {
        return Customer.builder()
                .memberId(memberId)
                .loyaltyGrade(LoyaltyGrade.NORMAL)
                .loyaltyDiscountApplied(false)
                .build();
    }

    // 직원이 할인을 적용한 고객만 등급 할인율을 받는다.
    public int currentDiscountRate() {
        return loyaltyDiscountApplied ? loyaltyGrade.getDiscountRate() : 0;
    }
}
