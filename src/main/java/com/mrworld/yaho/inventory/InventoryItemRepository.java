package com.mrworld.yaho.inventory;

import com.mrworld.yaho.common.dto.IdTotal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    // 물품명 검색어가 들어간 재고만 조회한다.
    Page<InventoryItem> findByItemNameContaining(String keyword, Pageable pageable);

    boolean existsByItemName(String itemName);

    // 물품별 사용 예정 수량: 아직 확정 전(모집 중)인 투어의 결제 완료 인원 × 1인당 지급 수량의 합.
    // 확정된 투어는 이미 재고에서 차감됐으므로 세지 않는다. 사용 예정이 없는 물품은 결과에 없다.
    @Query("""
            select g.inventoryItem.itemId as id, sum(g.quantityPerGuest * b.guestCount) as total
            from TourGift g, Booking b
            where b.tour = g.tour
              and g.tour.tourStatus = com.mrworld.yaho.common.enums.TourStatus.RECRUITING
              and b.bookingStatus = com.mrworld.yaho.common.enums.BookingStatus.PAID
              and g.inventoryItem.itemId in :itemIds
            group by g.inventoryItem.itemId
            """)
    List<IdTotal> sumRequiredQuantityByItemIds(@Param("itemIds") List<Long> itemIds);

    // 수정 시 자기 자신을 뺀 다른 재고와 이름이 겹치는지 확인한다.
    boolean existsByItemNameAndItemIdNot(String itemName, Long itemId);
}
