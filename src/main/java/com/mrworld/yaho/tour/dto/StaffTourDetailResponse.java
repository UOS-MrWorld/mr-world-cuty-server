package com.mrworld.yaho.tour.dto;

import com.mrworld.yaho.booking.Booking;
import com.mrworld.yaho.common.enums.BookingStatus;
import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.common.enums.Transport;
import com.mrworld.yaho.tour.Tour;
import com.mrworld.yaho.tour.TourGift;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

// 직원용 상품 상세 현황. 투어 정보, 결제 완료 인원, 예약 현황(모든 상태), 기념품 구성과 현재 재고를 담는다.
@Getter
@Builder
public class StaffTourDetailResponse {
    private TourInfo tour;
    private int paidGuestCount;
    private List<BookingInfo> bookings;
    private List<GiftInfo> gifts;

    @Getter
    @Builder
    public static class TourInfo {
        private Long tourId;
        private Theme theme;
        private String tourName;
        private String description;
        private LocalDate startDate;
        private LocalDate endDate;
        private int basePrice;
        private Transport defaultTransport;
        private TourStatus tourStatus;

        public static TourInfo from(Tour tour) {
            return TourInfo.builder()
                    .tourId(tour.getTourId())
                    .theme(tour.getTheme())
                    .tourName(tour.getTourName())
                    .description(tour.getDescription())
                    .startDate(tour.getStartDate())
                    .endDate(tour.getEndDate())
                    .basePrice(tour.getPrice())
                    .defaultTransport(tour.getDefaultTransport())
                    .tourStatus(tour.getTourStatus())
                    .build();
        }
    }

    // name은 예약한 고객의 이름이다.
    @Getter
    @Builder
    public static class BookingInfo {
        private Long bookingId;
        private Long customerId;
        private String name;
        private int guestCount;
        private BookingStatus bookingStatus;

        public static BookingInfo from(Booking booking) {
            return BookingInfo.builder()
                    .bookingId(booking.getBookingId())
                    .customerId(booking.getCustomer().getMemberId())
                    .name(booking.getCustomer().getMember().getName())
                    .guestCount(booking.getGuestCount())
                    .bookingStatus(booking.getBookingStatus())
                    .build();
        }
    }

    // currentStock은 창고에 남아 있는 해당 기념품 수량이다.
    @Getter
    @Builder
    public static class GiftInfo {
        private Long itemId;
        private String itemName;
        private int quantityPerGuest;
        private int currentStock;

        public static GiftInfo from(TourGift tourGift) {
            return GiftInfo.builder()
                    .itemId(tourGift.getInventoryItem().getItemId())
                    .itemName(tourGift.getInventoryItem().getItemName())
                    .quantityPerGuest(tourGift.getQuantityPerGuest())
                    .currentStock(tourGift.getInventoryItem().getQuantity())
                    .build();
        }
    }

    public static StaffTourDetailResponse of(Tour tour, int paidGuestCount, List<Booking> bookings) {
        return StaffTourDetailResponse.builder()
                .tour(TourInfo.from(tour))
                .paidGuestCount(paidGuestCount)
                .bookings(bookings.stream().map(BookingInfo::from).toList())
                .gifts(tour.getTourGifts().stream().map(GiftInfo::from).toList())
                .build();
    }
}
