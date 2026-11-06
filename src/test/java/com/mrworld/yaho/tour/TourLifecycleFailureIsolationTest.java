package com.mrworld.yaho.tour;

import com.mrworld.yaho.booking.Booking;
import com.mrworld.yaho.booking.BookingRepository;
import com.mrworld.yaho.common.enums.BookingStatus;
import com.mrworld.yaho.common.enums.HotelGrade;
import com.mrworld.yaho.common.enums.MealType;
import com.mrworld.yaho.common.enums.PaymentStatus;
import com.mrworld.yaho.common.enums.Role;
import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourLevel;
import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.common.enums.Transport;
import com.mrworld.yaho.customer.Customer;
import com.mrworld.yaho.customer.CustomerRepository;
import com.mrworld.yaho.inventory.InventoryItem;
import com.mrworld.yaho.inventory.InventoryItemRepository;
import com.mrworld.yaho.member.Member;
import com.mrworld.yaho.member.MemberRepository;
import com.mrworld.yaho.notification.SmsSender;
import com.mrworld.yaho.payment.Payment;
import com.mrworld.yaho.payment.PaymentRepository;
import com.mrworld.yaho.payment.PaymentService;
import com.mrworld.yaho.support.DatabaseCleaner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// 투어 마감 일괄 처리에서 한 투어가 실패해도
// 1) 실패한 투어의 변경은 모두 롤백되고 2) 다른 투어는 정상 처리되며 3) 다음 실행에서 다시 처리되는지 검증한다.
// 테스트 메서드에 @Transactional을 붙이지 않아야 TourLifecycleService의 투어별 트랜잭션이 실제로 커밋·롤백된다.
@SpringBootTest
class TourLifecycleFailureIsolationTest {

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private TourLifecycleService tourLifecycleService;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MemberRepository memberRepository;

    // 실제 문자가 나가지 않도록 문자 발송기를 통째로 가짜로 바꾼다. (환경변수에 SMS 키가 있어도 안전)
    @MockitoBean
    private SmsSender smsSender;

    // 실제 환불 로직은 그대로 두고, 특정 조건에서만 예외를 던지도록 감싼다.
    @MockitoSpyBean
    private PaymentService paymentService;

    private InventoryItem scarf;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        scarf = inventoryItemRepository.save(InventoryItem.create("스카프", 10));
    }

    @AfterEach
    void tearDown() {
        databaseCleaner.clean();
    }

    @Test
    void 한_투어의_환불이_실패하면_그_투어만_롤백되고_다른_투어는_처리되며_다음_실행에서_다시_처리된다() {
        // 투어 A: 결제 2명(1명 + 1명) → 인원 미달로 취소·환불 대상
        Tour failingTour = saveTour(6);
        Booking failingFirst = savePaidBooking(failingTour, saveCustomer("01011110001"), 1);
        Booking failingSecond = savePaidBooking(failingTour, saveCustomer("01011110002"), 1);
        // 투어 B: 결제 3명 → 확정 대상
        Tour healthyTour = saveTour(6);
        Booking healthyBooking = savePaidBooking(healthyTour, saveCustomer("01022220001"), 3);

        // 투어 A의 환불 중 두 번째 호출에서만 실제 환불을 수행한 뒤 예외를 던진다.
        // - 처리 순서와 관계없이 "환불 1건은 이미 성공한 뒤 실패"하는 상황을 만든다.
        // - 실제 환불 메서드의 결제 조회 쿼리가 실행되면서 앞선 환불·예약 취소가 같은 트랜잭션 안에서 DB로 flush된다.
        //   그래서 아래 검증은 "DB에 반영되지 않았던 변경"이 아니라 "DB까지 내려간 변경이 롤백됐는지"를 확인한다.
        Set<Long> failingBookingIds = Set.of(failingFirst.getBookingId(), failingSecond.getBookingId());
        AtomicBoolean failureEnabled = new AtomicBoolean(true);
        AtomicInteger failingTourRefundCalls = new AtomicInteger();
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            Booking booking = invocation.getArgument(0);
            if (failureEnabled.get()
                    && failingBookingIds.contains(booking.getBookingId())
                    && failingTourRefundCalls.incrementAndGet() == 2) {
                throw new IllegalStateException("테스트용 환불 실패");
            }
            return result;
        }).when(paymentService).refund(any(Booking.class));

        // 1차 실행: 투어 A는 두 번째 환불에서 실패한다.
        tourLifecycleService.closeRecruitingTours();

        // 투어 A: 두 건의 환불과 먼저 처리된 예약 취소까지 함께 롤백되어 처리 전 상태 그대로 남는다.
        assertThat(failingTourRefundCalls.get()).isEqualTo(2);
        assertThat(tourStatusOf(failingTour)).isEqualTo(TourStatus.RECRUITING);
        assertThat(bookingStatusOf(failingFirst)).isEqualTo(BookingStatus.PAID);
        assertThat(bookingStatusOf(failingSecond)).isEqualTo(BookingStatus.PAID);
        assertThat(hasPayment(failingFirst, PaymentStatus.SUCCESS)).isTrue();
        assertThat(hasPayment(failingSecond, PaymentStatus.SUCCESS)).isTrue();

        // 투어 B: 투어 A의 실패와 관계없이 확정되고 재고 차감·확정 문자까지 처리된다.
        assertThat(tourStatusOf(healthyTour)).isEqualTo(TourStatus.CONFIRMED);
        assertThat(bookingStatusOf(healthyBooking)).isEqualTo(BookingStatus.PAID);
        // 스카프 10개 - 1인당 2개 × 3명 = 4개
        assertThat(stockOfScarf()).isEqualTo(4);
        verify(smsSender, times(1)).send(eq("01022220001"), anyString());

        // 2차 실행: 실패 원인이 사라진 다음 날 스케줄러가 다시 돈 상황
        failureEnabled.set(false);
        tourLifecycleService.closeRecruitingTours();

        // 투어 A: 이번에는 취소·환불이 모두 반영된다.
        assertThat(tourStatusOf(failingTour)).isEqualTo(TourStatus.CANCELLED);
        assertThat(bookingStatusOf(failingFirst)).isEqualTo(BookingStatus.CANCELLED);
        assertThat(bookingStatusOf(failingSecond)).isEqualTo(BookingStatus.CANCELLED);
        assertThat(hasPayment(failingFirst, PaymentStatus.REFUNDED)).isTrue();
        assertThat(hasPayment(failingSecond, PaymentStatus.REFUNDED)).isTrue();

        // 투어 B: 이미 확정되어 다시 처리되지 않으므로 재고 차감과 문자가 중복되지 않는다.
        assertThat(tourStatusOf(healthyTour)).isEqualTo(TourStatus.CONFIRMED);
        assertThat(stockOfScarf()).isEqualTo(4);
        verify(smsSender, times(1)).send(eq("01022220001"), anyString());

        // 취소된 투어 A의 고객에게는 문자가 나가지 않는다.
        verify(smsSender, never()).send(eq("01011110001"), anyString());
        verify(smsSender, never()).send(eq("01011110002"), anyString());
    }

    // 시작일이 오늘부터 daysLater일 뒤이고 1인당 스카프 2개를 주는 투어
    private Tour saveTour(int daysLater) {
        LocalDate startDate = LocalDate.now().plusDays(daysLater);
        Tour tour = Tour.create(Theme.GOLF, "골프 챌린지 투어", null,
                startDate, startDate.plusDays(3), 500000, Transport.PREMIUM_VAN_10);
        tour.addGift(scarf, 2);
        return tourRepository.save(tour);
    }

    private Booking savePaidBooking(Tour tour, Customer customer, int guestCount) {
        Booking booking = Booking.create(tour, customer, TourLevel.CLASSIC, guestCount,
                HotelGrade.THREE_STAR, MealType.LUNCH_BOX, Transport.PREMIUM_VAN_10, 1_000_000, 0);
        booking.pay(0);
        booking = bookingRepository.save(booking);
        paymentRepository.save(Payment.success(booking, 1_000_000, LocalDateTime.now()));
        return booking;
    }

    private Customer saveCustomer(String phoneNum) {
        Member member = memberRepository.save(Member.create(
                "isolation-" + UUID.randomUUID(), "pw", "고객", phoneNum, "서울", Role.CUSTOMER));
        return customerRepository.save(Customer.create(member));
    }

    private TourStatus tourStatusOf(Tour tour) {
        return tourRepository.findById(tour.getTourId()).orElseThrow().getTourStatus();
    }

    private BookingStatus bookingStatusOf(Booking booking) {
        return bookingRepository.findById(booking.getBookingId()).orElseThrow().getBookingStatus();
    }

    private boolean hasPayment(Booking booking, PaymentStatus status) {
        return paymentRepository.findByBookingBookingIdAndPaymentStatus(booking.getBookingId(), status).isPresent();
    }

    private int stockOfScarf() {
        return inventoryItemRepository.findById(scarf.getItemId()).orElseThrow().getQuantity();
    }
}
