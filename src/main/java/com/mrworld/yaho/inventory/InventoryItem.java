package com.mrworld.yaho.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// 창고에 보관 중인 기념품 재고
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    @Column(nullable = false, unique = true)
    private String itemName;

    @Column(nullable = false)
    private int quantity;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static InventoryItem create(String itemName, int quantity) {
        return InventoryItem.builder()
                .itemName(itemName)
                .quantity(quantity)
                .build();
    }

    // 투어 확정 시 기념품을 지급한 만큼 재고를 줄인다.
    // 재고가 부족해도 여행은 진행되므로 마이너스까지 줄이며, 마이너스는 직원이 채워야 할 부족 수량이다.
    public void deduct(int amount) {
        this.quantity -= amount;
    }

    public void update(String itemName, Integer quantity) {
        if (itemName != null) this.itemName = itemName;
        if (quantity != null) this.quantity = quantity;
    }
}
