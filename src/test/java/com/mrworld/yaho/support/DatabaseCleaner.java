package com.mrworld.yaho.support;

import com.mrworld.yaho.booking.BookingRepository;
import com.mrworld.yaho.inventory.InventoryItemRepository;
import com.mrworld.yaho.payment.PaymentRepository;
import com.mrworld.yaho.tour.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 통합 테스트가 만든 결제·예약·투어·재고를 지운다.
// 결제 → 예약 → 투어(기념품 구성 포함) → 재고 순서로 지워야 외래키 때문에 실패하지 않는다.
// 회원·고객과 기본 음료 옵션은 테스트마다 새로 만들거나 그대로 쓰므로 지우지 않는다.
@Component
@RequiredArgsConstructor
public class DatabaseCleaner {
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final TourRepository tourRepository;
    private final InventoryItemRepository inventoryItemRepository;

    public void clean() {
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        tourRepository.deleteAll();
        inventoryItemRepository.deleteAll();
    }
}
