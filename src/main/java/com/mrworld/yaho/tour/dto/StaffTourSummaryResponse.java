package com.mrworld.yaho.tour.dto;

import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

// 직원용 상품 현황 목록의 한 항목. paidGuestCount는 결제 완료 예약의 인원 합이다.
@Getter
@Builder
public class StaffTourSummaryResponse {
    private Long tourId;
    private String tourName;
    private Theme theme;
    private LocalDate startDate;
    private TourStatus tourStatus;
    private int paidGuestCount;
}
