package com.mrworld.yaho.tour;

import com.mrworld.yaho.inventory.InventoryItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 투어별로 지급할 기념품과 1인당 지급 수량. Tour에 포함되는 구성 요소라 Tour와 함께 저장·삭제된다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TourGift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tourGiftId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem inventoryItem;

    @Column(nullable = false)
    private int quantityPerGuest;

    static TourGift create(Tour tour, InventoryItem inventoryItem, int quantityPerGuest) {
        TourGift tourGift = new TourGift();
        tourGift.tour = tour;
        tourGift.inventoryItem = inventoryItem;
        tourGift.quantityPerGuest = quantityPerGuest;
        return tourGift;
    }
}
