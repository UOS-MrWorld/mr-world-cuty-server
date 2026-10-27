package com.mrworld.yaho.booking.dto;

import com.mrworld.yaho.booking.Booking;
import com.mrworld.yaho.booking.BookingDrinkOption;
import com.mrworld.yaho.common.enums.BookingStatus;
import com.mrworld.yaho.common.enums.HotelGrade;
import com.mrworld.yaho.common.enums.MealType;
import com.mrworld.yaho.common.enums.PaymentStatus;
import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourLevel;
import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.common.enums.Transport;
import com.mrworld.yaho.tour.Tour;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

// 예약 상세. paymentStatus는 결제 기능이 생기기 전까지 null이다.
@Getter
@Builder
public class BookingDetailResponse {
    private Long bookingId;
    private TourInfo tour;
    private TourLevel tourLevel;
    private int guestCount;
    private HotelGrade hotelGrade;
    private MealType mealType;
    private Transport transport;
    private List<DrinkOptionInfo> drinkOptions;
    private BookingStatus bookingStatus;
    private int totalPrice;
    private int discountRate;
    private PaymentStatus paymentStatus;

    public static BookingDetailResponse from(Booking booking) {
        return BookingDetailResponse.builder()
                .bookingId(booking.getBookingId())
                .tour(TourInfo.from(booking.getTour()))
                .tourLevel(booking.getTourLevel())
                .guestCount(booking.getGuestCount())
                .hotelGrade(booking.getHotelGrade())
                .mealType(booking.getMealType())
                .transport(booking.getTransport())
                .drinkOptions(booking.getBookingDrinkOptions().stream().map(DrinkOptionInfo::from).toList())
                .bookingStatus(booking.getBookingStatus())
                .totalPrice(booking.getTotalPrice())
                .discountRate(booking.getDiscountRate())
                .build();
    }

    @Getter
    @Builder
    public static class TourInfo {
        private Long tourId;
        private String tourName;
        private Theme theme;
        private LocalDate startDate;
        private LocalDate endDate;
        private TourStatus tourStatus;

        static TourInfo from(Tour tour) {
            return TourInfo.builder()
                    .tourId(tour.getTourId())
                    .tourName(tour.getTourName())
                    .theme(tour.getTheme())
                    .startDate(tour.getStartDate())
                    .endDate(tour.getEndDate())
                    .tourStatus(tour.getTourStatus())
                    .build();
        }
    }

    // price는 음료 1개 가격이다.
    @Getter
    @Builder
    public static class DrinkOptionInfo {
        private Long optionId;
        private String drinkName;
        private int quantity;
        private int price;

        static DrinkOptionInfo from(BookingDrinkOption bookingDrinkOption) {
            return DrinkOptionInfo.builder()
                    .optionId(bookingDrinkOption.getDrinkOption().getOptionId())
                    .drinkName(bookingDrinkOption.getDrinkOption().getDrinkName())
                    .quantity(bookingDrinkOption.getQuantity())
                    .price(bookingDrinkOption.getDrinkOption().getDrinkPrice())
                    .build();
        }
    }
}
