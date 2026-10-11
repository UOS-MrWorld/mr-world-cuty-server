package com.mrworld.yaho.tour.dto;

import com.mrworld.yaho.tour.Tour;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// GUI와 음성 입력이 같은 형식으로 옵션을 전달한다.
@Getter
@Setter
public class QuoteRequest {
    @NotNull(message = "투어 등급은 필수입니다.")
    private Tour.TourLevel tourLevel;

    @NotNull(message = "예약 인원은 필수입니다.")
    @Min(value = 1, message = "예약 인원은 1명 이상이어야 합니다.")
    private Integer guestCount;

    @NotNull(message = "호텔 등급은 필수입니다.")
    private Tour.HotelGrade hotelGrade;

    @NotNull(message = "식사 타입은 필수입니다.")
    private Tour.MealType mealType;

    @NotNull(message = "교통 타입은 필수입니다.")
    private Tour.Transport transport;

    @Valid
    private List<DrinkOptionItem> drinkOptions;

    @Getter
    @Setter
    public static class DrinkOptionItem {
        @NotNull(message = "음료 옵션 ID는 필수입니다.")
        private Long optionId;

        @NotNull(message = "음료 수량은 필수입니다.")
        @Min(value = 1, message = "음료 수량은 1 이상이어야 합니다.")
        private Integer quantity;
    }
}
