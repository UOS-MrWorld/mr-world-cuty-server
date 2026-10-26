package com.mrworld.yaho.tour.dto;

import lombok.Builder;
import lombok.Getter;

// 상세 조회에서 보여주는 투어 기념품 구성
@Getter
@Builder
public class TourGiftResponse {
    private Long itemId;
    private String itemName;
    private int quantityPerGuest;
}
