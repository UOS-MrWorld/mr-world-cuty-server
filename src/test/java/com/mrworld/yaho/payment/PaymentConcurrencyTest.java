package com.mrworld.yaho.payment;

import com.mrworld.yaho.booking.Booking;
import com.mrworld.yaho.booking.BookingRepository;
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
import java.util.Collections;
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

// 같은 예약에 결제 요청이 동시에 들어올 때 성공 결제가 1건만 저장되는지 확인한다.
// PaymentService.pay는 "성공 결제 존재 여부 조회 → 결제 저장 → 예약 PAID 변경" 순서로 동작하는데,
// 조회와 저장 사이를 막는 락·버전·DB 제약이 없어 동시 요청이 모두 조회를 통과할 수 있는지 재현한다.
// 테스트 메서드에 @Transactional을 붙이지 않아야 요청마다 실제로 별도 트랜잭션이 커밋된다.
@SpringBootTest
class PaymentConcurrencyTest {

    private static final int REQUEST_COUNT = 10;
    private static final String APPROVED_CARD_NUMBER = "1";

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private PaymentService paymentService;

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

    // 결제 경로는 문자를 보내지 않지만, 실제 발송 구현체가 만들어지지 않도록 막아 둔다.
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
    void 같은_예약에_결제_요청이_동시에_들어와도_성공_결제는_1건만_저장된다() throws Exception {
        Customer customer = saveCustomer();
        Booking booking = savePendingBooking(customer);

        AtomicInteger approvedResponses = new AtomicInteger();
        Map<String, AtomicInteger> rejectedByException = new ConcurrentHashMap<>();
        List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<>());

        // 모든 스레드가 준비된 뒤 한꺼번에 출발시켜 조회와 저장 사이가 겹치도록 한다.
        CountDownLatch ready = new CountDownLatch(REQUEST_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(REQUEST_COUNT);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < REQUEST_COUNT; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    paymentService.pay(customer.getMemberId(), paymentRequest(booking.getBookingId()));
                    approvedResponses.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    unexpected.add(e);
                } catch (RuntimeException e) {
                    rejectedByException
                            .computeIfAbsent(e.getClass().getSimpleName(), key -> new AtomicInteger())
                            .incrementAndGet();
                }
            }));
        }
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();

        long successPayments = paymentRepository.findAll().stream()
                .filter(payment -> payment.getPaymentStatus() == PaymentStatus.SUCCESS)
                .count();

        // 재현 결과를 기록으로 남기기 위해 출력한다.
        System.out.printf("[동시 결제] 요청 %d건 → 승인 응답 %d건, 거절 %s, 저장된 성공 결제 %d건%n",
                REQUEST_COUNT, approvedResponses.get(), rejectedByException, successPayments);

        assertThat(unexpected).isEmpty();
        assertThat(bookingRepository.findById(booking.getBookingId()).orElseThrow().getBookingStatus())
                .isEqualTo(BookingStatus.PAID);
        assertThat(approvedResponses.get()).isEqualTo(1);
        assertThat(successPayments).isEqualTo(1);
    }

    private PaymentRequest paymentRequest(Long bookingId) {
        PaymentRequest request = new PaymentRequest();
        request.setBookingId(bookingId);
        request.setCardNumber(APPROVED_CARD_NUMBER);
        return request;
    }

    // 결제 마감 전(시작 30일 뒤) 모집 중 투어의 결제 전 예약
    private Booking savePendingBooking(Customer customer) {
        LocalDate startDate = LocalDate.now().plusDays(30);
        Tour tour = tourRepository.save(Tour.create(Theme.GOLF, "골프 챌린지 투어", null,
                startDate, startDate.plusDays(3), 500000, Transport.PREMIUM_VAN_10));
        return bookingRepository.save(Booking.create(tour, customer, TourLevel.CLASSIC, 2,
                HotelGrade.THREE_STAR, MealType.LUNCH_BOX, Transport.PREMIUM_VAN_10, 1_000_000, 0));
    }

    private Customer saveCustomer() {
        Member member = memberRepository.save(Member.create(
                "payment-race-" + UUID.randomUUID(), "pw", "고객", "01012345678", "서울", Role.CUSTOMER));
        return customerRepository.save(Customer.create(member));
    }
}
