package com.mrworld.yaho.tour.dto;

import com.mrworld.yaho.tour.Tour;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class TourDetailResponse {
    private long tourId;
    private Tour.Theme theme;
    private String tourName;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private int basePrice;
    private Tour.Transport defaultTransport;
    private Tour.Status tourStatus;
    private List<Tour.TourLevel> availableTourLevels;
}
