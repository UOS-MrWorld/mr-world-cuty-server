package com.mrworld.yaho.tour.dto;

import com.mrworld.yaho.tour.Tour;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

// GUI 검색 조건은 모두 선택값이며 여러 조건은 AND로 적용한다.
@Getter
@Setter
public class TourSearchRequest extends TourListRequest {
    private String keyword;

    private Tour.Theme theme;

    @Min(value = 0, message = "최대 가격은 0 이상이어야 합니다.")
    private Integer maxPrice;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate departureDate;
}
