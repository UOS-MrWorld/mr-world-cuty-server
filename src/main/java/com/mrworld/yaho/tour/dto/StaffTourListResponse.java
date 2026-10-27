package com.mrworld.yaho.tour.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StaffTourListResponse {
    private List<StaffTourSummaryResponse> content;
    private long totalElements;
}
