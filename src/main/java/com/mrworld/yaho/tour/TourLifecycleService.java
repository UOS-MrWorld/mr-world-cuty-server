package com.mrworld.yaho.tour;

import com.mrworld.yaho.booking.Booking;
import com.mrworld.yaho.booking.BookingRepository;
import com.mrworld.yaho.common.enums.BookingStatus;
import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.inventory.InventoryService;
import com.mrworld.yaho.notification.NotificationService;
import com.mrworld.yaho.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

// 시간에 따른 투어 상태 전환. 예약 마감 시 확정·취소(RECRUITING → CONFIRMED/CANCELLED)와
// 여행 종료 시 완료(CONFIRMED → COMPLETED)를 처리한다. TourScheduler가 매일 호출한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class TourLifecycleService {

    // 마감 시점 결제 완료 인원이 이 수 이상이면 투어를 확정한다.
    // 허니문은 예약이 2명 단위라 결제 인원이 항상 짝수이므로, 실제로는 커플 2쌍(4명)부터 확정된다.
    private static final int MIN_CONFIRM_GUEST_COUNT = 3;

    private final TourRepository tourRepository;
    private final BookingRepository bookingRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final PlatformTransactionManager transactionManager;

    // 예약 마감(시작 7일 전)이 지난 모집 중 투어를 확정하거나 취소한다. TourScheduler가 매일 호출한다.
    // 투어마다 별도 트랜잭션으로 처리해, 한 투어가 실패해도 나머지 투어는 처리되고 실패한 투어는 다음 실행에 다시 시도한다.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void closeRecruitingTours() {
        LocalDate closedBefore = LocalDate.now().plusDays(Tour.BOOKING_CLOSE_DAYS_BEFORE_START);
        List<Long> tourIds = tourRepository.findAllByTourStatusAndStartDateBefore(TourStatus.RECRUITING, closedBefore)
                .stream()
                .map(Tour::getTourId)
                .toList();

        processEachTour(tourIds, this::closeTour, "투어 마감 처리 실패");
    }

    // 여행 기간이 끝난 확정 투어를 완료 처리하고, 참가 고객의 단골 등급을 완료 여행 횟수로 다시 정한다.
    // TourScheduler가 매일 호출하며, 마감 처리와 같이 투어마다 별도 트랜잭션으로 처리한다.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void completeEndedTours() {
        List<Long> tourIds = tourRepository.findAllByTourStatusAndEndDateBefore(TourStatus.CONFIRMED, LocalDate.now())
                .stream()
                .map(Tour::getTourId)
                .toList();

        processEachTour(tourIds, this::completeTour, "투어 완료 처리 실패");
    }

    // 투어마다 새 트랜잭션에서 처리한다. 실패한 투어는 로그만 남기고 다음 투어로 넘어간다.
    private void processEachTour(List<Long> tourIds, Consumer<Long> action, String failMessage) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        for (Long tourId : tourIds) {
            try {
                transaction.executeWithoutResult(status -> action.accept(tourId));
            } catch (Exception e) {
                log.error("{} tourId={}", failMessage, tourId, e);
            }
        }
    }

    // 데모 API(DemoService)에서도 호출하므로 public으로 연다.
    @Transactional
    public void completeTour(Long tourId) {
        Tour tour = tourRepository.findById(tourId).orElseThrow();
        tour.complete();

        // 완료 처리가 반영된 뒤 횟수를 세도록, 등급 계산 쿼리 전에 상태 변경을 DB에 내보낸다.
        tourRepository.flush();
        bookingRepository.findAllByTourIdAndBookingStatus(tourId, BookingStatus.PAID).stream()
                .map(Booking::getCustomer)
                .distinct()
                .forEach(customer -> customer.updateLoyaltyGrade(
                        bookingRepository.countCompletedToursByCustomerId(customer.getMemberId())));
    }

    // 데모 API(DemoService)에서도 호출하므로 public으로 연다.
    @Transactional
    public void closeTour(Long tourId) {
        Tour tour = tourRepository.findById(tourId).orElseThrow();
        List<Booking> paidBookings = bookingRepository.findAllByTourIdAndBookingStatus(tourId, BookingStatus.PAID);
        int paidGuestCount = paidBookings.stream().mapToInt(Booking::getGuestCount).sum();

        if (paidGuestCount >= MIN_CONFIRM_GUEST_COUNT) {
            confirmTour(tour, paidBookings);
        } else {
            cancelTour(tour, paidBookings);
        }

        // 마감 후에는 결제할 수 없으므로 결제 전(PENDING) 예약은 모두 취소한다.
        bookingRepository.findAllByTourIdAndBookingStatus(tourId, BookingStatus.PENDING)
                .forEach(Booking::cancel);
    }

    // 확정 인원만큼 기념품 재고를 차감하고, 결제 완료 고객에게 확정 문자를 보낸다.
    private void confirmTour(Tour tour, List<Booking> paidBookings) {
        int confirmedGuestCount = paidBookings.stream().mapToInt(Booking::getGuestCount).sum();
        tour.confirm();
        inventoryService.deductStock(tour, confirmedGuestCount);
        notificationService.sendTourConfirmedSms(tour, paidBookings);
    }

    // 인원 미달이면 결제 완료 예약을 모두 환불하고 취소한다.
    private void cancelTour(Tour tour, List<Booking> paidBookings) {
        tour.cancel();
        for (Booking booking : paidBookings) {
            paymentService.refund(booking);
            booking.cancel();
        }
    }
}
