package com.mrworld.yaho.common.enums;

// 단골 등급. 등급 기준 완료 여행 횟수와 할인율은 직원이 수정할 수 없는 고정값이다.
public enum LoyaltyGrade {
    NORMAL(0, 0),
    VIP(5, 10);

    private final int requiredTourCount;
    private final int discountRate;

    LoyaltyGrade(int requiredTourCount, int discountRate) {
        this.requiredTourCount = requiredTourCount;
        this.discountRate = discountRate;
    }

    // 완료 여행 횟수로 받을 수 있는 가장 높은 등급
    public static LoyaltyGrade of(int completedTourCount) {
        LoyaltyGrade grade = NORMAL;
        for (LoyaltyGrade candidate : values()) {
            if (completedTourCount >= candidate.requiredTourCount
                    && candidate.requiredTourCount >= grade.requiredTourCount) {
                grade = candidate;
            }
        }
        return grade;
    }

    public int getRequiredTourCount() {
        return requiredTourCount;
    }

    public int getDiscountRate() {
        return discountRate;
    }
}
