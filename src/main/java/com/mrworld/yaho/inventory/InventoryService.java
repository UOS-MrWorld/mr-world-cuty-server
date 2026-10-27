package com.mrworld.yaho.inventory;

import com.mrworld.yaho.common.dto.IdTotal;
import com.mrworld.yaho.common.dto.PageResponse;
import com.mrworld.yaho.common.exception.BadRequestException;
import com.mrworld.yaho.common.exception.ConflictException;
import com.mrworld.yaho.common.exception.NotFoundException;
import com.mrworld.yaho.inventory.dto.InventoryListRequest;
import com.mrworld.yaho.inventory.dto.InventoryRegisterRequest;
import com.mrworld.yaho.inventory.dto.InventoryResponse;
import com.mrworld.yaho.inventory.dto.InventoryUpdateRequest;
import com.mrworld.yaho.tour.Tour;
import com.mrworld.yaho.tour.TourGift;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

// 재고 등록/조회/수정과 투어 확정 시점의 재고 차감(deductStock).
// 재고가 부족해도 예약·확정을 막지 않으며, 부족 여부는 직원이 재고 목록의 현재 수량과 사용 예정 수량으로 확인한다.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {
    private static final Sort ITEM_SORT = Sort.by("itemId");

    private final InventoryItemRepository inventoryItemRepository;

    public PageResponse<InventoryResponse> getItems(InventoryListRequest request) {
        String keyword = request.getKeyword() == null ? "" : request.getKeyword().trim();
        Page<InventoryItem> page = inventoryItemRepository.findByItemNameContaining(keyword, request.toPageable(ITEM_SORT));

        Map<Long, Long> requiredQuantities = findRequiredQuantities(
                page.getContent().stream().map(InventoryItem::getItemId).toList());

        return PageResponse.of(page,
                item -> InventoryResponse.from(item, requiredQuantities.getOrDefault(item.getItemId(), 0L).intValue()));
    }

    @Transactional
    public InventoryResponse registerItem(InventoryRegisterRequest request) {
        String itemName = request.getItemName().trim();
        if (inventoryItemRepository.existsByItemName(itemName)) {
            throw new ConflictException("이미 등록된 물품명입니다.");
        }

        InventoryItem item = inventoryItemRepository.save(InventoryItem.create(itemName, request.getQuantity()));
        // 새로 등록한 물품은 아직 어떤 투어에도 쓰이지 않는다.
        return InventoryResponse.from(item, 0);
    }

    // 물품명과 수량 중 보낸 값만 바꾼다.
    @Transactional
    public InventoryResponse updateItem(Long itemId, InventoryUpdateRequest request) {
        if (request.getItemName() == null && request.getQuantity() == null) {
            throw new BadRequestException("수정할 값이 없습니다.");
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
        return InventoryResponse.from(item, findRequiredQuantity(itemId));
    }

    // 투어 확정 시 기념품마다 1인당 지급 수량 × 확정 인원만큼 재고를 차감한다.
    // 재고가 부족하면 마이너스가 되며, 직원이 재고 목록에서 보고 채울 수 있도록 경고 로그를 남긴다.
    @Transactional
    public void deductStock(Tour tour, int confirmedGuestCount) {
        for (TourGift tourGift : tour.getTourGifts()) {
            InventoryItem item = tourGift.getInventoryItem();
            item.deduct(tourGift.getQuantityPerGuest() * confirmedGuestCount);
            if (item.getQuantity() < 0) {
                log.warn("기념품 재고 부족 itemId={}, itemName={}, 부족 수량={}, tourId={}",
                        item.getItemId(), item.getItemName(), -item.getQuantity(), tour.getTourId());
            }
        }
    }

    // 물품별 사용 예정 수량. 사용 예정이 없는 물품은 맵에 없다.
    private Map<Long, Long> findRequiredQuantities(List<Long> itemIds) {
        return itemIds.isEmpty() ? Map.of() : IdTotal.toMap(inventoryItemRepository.sumRequiredQuantityByItemIds(itemIds));
    }

    private int findRequiredQuantity(Long itemId) {
        return findRequiredQuantities(List.of(itemId)).getOrDefault(itemId, 0L).intValue();
    }
}
