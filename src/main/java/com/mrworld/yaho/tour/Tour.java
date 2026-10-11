package com.mrworld.yaho.tour;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Tour {

    public enum Theme {
        HONEYMOON,
        HEALING,
        GOLF,
        TREKKING
    }

    // 투어 등급 (CLASSIC=3성급+도시락, GRAND=4성급+현지식, PREMIUM=5성급+파인다이닝)
    public enum TourLevel {
        CLASSIC,
        GRAND,
        PREMIUM
    }

    // 옵션 추가금은 1인 기준 금액이며, 팀에서 확정되기 전까지 임시 값이다.
    public enum HotelGrade {
        THREE_STAR(0),
        FOUR_STAR(100_000),
        FIVE_STAR(250_000);

        private final int price;

        HotelGrade(int price) {
            this.price = price;
        }

        public int getPrice() {
            return price;
        }
    }

    public enum MealType {
        LUNCH_BOX(0),
        LOCAL_RESTAURANT(50_000),
        FINE_DINING(150_000);

        private final int price;

        MealType(int price) {
            this.price = price;
        }

        public int getPrice() {
            return price;
        }
    }

    public enum Transport {
        PREMIUM_CAR_2(120_000),
        PREMIUM_VAN_10(50_000);

        private final int price;

        Transport(int price) {
            this.price = price;
        }

        public int getPrice() {
            return price;
        }
    }

    public enum Status {
        RECRUITING,
        CONFIRMED,
        COMPLETED,
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(nullable = false)
    private int basePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Transport defaultTransport;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status tourStatus;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Tour create(
            Theme theme,
            String tourName,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            int basePrice,
            Transport defaultTransport
    ) {
        return Tour.builder()
                .theme(theme)
                .tourName(tourName)
                .description(description)
                .startDate(startDate)
                .endDate(endDate)
                .basePrice(basePrice)
                .defaultTransport(defaultTransport)
                .tourStatus(Status.RECRUITING)
                .build();
    }

    // HONEYMOON/HEALING은 GRAND 이상만 선택 가능
    public List<TourLevel> availableLevels() {
        if (theme == Theme.HONEYMOON || theme == Theme.HEALING) {
            return List.of(TourLevel.GRAND, TourLevel.PREMIUM);
        }
        return List.of(TourLevel.values());
    }
}
