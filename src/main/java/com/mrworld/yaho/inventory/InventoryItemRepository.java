package com.mrworld.yaho.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    // 물품명 검색어가 들어간 재고만 조회한다.
    Page<InventoryItem> findByItemNameContaining(String keyword, Pageable pageable);
}
