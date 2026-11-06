package com.mrworld.yaho.booking;

import com.mrworld.yaho.common.enums.BookingStatus;
import com.mrworld.yaho.common.enums.HotelGrade;
import com.mrworld.yaho.common.enums.MealType;
import com.mrworld.yaho.common.enums.PaymentStatus;
import com.mrworld.yaho.common.enums.Role;
import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourLevel;
import com.mrworld.yaho.common.enums.Transport;
import com.mrworld.yaho.customer.Customer;
import com.mrworld.yaho.customer.CustomerRepository;
import com.mrworld.yaho.member.Member;
import com.mrworld.yaho.member.MemberRepository;
import com.mrworld.yaho.notification.SmsSender;
import com.mrworld.yaho.payment.Payment;
import com.mrworld.yaho.payment.PaymentRepository;
import com.mrworld.yaho.payment.PaymentService;
import com.mrworld.yaho.payment.dto.PaymentRequest;
import com.mrworld.yaho.support.DatabaseCleaner;
import com.mrworld.yaho.tour.Tour;
import com.mrworld.yaho.tour.TourRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

// 같은 예약에 결제와 고객 취소가 동시에 들어와도 예약 상태와 결제 상태가 어긋나지 않는지 확인한다.
// 허용되는 결과는 두 가지뿐이다.
//  - 결제가 먼저: 예약 CANCELLED + 결제 REFUNDED (성공 결제 0건)
//  - 취소가 먼저: 예약 CANCELLED + 결제 없음 (결제 요청은 거절)
// 즉 어떤 순서든 최종 상태는 CANCELLED여야 하고, "취소된 예약에 성공 결제가 남는" 상태도 생기면 안 된다.
// (PAID로 남는다면 취소 요청은 성공 응답을 받았는데 결제가 그 결과를 덮어쓴 것이다.)
// 한 번의 동시 요청으로는 타이밍이 겹치지 않을 수 있어, 예약 여러 건에 대해 같은 상황을 한꺼번에 만든다.
@SpringBootTest
class BookingPaymentCancelConcurrencyTest {

    private static final int BOOKING_COUNT = 20;
    private static final String APPROVED_CARD_NUMBER = "1";

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MemberRepository memberRepository;

    // 결제·취소 경로는 문자를 보내지 않지만, 실제 발송 구현체가 만들어지지 않도록 막아 둔다.
    @MockitoBean
    private SmsSender smsSender;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
    }

    @AfterEach
    void tearDown() {
        databaseCleaner.clean();
    }

    @Test
    void 결제와_취소가_동시에_들어와도_취소된_예약에_성공_결제가_남지_않는다() throws Exception {
        Customer customer = saveCustomer();
        Tour tour = saveRecruitingTour();
        List<Long> bookingIds = new ArrayList<>();
        for (int i = 0; i < BOOKING_COUNT; i++) {
            bookingIds.add(savePendingBooking(tour, customer).getBookingId());
        }

        Map<String, AtomicInteger> rejectedByException = new ConcurrentHashMap<>();
        int threadCount = BOOKING_COUNT * 2;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();
        for (Long bookingId : bookingIds) {
            futures.add(executor.submit(() -> run(ready, start, rejectedByException,
                    () -> paymentService.pay(customer.getMemberId(), paymentRequest(bookingId)))));
            futures.add(executor.submit(() -> run(ready, start, rejectedByException,
                    () -> bookingService.cancelBooking(customer.getMemberId(), bookingId))));
        }
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // 예약마다 최종 상태를 확인한다.
        int cancelledWithSuccessPayment = 0;
        int notCancelled = 0;
        Map<String, Integer> outcomes = new ConcurrentHashMap<>();
        for (Long bookingId : bookingIds) {
            BookingStatus bookingStatus = bookingRepository.findById(bookingId).orElseThrow().getBookingStatus();
            long successPayments = countPayments(bookingId, PaymentStatus.SUCCESS);
            long refundedPayments = countPayments(bookingId, PaymentStatus.REFUNDED);
            outcomes.merge(bookingStatus + "/성공" + successPayments + "/환불" + refundedPayments, 1, Integer::sum);

            if (bookingStatus == BookingStatus.CANCELLED && successPayments > 0) {
                cancelledWithSuccessPayment++;
            }
            if (bookingStatus != BookingStatus.CANCELLED) {
                notCancelled++;
            }
        }

        // 재현 결과를 기록으로 남기기 위해 출력한다.
        System.out.printf("[결제·취소 동시] 예약 %d건 → 최종 상태 %s, 거절 %s%n",
                BOOKING_COUNT, outcomes, rejectedByException);

        assertThat(cancelledWithSuccessPayment).as("취소됐는데 성공 결제가 남은 예약 수").isZero();
        assertThat(notCancelled).as("취소 요청이 사라져 CANCELLED가 아닌 예약 수").isZero();
    }

    private void run(CountDownLatch ready, CountDownLatch start,
                     Map<String, AtomicInteger> rejectedByException, Runnable action) {
        ready.countDown();
        try {
            start.await();
            action.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (RuntimeException e) {
            rejectedByException
                    .computeIfAbsent(e.getClass().getSimpleName(), key -> new AtomicInteger())
                    .incrementAndGet();
        }
    }

    private long countPayments(Long bookingId, PaymentStatus status) {
        return paymentRepository.findAll().stream()
                .filter(payment -> payment.getBooking().getBookingId().equals(bookingId))
                .map(Payment::getPaymentStatus)
                .filter(status::equals)
                .count();
    }

    private PaymentRequest paymentRequest(Long bookingId) {
        PaymentRequest request = new PaymentRequest();
        request.setBookingId(bookingId);
        request.setCardNumber(APPROVED_CARD_NUMBER);
        return request;
    }

    // 결제·취소 마감 전(시작 30일 뒤) 모집 중 투어
    private Tour saveRecruitingTour() {
        LocalDate startDate = LocalDate.now().plusDays(30);
        return tourRepository.save(Tour.create(Theme.GOLF, "골프 챌린지 투어", null,
                startDate, startDate.plusDays(3), 500000, Transport.PREMIUM_VAN_10));
    }

    private Booking savePendingBooking(Tour tour, Customer customer) {
        return bookingRepository.save(Booking.create(tour, customer, TourLevel.CLASSIC, 2,
                HotelGrade.THREE_STAR, MealType.LUNCH_BOX, Transport.PREMIUM_VAN_10, 1_000_000, 0));
    }

    private Customer saveCustomer() {
        Member member = memberRepository.save(Member.create(
                "pay-cancel-race-" + UUID.randomUUID(), "pw", "고객", "01012345678", "서울", Role.CUSTOMER));
        return customerRepository.save(Customer.create(member));
    }
}
