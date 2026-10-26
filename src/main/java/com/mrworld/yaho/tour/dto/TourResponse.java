package com.mrworld.yaho.tour.dto;

import com.mrworld.yaho.common.enums.TourStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 직원의 상품 등록 결과
@Getter
@Builder
public class TourResponse {
    private Long tourId;
    private TourStatus tourStatus;
    private LocalDateTime createdAt;
}
