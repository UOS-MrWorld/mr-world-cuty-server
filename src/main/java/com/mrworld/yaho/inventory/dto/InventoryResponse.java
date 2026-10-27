package com.mrworld.yaho.inventory.dto;

import com.mrworld.yaho.inventory.InventoryItem;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 재고 목록·등록·수정 응답이 함께 쓰는 재고 물품 정보.
// quantity는 현재 창고 수량(확정된 투어 때문에 마이너스일 수 있음),
// requiredQuantity는 아직 확정 전인 투어의 결제 인원에게 지급할 사용 예정 수량이다.
@Getter
@Builder
public class InventoryResponse {
    private Long itemId;
    private String itemName;
    private int quantity;
    private int requiredQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static InventoryResponse from(InventoryItem item, int requiredQuantity) {
        return InventoryResponse.builder()
                .itemId(item.getItemId())
                .itemName(item.getItemName())
                .quantity(item.getQuantity())
                .requiredQuantity(requiredQuantity)
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
