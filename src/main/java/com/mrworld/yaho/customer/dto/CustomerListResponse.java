package com.mrworld.yaho.customer.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CustomerListResponse {
    private List<CustomerSummaryResponse> content;
    private long totalElements;
}
