package com.mrworld.yaho.demo;

import com.mrworld.yaho.auth.AuthService;
import com.mrworld.yaho.auth.dto.LoginRequest;
import com.mrworld.yaho.booking.Booking;
import com.mrworld.yaho.booking.BookingRepository;
import com.mrworld.yaho.common.enums.BookingStatus;
import com.mrworld.yaho.common.enums.LoyaltyGrade;
import com.mrworld.yaho.common.enums.PaymentStatus;
import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.customer.Customer;
import com.mrworld.yaho.customer.CustomerRepository;
import com.mrworld.yaho.member.Member;
import com.mrworld.yaho.member.MemberRepository;
import com.mrworld.yaho.member.StaffRepository;
import com.mrworld.yaho.payment.PaymentRepository;
import com.mrworld.yaho.tour.Tour;
import com.mrworld.yaho.tour.TourLifecycleService;
import com.mrworld.yaho.tour.TourRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

// 다른 테스트와 섞이지 않도록 별도의 H2 DB에 데모 데이터를 넣고 확인한다.
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:demo-data;MODE=MySQL")
@ActiveProfiles("demo")
@Transactional
class DemoDataInitializerTest {

    @Autowired
    private DemoDataInitializer demoDataInitializer;

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TourLifecycleService tourLifecycleService;

    @Test
    void 고객과_직원은_데모_비밀번호로_로그인한다() {
        assertThat(authService.login(loginRequest("customer", "customer")).getAccessToken()).isNotBlank();
        assertThat(authService.login(loginRequest("customer3", "customer")).getAccessToken()).isNotBlank();
        assertThat(authService.login(loginRequest("staff", "staffstaff")).getAccessToken()).isNotBlank();
        assertThat(staffRepository.existsById(findMember("staff").getMemberId())).isTrue();
    }

    @Test
    void 단골_등급과_할인은_시나리오대로_들어간다() {
        Customer customer = findCustomer("customer");
        assertThat(customer.getLoyaltyGrade()).isEqualTo(LoyaltyGrade.VIP);
        assertThat(customer.isLoyaltyDiscountApplied()).isFalse();

        Customer customer2 = findCustomer("customer2");
        assertThat(customer2.getLoyaltyGrade()).isEqualTo(LoyaltyGrade.VIP);
        assertThat(customer2.currentDiscountRate()).isEqualTo(10);

        Customer customer3 = findCustomer("customer3");
        assertThat(customer3.getLoyaltyGrade()).isEqualTo(LoyaltyGrade.NORMAL);
        assertThat(bookingRepository.countCompletedToursByCustomerId(customer3.getMemberId())).isEqualTo(4);
    }

    @Test
    void 예약은_투어_규칙을_지키고_결제_금액과_맞는다() {
        for (Booking booking : bookingRepository.findAll()) {
            Tour tour = booking.getTour();
            assertThat(tour.availableLevels()).contains(booking.getTourLevel());

            if (booking.getBookingStatus() == BookingStatus.PAID) {
                assertThat(paymentRepository.findByBookingBookingIdAndPaymentStatus(
                        booking.getBookingId(), PaymentStatus.SUCCESS))
                        .hasValueSatisfying(payment ->
                                assertThat(payment.getAmount()).isEqualTo(booking.expectedPaymentAmount()));
            }
        }
    }

    @Test
    void 모집_중인_투어는_아직_마감되지_않았다() {
        assertThat(tourRepository.findAll()).filteredOn(tour -> tour.getTourStatus() == TourStatus.RECRUITING)
                .isNotEmpty()
                .allSatisfy(tour -> assertThat(tour.isBookingClosed(LocalDate.now())).isFalse());
    }

    @Test
    void 시연_고객은_세_명이다() {
        assertThat(customerRepository.count()).isEqualTo(3);
        assertThat(memberRepository.findByLoginId("customer4")).isEmpty();
    }

    @Test
    void 고객_취소_시연용_예약은_결제_완료이고_취소할_수_있다() {
        Tour gijang = findTour("부산 기장 골프 3박 4일");
        Customer customer = findCustomer("customer");

        assertThat(bookingRepository.findAllByTourIdAndBookingStatus(gijang.getTourId(), BookingStatus.PAID))
                .singleElement()
                .satisfies(booking -> assertThat(booking.getCustomer().getMemberId()).isEqualTo(customer.getMemberId()));
        assertThat(gijang.isBookingClosed(LocalDate.now())).isFalse();
    }

    // demo.base-date가 없으면 실행일이 발표일이다.
    @Test
    void 완료된_투어는_발표일_전에_끝났다() {
        LocalDate demoDay = LocalDate.now();
        assertThat(tourRepository.findAll()).filteredOn(tour -> tour.getTourStatus() == TourStatus.COMPLETED)
                .isNotEmpty()
                .allSatisfy(tour -> assertThat(tour.getEndDate()).isBefore(demoDay));
    }

    @Test
    void 발표_장면의_투어는_발표일이_처리일이다() {
        LocalDate demoDay = LocalDate.now();

        // 평창: 발표일에 끝나는 확정 투어. 종료일 당일에는 스케줄러가 완료하지 않는다.
        Tour pyeongchang = findTour("평창 골프 4박 5일");
        assertThat(pyeongchang.getTourStatus()).isEqualTo(TourStatus.CONFIRMED);
        assertThat(pyeongchang.getEndDate()).isEqualTo(demoDay);
        assertThat(tourRepository.findAllByTourStatusAndEndDateBefore(TourStatus.CONFIRMED, demoDay)).isEmpty();

        // 속초·거제: 발표일이 예약 마감일이라 발표일에도 결제할 수 있고, 자정 마감 대상은 아니다.
        for (String tourName : List.of("속초 힐링 리조트 4박 5일", "거제 허니문 5박 6일")) {
            Tour tour = findTour(tourName);
            assertThat(tour.getTourStatus()).isEqualTo(TourStatus.RECRUITING);
            assertThat(tour.getStartDate().minusDays(Tour.BOOKING_CLOSE_DAYS_BEFORE_START)).isEqualTo(demoDay);
            assertThat(tour.isBookingClosed(demoDay)).isFalse();
        }
        assertThat(tourRepository.findAllByTourStatusAndStartDateBefore(
                TourStatus.RECRUITING, demoDay.plusDays(Tour.BOOKING_CLOSE_DAYS_BEFORE_START))).isEmpty();
    }

    // 상품명의 "N박 M일"이 실제 출발일·종료일과 맞아야 한다.
    @Test
    void 상품명의_일정_표기는_실제_일정과_같다() {
        Pattern schedule = Pattern.compile("(\\d+)박 (\\d+)일");
        assertThat(tourRepository.findAll()).allSatisfy(tour -> {
            Matcher matcher = schedule.matcher(tour.getTourName());
            assertThat(matcher.find()).as(tour.getTourName()).isTrue();
            long nights = ChronoUnit.DAYS.between(tour.getStartDate(), tour.getEndDate());
            assertThat(Long.parseLong(matcher.group(1))).as(tour.getTourName()).isEqualTo(nights);
            assertThat(Long.parseLong(matcher.group(2))).as(tour.getTourName()).isEqualTo(nights + 1);
        });
    }

    @Test
    void 결제_인원이_모자란_거제_투어를_마감하면_취소된다() {
        Tour geoje = findTour("거제 허니문 5박 6일");

        tourLifecycleService.closeTour(geoje.getTourId());

        assertThat(geoje.getTourStatus()).isEqualTo(TourStatus.CANCELLED);
    }

    @Test
    void 속초_투어는_결제_대기_예약까지_결제하면_확정_인원이_된다() {
        Tour sokcho = findTour("속초 힐링 리조트 4박 5일");

        int guestCount = bookingRepository.findAllByTourIdAndBookingStatus(sokcho.getTourId(), BookingStatus.PAID)
                .stream().mapToInt(Booking::getGuestCount).sum();
        int pendingGuestCount = bookingRepository.findAllByTourIdAndBookingStatus(sokcho.getTourId(), BookingStatus.PENDING)
                .stream().mapToInt(Booking::getGuestCount).sum();

        assertThat(guestCount).isEqualTo(2);
        assertThat(pendingGuestCount).isEqualTo(1);
    }

    @Test
    void 다시_실행해도_중복으로_만들지_않는다() {
        long memberCount = memberRepository.count();
        long tourCount = tourRepository.count();

        demoDataInitializer.run(null);

        assertThat(memberRepository.count()).isEqualTo(memberCount);
        assertThat(tourRepository.count()).isEqualTo(tourCount);
    }

    private LoginRequest loginRequest(String loginId, String password) {
        LoginRequest request = new LoginRequest();
        request.setLoginId(loginId);
        request.setPassword(password);
        return request;
    }

    private Member findMember(String loginId) {
        return memberRepository.findByLoginId(loginId).orElseThrow();
    }

    private Customer findCustomer(String loginId) {
        return customerRepository.findById(findMember(loginId).getMemberId()).orElseThrow();
    }

    private Tour findTour(String tourName) {
        return tourRepository.findAll().stream()
                .filter(tour -> tour.getTourName().equals(tourName))
                .findFirst()
                .orElseThrow();
    }
}
