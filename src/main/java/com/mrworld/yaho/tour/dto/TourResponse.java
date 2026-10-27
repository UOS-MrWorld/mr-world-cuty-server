package com.mrworld.yaho.tour.dto;

import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.tour.Tour;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 직원의 상품 등록·수정 결과
@Getter
@Builder
public class TourResponse {
    private Long tourId;
    private String tourName;
    private int basePrice;
    private TourStatus tourStatus;
    private LocalDateTime createdAt;

    public static TourResponse from(Tour tour) {
        return TourResponse.builder()
                .tourId(tour.getTourId())
                .tourName(tour.getTourName())
                .basePrice(tour.getPrice())
                .tourStatus(tour.getTourStatus())
                .createdAt(tour.getCreatedAt())
                .build();
    }
}
