package com.mrworld.yaho.common.enums;

import java.util.List;

// 투어 테마. 테마별로 선택 가능한 투어 등급과 예약 인원 규칙을 가진다.
public enum Theme {
    HONEYMOON("허니문", List.of(TourLevel.GRAND, TourLevel.PREMIUM), 2),
    HEALING("힐링", List.of(TourLevel.GRAND, TourLevel.PREMIUM), null),
    GOLF("골프", List.of(TourLevel.values()), null),
    TREKKING("트레킹", List.of(TourLevel.values()), null);

    private final String label;
    private final List<TourLevel> availableLevels;
    // 예약 인원이 정해진 테마의 인원. 제한이 없으면 null이다.
    private final Integer fixedGuestCount;

    Theme(String label, List<TourLevel> availableLevels, Integer fixedGuestCount) {
        this.label = label;
        this.availableLevels = availableLevels;
        this.fixedGuestCount = fixedGuestCount;
    }

    public boolean allowsGuestCount(int guestCount) {
        return fixedGuestCount == null || fixedGuestCount == guestCount;
    }

    public String getLabel() {
        return label;
    }

    public List<TourLevel> getAvailableLevels() {
        return availableLevels;
    }

    public Integer getFixedGuestCount() {
        return fixedGuestCount;
    }
}
