package com.mrworld.yaho.inventory.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class InventoryListResponse {
    private List<InventoryResponse> content;
    private long totalElements;
}
