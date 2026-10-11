package com.mrworld.yaho.tour.dto;

import lombok.Builder;
import lombok.Getter;

// 상세 조회에서 선택 가능한 추가 음료 옵션
@Getter
@Builder
public class DrinkOptionResponse {
    private long optionId;
    private String drinkName;
    private int drinkPrice;
}
