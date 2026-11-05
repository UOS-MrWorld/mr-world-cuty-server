package com.mrworld.yaho.customer;

import com.mrworld.yaho.common.enums.LoyaltyGrade;
import com.mrworld.yaho.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Customer {

    // Member의 id를 그대로 식별자로 사용한다. (member.memberId를 참조하는 외래키)
    @Id
    private Long memberId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoyaltyGrade loyaltyGrade;

    // 기존 운영 DB의 NOT NULL 컬럼을 안전하게 유지하기 위한 호환 필드다.
    // 할인 판단에는 사용하지 않으며, 스키마 마이그레이션 뒤 제거한다.
    @Column(name = "loyalty_discount_applied", nullable = false)
    private boolean legacyDiscountApplied;

    @Column(name = "loyalty_discount_applied_at")
    private LocalDateTime legacyDiscountAppliedAt;

    public static Customer create(Member member) {
        return Customer.builder()
                .member(member)
                .loyaltyGrade(LoyaltyGrade.NORMAL)
                .legacyDiscountApplied(false)
                .build();
    }

    // 단골 등급은 직원의 명시적인 일괄 저장으로만 바뀐다.
    public void updateLoyaltyGrade(LoyaltyGrade loyaltyGrade) {
        this.loyaltyGrade = loyaltyGrade;
    }

    // 할인율은 별도 활성화 상태 없이 현재 단골 등급에서 바로 결정한다.
    public int currentDiscountRate() {
        return loyaltyGrade.getDiscountRate();
    }
}
