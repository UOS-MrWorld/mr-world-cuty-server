package com.mrworld.yaho.booking;

import com.mrworld.yaho.tour.DrinkOption;
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

// 예약에서 선택한 추가 음료와 수량. Booking에 포함되는 구성 요소라 Booking과 함께 저장·삭제된다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookingDrinkOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingDrinkOptionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private DrinkOption drinkOption;

    // 예약 전체 기준 음료 개수 (인원수와 무관)
    @Column(nullable = false)
    private int quantity;

    static BookingDrinkOption create(Booking booking, DrinkOption drinkOption, int quantity) {
        BookingDrinkOption bookingDrinkOption = new BookingDrinkOption();
        bookingDrinkOption.booking = booking;
        bookingDrinkOption.drinkOption = drinkOption;
        bookingDrinkOption.quantity = quantity;
        return bookingDrinkOption;
    }
}
