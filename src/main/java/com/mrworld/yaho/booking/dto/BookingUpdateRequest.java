package com.mrworld.yaho.booking.dto;

import com.mrworld.yaho.common.enums.HotelGrade;
import com.mrworld.yaho.common.enums.MealType;
import com.mrworld.yaho.common.enums.TourLevel;
import com.mrworld.yaho.common.enums.Transport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// 바꿀 옵션만 보낸다. drinkOptions를 보내면 기존 음료 구성 전체를 그 목록으로 교체한다(빈 배열이면 모두 삭제).
@Getter
@Setter
public class BookingUpdateRequest {
    private TourLevel tourLevel;

    @Min(value = 1, message = "예약 인원은 1명 이상이어야 합니다.")
    private Integer guestCount;

    private HotelGrade hotelGrade;

    private MealType mealType;

    private Transport transport;

    @Valid
    private List<QuoteRequest.DrinkOptionItem> drinkOptions;

    public boolean hasNoUpdate() {
        return tourLevel == null && guestCount == null && hotelGrade == null
                && mealType == null && transport == null && drinkOptions == null;
    }
}
