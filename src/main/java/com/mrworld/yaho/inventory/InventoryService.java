package com.mrworld.yaho.inventory;

import com.mrworld.yaho.common.exception.ConflictException;
import com.mrworld.yaho.inventory.dto.InventoryListRequest;
import com.mrworld.yaho.inventory.dto.InventoryListResponse;
import com.mrworld.yaho.inventory.dto.InventoryRegisterRequest;
import com.mrworld.yaho.inventory.dto.InventoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 재고 등록/조회/수정. 예약·확정 시점의 재고 확인(validateStock)과 차감(deductStock)은 TourGift와 함께 추가한다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {
    private final InventoryItemRepository inventoryItemRepository;

    public InventoryListResponse getItems(InventoryListRequest request) {
        String keyword = request.getKeyword() == null ? "" : request.getKeyword().trim();
        Page<InventoryItem> page = inventoryItemRepository.findByItemNameContaining(keyword, request.toPageable());

        return InventoryListResponse.builder()
                .content(page.getContent().stream().map(InventoryResponse::from).toList())
                .totalElements(page.getTotalElements())
                .build();
    }

    @Transactional
    public InventoryResponse registerItem(InventoryRegisterRequest request) {
        String itemName = request.getItemName().trim();
        if (inventoryItemRepository.existsByItemName(itemName)) {
            throw new ConflictException("이미 등록된 물품명입니다.");
        }

        InventoryItem item = inventoryItemRepository.save(InventoryItem.create(itemName, request.getQuantity()));
        return InventoryResponse.from(item);
    }
}
