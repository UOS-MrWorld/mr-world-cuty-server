package com.mrworld.yaho.payment;

import com.mrworld.yaho.booking.Booking;
import com.mrworld.yaho.booking.BookingRepository;
import com.mrworld.yaho.booking.BookingService;
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

// 같은 예약에 결제·취소 요청이 동시에 들어올 때 예약과 결제 상태가 어긋나지 않는지 확인한다.
// 결제와 취소는 예약 행을 잠그고(BookingRepository.findByIdForUpdate) 차례로 처리한다.
// 테스트 메서드에 @Transactional을 붙이지 않아야 요청마다 실제로 별도 트랜잭션이 커밋된다.
@SpringBootTest
class PaymentConcurrencyTest {

    private static final String APPROVED_CARD_NUMBER = "1";

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MemberRepository memberRepository;

    // 결제·취소 경로는 문자를 보내지 않지만, 실제 발송 구현체가 만들어지지 않도록 막아 둔다.
    @MockitoBean
    private SmsSender smsSender;

    private Customer customer;
    private Tour tour;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        customer = saveCustomer();
        tour = saveRecruitingTour();
    }

    @AfterEach
    void tearDown() {
        databaseCleaner.clean();
    }

    // 잠그지 않으면 동시 요청이 모두 "성공 결제 없음" 조회를 통과해 성공 결제가 요청 수만큼 저장된다.
    @Test
    void 같은_예약에_결제_요청이_동시에_들어와도_성공_결제는_1건만_저장된다() throws Exception {
        Long bookingId = savePendingBooking().getBookingId();
        int requestCount = 10;
        AtomicInteger approvedResponses = new AtomicInteger();
        Map<String, AtomicInteger> rejected = new ConcurrentHashMap<>();

        List<Runnable> requests = new ArrayList<>();
        for (int i = 0; i < requestCount; i++) {
            requests.add(() -> {
                paymentService.pay(customer.getMemberId(), paymentRequest(bookingId));
                approvedResponses.incrementAndGet();
            });
        }
        runConcurrently(requests, rejected);

        long successPayments = countPayments(bookingId, PaymentStatus.SUCCESS);
        System.out.printf("[동시 결제] 요청 %d건 → 승인 응답 %d건, 거절 %s, 저장된 성공 결제 %d건%n",
                requestCount, approvedResponses.get(), rejected, successPayments);

        assertThat(bookingRepository.findById(bookingId).orElseThrow().getBookingStatus())
                .isEqualTo(BookingStatus.PAID);
        assertThat(approvedResponses.get()).isEqualTo(1);
        assertThat(successPayments).isEqualTo(1);
    }

    // 허용되는 결과는 두 가지뿐이다. 어느 쪽이든 최종 상태는 CANCELLED다.
    //  - 결제가 먼저: CANCELLED + 결제 REFUNDED
    //  - 취소가 먼저: CANCELLED + 결제 없음 (결제 요청은 거절)
    // 잠그지 않으면 취소가 PENDING을 보고 환불 없이 취소하거나(성공 결제가 남음),
    // 결제가 상태를 PAID로 덮어써 성공 응답을 받은 취소가 사라진다.
    // 한 번의 동시 요청으로는 타이밍이 겹치지 않을 수 있어, 예약 여러 건에 대해 같은 상황을 한꺼번에 만든다.
    @Test
    void 결제와_취소가_동시에_들어와도_취소된_예약에_성공_결제가_남지_않는다() throws Exception {
        int bookingCount = 20;
        List<Long> bookingIds = new ArrayList<>();
        List<Runnable> requests = new ArrayList<>();
        for (int i = 0; i < bookingCount; i++) {
            Long bookingId = savePendingBooking().getBookingId();
            bookingIds.add(bookingId);
            requests.add(() -> paymentService.pay(customer.getMemberId(), paymentRequest(bookingId)));
            requests.add(() -> bookingService.cancelBooking(customer.getMemberId(), bookingId));
        }
        Map<String, AtomicInteger> rejected = new ConcurrentHashMap<>();
        runConcurrently(requests, rejected);

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
        System.out.printf("[결제·취소 동시] 예약 %d건 → 최종 상태 %s, 거절 %s%n",
                bookingCount, outcomes, rejected);

        assertThat(cancelledWithSuccessPayment).as("취소됐는데 성공 결제가 남은 예약 수").isZero();
        assertThat(notCancelled).as("취소 요청이 사라져 CANCELLED가 아닌 예약 수").isZero();
    }

    // 모든 요청을 준비시킨 뒤 한꺼번에 출발시켜, 조회와 저장 사이가 겹치도록 한다.
    // 요청이 던진 예외는 실패가 아니라 거절로 보고 예외 종류별로 센다.
    private void runConcurrently(List<Runnable> requests, Map<String, AtomicInteger> rejected) throws Exception {
        CountDownLatch ready = new CountDownLatch(requests.size());
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(requests.size());
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Runnable request : requests) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        request.run();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    } catch (RuntimeException e) {
                        rejected.computeIfAbsent(e.getClass().getSimpleName(), key -> new AtomicInteger())
                                .incrementAndGet();
                    }
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private long countPayments(Long bookingId, PaymentStatus status) {
        return paymentRepository.findAll().stream()
                .filter(payment -> payment.getBooking().getBookingId().equals(bookingId))
                .filter(payment -> payment.getPaymentStatus() == status)
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

    private Booking savePendingBooking() {
        return bookingRepository.save(Booking.create(tour, customer, TourLevel.CLASSIC, 2,
                HotelGrade.THREE_STAR, MealType.LUNCH_BOX, Transport.PREMIUM_VAN_10, 1_000_000, 0));
    }

    private Customer saveCustomer() {
        Member member = memberRepository.save(Member.create(
                "payment-race-" + UUID.randomUUID(), "pw", "고객", "01012345678", "서울", Role.CUSTOMER));
        return customerRepository.save(Customer.create(member));
    }
}
