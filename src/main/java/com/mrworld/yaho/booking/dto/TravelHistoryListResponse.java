package com.mrworld.yaho.booking.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TravelHistoryListResponse {
    private List<TravelHistoryResponse> content;
    private long totalElements;
}
