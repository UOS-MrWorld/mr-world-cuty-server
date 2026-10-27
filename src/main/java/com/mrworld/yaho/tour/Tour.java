package com.mrworld.yaho.tour;

import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourLevel;
import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.common.enums.Transport;
import com.mrworld.yaho.inventory.InventoryItem;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Tour {

    // 투어 시작 7일 전까지만 예약·결제할 수 있다.
    public static final int BOOKING_CLOSE_DAYS_BEFORE_START = 7;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tourId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Theme theme;

    @Column(nullable = false)
    private String tourName;

    private String description;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    // 1인 기준 테마 기본 가격
    @Column(nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Transport defaultTransport;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TourStatus tourStatus;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 기념품 구성은 Tour와 생명주기를 함께한다. (합성 관계)
    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TourGift> tourGifts = new ArrayList<>();

    public static Tour create(
            Theme theme,
            String tourName,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            int price,
            Transport defaultTransport
    ) {
        return Tour.builder()
                .theme(theme)
                .tourName(tourName)
                .description(description)
                .startDate(startDate)
                .endDate(endDate)
                .price(price)
                .defaultTransport(defaultTransport)
                .tourStatus(TourStatus.RECRUITING)
                .build();
    }

    public void addGift(InventoryItem inventoryItem, int quantityPerGuest) {
        tourGifts.add(TourGift.create(this, inventoryItem, quantityPerGuest));
    }

    // HONEYMOON/HEALING은 GRAND 이상만 선택 가능
    public List<TourLevel> availableLevels() {
        if (theme == Theme.HONEYMOON || theme == Theme.HEALING) {
            return List.of(TourLevel.GRAND, TourLevel.PREMIUM);
        }
        return List.of(TourLevel.values());
    }

    // 시작 7일 전 당일까지는 예약 가능하고, 그 다음 날부터 마감이다.
    public boolean isBookingClosed(LocalDate today) {
        return today.isAfter(startDate.minusDays(BOOKING_CLOSE_DAYS_BEFORE_START));
    }

    // 마감 시점에 결제 인원이 최소 인원 이상이면 확정한다.
    public void confirm() {
        this.tourStatus = TourStatus.CONFIRMED;
    }

    // 마감 시점에 결제 인원이 최소 인원 미달이면 취소한다.
    public void cancel() {
        this.tourStatus = TourStatus.CANCELLED;
    }

    // 확정된 투어의 여행 기간이 끝나면 완료 처리한다.
    public void complete() {
        this.tourStatus = TourStatus.COMPLETED;
    }

    // 직원의 상품 수정. null인 값은 기존 값을 유지한다.
    public void update(String tourName, String description, LocalDate startDate, LocalDate endDate,
                       Integer price, Transport defaultTransport) {
        if (tourName != null) this.tourName = tourName;
        if (description != null) this.description = description;
        if (startDate != null) this.startDate = startDate;
        if (endDate != null) this.endDate = endDate;
        if (price != null) this.price = price;
        if (defaultTransport != null) this.defaultTransport = defaultTransport;
    }

    public void clearGifts() {
        tourGifts.clear();
    }
}
