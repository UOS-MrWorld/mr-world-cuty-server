package com.mrworld.yaho.tour.dto;

import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourLevel;
import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.common.enums.Transport;
import com.mrworld.yaho.tour.Tour;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class TourDetailResponse {
    private Long tourId;
    private Theme theme;
    private String tourName;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private int basePrice;
    private Transport defaultTransport;
    private TourStatus tourStatus;
    private List<TourLevel> availableTourLevels;
    private List<DrinkOptionResponse> drinkOptions;
    private List<TourGiftResponse> gifts;

    // 상세 화면에서 고를 수 있는 음료 옵션 목록은 투어와 무관한 전체 목록이라 따로 받는다.
    public static TourDetailResponse from(Tour tour, List<DrinkOptionResponse> drinkOptions) {
        return TourDetailResponse.builder()
                .tourId(tour.getTourId())
                .theme(tour.getTheme())
                .tourName(tour.getTourName())
                .description(tour.getDescription())
                .startDate(tour.getStartDate())
                .endDate(tour.getEndDate())
                .basePrice(tour.getPrice())
                .defaultTransport(tour.getDefaultTransport())
                .tourStatus(tour.getTourStatus())
                .availableTourLevels(tour.availableLevels())
                .drinkOptions(drinkOptions)
                .gifts(tour.getTourGifts().stream().map(TourGiftResponse::from).toList())
                .build();
    }
}
