package com.mrworld.yaho.common.enums;

// 교통수단. 옵션 추가금은 1인 기준 금액이다.
public enum Transport {
    PREMIUM_CAR_2(120_000),
    PREMIUM_VAN_10(50_000);

    private final int price;

    Transport(int price) {
        this.price = price;
    }

    public int getPrice() {
        return price;
    }
}
