package com.mrworld.yaho.tour.dto;

import com.mrworld.yaho.common.dto.PagingRequest;
import com.mrworld.yaho.common.enums.TourStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

// 직원용 상품 현황 목록 조건. 모두 선택값이며 여러 조건은 AND로 적용한다.
@Getter
@Setter
public class StaffTourListRequest extends PagingRequest {
    private TourStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDateTo;
}
