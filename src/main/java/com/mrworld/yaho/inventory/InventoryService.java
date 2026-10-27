package com.mrworld.yaho.inventory;

import com.mrworld.yaho.common.exception.ConflictException;
import com.mrworld.yaho.common.exception.NotFoundException;
import com.mrworld.yaho.inventory.dto.InventoryListRequest;
import com.mrworld.yaho.inventory.dto.InventoryListResponse;
import com.mrworld.yaho.inventory.dto.InventoryRegisterRequest;
import com.mrworld.yaho.inventory.dto.InventoryResponse;
import com.mrworld.yaho.inventory.dto.InventoryUpdateRequest;
import com.mrworld.yaho.tour.Tour;
import com.mrworld.yaho.tour.TourGift;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 재고 등록/조회/수정과 예약 시점의 재고 확인(validateStock). 확정 시점의 차감(deductStock)은 투어 마감 처리와 함께 추가한다.
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

    // 물품명과 수량 중 보낸 값만 바꾼다.
    @Transactional
    public InventoryResponse updateItem(Long itemId, InventoryUpdateRequest request) {
        if (request.getItemName() == null && request.getQuantity() == null) {
            throw new IllegalArgumentException("수정할 값이 없습니다.");
        }

        InventoryItem item = inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 재고 물품입니다."));

        String itemName = request.getItemName() == null ? null : request.getItemName().trim();
        if (itemName != null && inventoryItemRepository.existsByItemNameAndItemIdNot(itemName, itemId)) {
            throw new ConflictException("이미 등록된 물품명입니다.");
        }

        item.update(itemName, request.getQuantity());
        // 응답의 updatedAt이 이번 수정 시각이 되도록 바로 반영한다.
        inventoryItemRepository.flush();
        return InventoryResponse.from(item);
    }

    // 투어의 기념품마다 1인당 지급 수량 × guestCount 이상 재고가 있는지 확인한다.
    // guestCount는 재고로 감당해야 하는 전체 인원(결제 완료 인원 + 이번 예약 인원)이다.
    public void validateStock(Tour tour, int guestCount) {
        for (TourGift tourGift : tour.getTourGifts()) {
            int required = tourGift.getQuantityPerGuest() * guestCount;
            if (tourGift.getInventoryItem().getQuantity() < required) {
                throw new ConflictException("기념품 재고가 부족하여 예약할 수 없습니다.");
            }
        }
    }
}
