package com.mrworld.yaho.tour.dto;

import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourLevel;
import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.common.enums.Transport;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class TourDetailResponse {
    private long tourId;
    private Theme theme;
    private String tourName;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private int basePrice;
    private Transport defaultTransport;
    private TourStatus tourStatus;
    private List<TourLevel> availableTourLevels;
}
