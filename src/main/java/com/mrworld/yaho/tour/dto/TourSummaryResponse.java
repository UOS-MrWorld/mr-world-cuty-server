package com.mrworld.yaho.tour.dto;

import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.tour.Tour;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class TourSummaryResponse {
    private Long tourId;
    private Theme theme;
    private String tourName;
    private LocalDate startDate;
    private LocalDate endDate;
    private int basePrice;
    private TourStatus tourStatus;

    public static TourSummaryResponse from(Tour tour) {
        return TourSummaryResponse.builder()
                .tourId(tour.getTourId())
                .theme(tour.getTheme())
                .tourName(tour.getTourName())
                .startDate(tour.getStartDate())
                .endDate(tour.getEndDate())
                .basePrice(tour.getPrice())
                .tourStatus(tour.getTourStatus())
                .build();
    }
}
