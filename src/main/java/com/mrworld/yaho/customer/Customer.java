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

    @Column(nullable = false)
    private boolean loyaltyDiscountApplied;

    private LocalDateTime loyaltyDiscountAppliedAt;

    public static Customer create(Member member) {
        return Customer.builder()
                .member(member)
                .loyaltyGrade(LoyaltyGrade.NORMAL)
                .loyaltyDiscountApplied(false)
                .build();
    }

    // 완료 여행 횟수로 등급을 다시 정한다. 할인 적용 여부는 직원이 등급별 일괄 적용으로 따로 켠다.
    public void updateLoyaltyGrade(int completedTourCount) {
        this.loyaltyGrade = LoyaltyGrade.of(completedTourCount);
    }

    // 직원이 할인을 적용한 고객만 등급 할인율을 받는다.
    public int currentDiscountRate() {
        return loyaltyDiscountApplied ? loyaltyGrade.getDiscountRate() : 0;
    }
}
