package com.mrworld.yaho.tour.dto;

import com.mrworld.yaho.tour.DrinkOption;
import lombok.Builder;
import lombok.Getter;

// 상세 조회에서 선택 가능한 추가 음료 옵션
@Getter
@Builder
public class DrinkOptionResponse {
    private Long optionId;
    private String drinkName;
    private int drinkPrice;

    public static DrinkOptionResponse from(DrinkOption drinkOption) {
        return DrinkOptionResponse.builder()
                .optionId(drinkOption.getOptionId())
                .drinkName(drinkOption.getDrinkName())
                .drinkPrice(drinkOption.getDrinkPrice())
                .build();
    }
}
