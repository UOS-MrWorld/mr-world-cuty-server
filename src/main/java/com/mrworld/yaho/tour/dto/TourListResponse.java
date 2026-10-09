package com.mrworld.yaho.tour.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TourListResponse {
    private List<TourSummaryResponse> content;
    private long totalElements;
}
