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

    public enum Transport {
        PREMIUM_CAR_2,
        PREMIUM_VAN_10
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
