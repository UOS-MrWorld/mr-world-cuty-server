package com.mrworld.yaho.customer;

import com.mrworld.yaho.common.enums.LoyaltyGrade;
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
