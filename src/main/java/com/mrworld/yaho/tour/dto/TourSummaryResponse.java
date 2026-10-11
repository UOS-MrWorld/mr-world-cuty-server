package com.mrworld.yaho.tour.dto;

import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class TourSummaryResponse {
    private long tourId;
    private Theme theme;
    private String tourName;
    private LocalDate startDate;
    private LocalDate endDate;
    private int basePrice;
    private TourStatus tourStatus;
}
