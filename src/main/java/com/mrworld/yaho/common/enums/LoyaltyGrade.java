package com.mrworld.yaho.common.enums;

// 단골 등급. 등급 기준 횟수와 할인율은 직원이 수정할 수 없는 고정값이며, 팀 확정 전까지 임시 값이다.
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
