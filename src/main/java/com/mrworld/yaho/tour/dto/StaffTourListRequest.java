package com.mrworld.yaho.tour.dto;

import com.mrworld.yaho.common.enums.TourStatus;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

// 직원용 상품 현황 목록 조건. 모두 선택값이며 여러 조건은 AND로 적용한다.
@Getter
@Setter
public class StaffTourListRequest {
    private TourStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDateTo;

    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    private int page = 0;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    private int size = 20;

    // 출발이 가까운 투어부터 보여준다.
    public Pageable toPageable() {
        return PageRequest.of(page, size, Sort.by("startDate", "tourId"));
    }
}
