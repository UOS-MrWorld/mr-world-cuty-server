package com.mrworld.yaho.tour.dto;

import com.mrworld.yaho.tour.Tour;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class TourSummaryResponse {
    private long tourId;
    private Tour.Theme theme;
    private String tourName;
    private LocalDate startDate;
    private LocalDate endDate;
    private int basePrice;
    private Tour.Status tourStatus;
}
