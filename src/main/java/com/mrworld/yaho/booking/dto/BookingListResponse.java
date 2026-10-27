package com.mrworld.yaho.booking.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BookingListResponse {
    private List<BookingSummaryResponse> content;
    private long totalElements;
}
