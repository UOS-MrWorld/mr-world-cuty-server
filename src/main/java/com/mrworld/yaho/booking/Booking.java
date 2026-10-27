package com.mrworld.yaho.booking;

import com.mrworld.yaho.common.enums.BookingStatus;
import com.mrworld.yaho.common.enums.HotelGrade;
import com.mrworld.yaho.common.enums.MealType;
import com.mrworld.yaho.common.enums.TourLevel;
import com.mrworld.yaho.common.enums.Transport;
import com.mrworld.yaho.customer.Customer;
import com.mrworld.yaho.tour.DrinkOption;
import com.mrworld.yaho.tour.Tour;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 고객의 예약. 선택한 옵션과 예약 시점의 금액·할인율을 저장한다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TourLevel tourLevel;

    @Column(nullable = false)
    private int guestCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HotelGrade hotelGrade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MealType mealType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Transport transport;

    // 할인 전 총 금액 (예약 인원 전체 기준)
    @Column(nullable = false)
    private int totalPrice;

    // 예약 생성 시점의 단골 할인율(%) 스냅샷
    @Column(nullable = false)
    private int discountRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus bookingStatus;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 선택한 음료는 Booking과 생명주기를 함께한다. (합성 관계)
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookingDrinkOption> bookingDrinkOptions = new ArrayList<>();

    public static Booking create(
            Tour tour,
            Customer customer,
            TourLevel tourLevel,
            int guestCount,
            HotelGrade hotelGrade,
            MealType mealType,
            Transport transport,
            int totalPrice,
            int discountRate
    ) {
        return Booking.builder()
                .tour(tour)
                .customer(customer)
                .tourLevel(tourLevel)
                .guestCount(guestCount)
                .hotelGrade(hotelGrade)
                .mealType(mealType)
                .transport(transport)
                .totalPrice(totalPrice)
                .discountRate(discountRate)
                .bookingStatus(BookingStatus.PENDING)
                .build();
    }

    public void addDrinkOption(DrinkOption drinkOption, int quantity) {
        bookingDrinkOptions.add(BookingDrinkOption.create(this, drinkOption, quantity));
    }

    // 결제 금액 = totalPrice × (100 - discountRate) / 100
    public int expectedPaymentAmount() {
        return totalPrice * (100 - discountRate) / 100;
    }
}
