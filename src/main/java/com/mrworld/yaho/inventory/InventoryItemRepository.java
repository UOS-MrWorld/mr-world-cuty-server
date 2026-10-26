package com.mrworld.yaho.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    // 물품명 검색어가 들어간 재고만 조회한다.
    Page<InventoryItem> findByItemNameContaining(String keyword, Pageable pageable);

    boolean existsByItemName(String itemName);

    // 수정 시 자기 자신을 뺀 다른 재고와 이름이 겹치는지 확인한다.
    boolean existsByItemNameAndItemIdNot(String itemName, Long itemId);
}
