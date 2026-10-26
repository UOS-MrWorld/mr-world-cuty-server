package com.mrworld.yaho.common.enums;

// 호텔 등급. 옵션 추가금은 1인 기준 금액이며, 팀에서 확정되기 전까지 임시 값이다.
public enum HotelGrade {
    THREE_STAR(0),
    FOUR_STAR(100_000),
    FIVE_STAR(250_000);

    private final int price;

    HotelGrade(int price) {
        this.price = price;
    }

    public int getPrice() {
        return price;
    }
}
