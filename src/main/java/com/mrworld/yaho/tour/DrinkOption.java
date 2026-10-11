package com.mrworld.yaho.tour;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

// 투어와 무관하게 모든 예약에서 고를 수 있는 추가 음료 옵션
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class DrinkOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String drinkName;

    @Column(nullable = false)
    private int drinkPrice;

    public static DrinkOption create(String drinkName, int drinkPrice) {
        return DrinkOption.builder()
                .drinkName(drinkName)
                .drinkPrice(drinkPrice)
                .build();
    }
}
