package com.mrworld.yaho.common.enums;

// 식사 타입. 옵션 추가금은 1인 기준 금액이며, 팀에서 확정되기 전까지 임시 값이다.
public enum MealType {
    LUNCH_BOX(0),
    LOCAL_RESTAURANT(50_000),
    FINE_DINING(150_000);

    private final int price;

    MealType(int price) {
        this.price = price;
    }

    public int getPrice() {
        return price;
    }
}
