package com.mrworld.yaho.notification;

import com.mrworld.yaho.booking.Booking;
import com.mrworld.yaho.common.enums.HotelGrade;
import com.mrworld.yaho.common.enums.MealType;
import com.mrworld.yaho.common.enums.TourLevel;
import com.mrworld.yaho.common.enums.Transport;
import com.mrworld.yaho.tour.Tour;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

// 고객에게 보내는 알림 문자. 문자 발송이 실패해도 투어 처리에는 영향을 주지 않고 로그만 남긴다.
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    // 문자에 보여줄 옵션 이름
    private static final Map<TourLevel, String> TOUR_LEVEL_NAMES = Map.of(
            TourLevel.CLASSIC, "클래식",
            TourLevel.GRAND, "그랜드",
            TourLevel.PREMIUM, "프리미엄");
    private static final Map<HotelGrade, String> HOTEL_GRADE_NAMES = Map.of(
            HotelGrade.THREE_STAR, "3성급 호텔",
            HotelGrade.FOUR_STAR, "4성급 호텔",
            HotelGrade.FIVE_STAR, "5성급 호텔");
    private static final Map<MealType, String> MEAL_TYPE_NAMES = Map.of(
            MealType.LUNCH_BOX, "도시락",
            MealType.LOCAL_RESTAURANT, "현지식 레스토랑",
            MealType.FINE_DINING, "파인다이닝");
    private static final Map<Transport, String> TRANSPORT_NAMES = Map.of(
            Transport.PREMIUM_CAR_2, "프리미엄 승용차 2인승",
            Transport.PREMIUM_VAN_10, "프리미엄 밴 10인승");

    private final SmsSender smsSender;

    // 투어 확정 문자를 결제 완료 예약마다 한 통씩 보낸다. 예약번호·인원·옵션을 넣어 예약별로 구분되게 한다.
    // 확정 처리가 롤백되면 문자가 나가면 안 되므로, 트랜잭션 안에서 부르면 커밋된 뒤에 보낸다.
    public void sendTourConfirmedSms(Tour tour, List<Booking> paidBookings) {
        // 커밋 뒤에는 지연 로딩을 할 수 없으므로 보낼 내용을 미리 만들어 둔다.
        List<Sms> messages = paidBookings.stream()
                .map(booking -> new Sms(
                        booking.getCustomer().getMember().getPhoneNum().replaceAll("\\D", ""),
                        tourConfirmedText(tour, booking)))
                .toList();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendAll(messages);
                }
            });
            return;
        }
        sendAll(messages);
    }

    private String tourConfirmedText(Tour tour, Booking booking) {
        return "[CUTY] 투어 확정 안내\n"
                + tour.getTourName() + "\n"
                + "출발일: " + tour.getStartDate() + "\n"
                + "예약번호: " + booking.getBookingId() + " (" + booking.getGuestCount() + "명)\n"
                + "옵션: " + TOUR_LEVEL_NAMES.get(booking.getTourLevel())
                + " / " + HOTEL_GRADE_NAMES.get(booking.getHotelGrade())
                + " / " + MEAL_TYPE_NAMES.get(booking.getMealType())
                + " / " + TRANSPORT_NAMES.get(booking.getTransport());
    }

    private void sendAll(List<Sms> messages) {
        for (Sms sms : messages) {
            try {
                smsSender.send(sms.to(), sms.text());
            } catch (Exception e) {
                log.error("투어 확정 문자 발송 실패 to={}", sms.to(), e);
            }
        }
    }

    private record Sms(String to, String text) {
    }
}
