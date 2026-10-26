package com.mrworld.yaho.inventory.dto;

import com.mrworld.yaho.inventory.InventoryItem;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 재고 목록·등록·수정 응답이 함께 쓰는 재고 물품 정보
@Getter
@Builder
public class InventoryResponse {
    private Long itemId;
    private String itemName;
    private int quantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static InventoryResponse from(InventoryItem item) {
        return InventoryResponse.builder()
                .itemId(item.getItemId())
                .itemName(item.getItemName())
                .quantity(item.getQuantity())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
