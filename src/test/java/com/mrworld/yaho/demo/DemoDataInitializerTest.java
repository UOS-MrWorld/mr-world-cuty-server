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
    void 고객과_직원은_아이디와_같은_비밀번호로_로그인한다() {
        assertThat(authService.login(loginRequest("customer", "customer")).getAccessToken()).isNotBlank();
        assertThat(authService.login(loginRequest("customer5", "customer")).getAccessToken()).isNotBlank();
        assertThat(authService.login(loginRequest("staff", "staff")).getAccessToken()).isNotBlank();
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
    void 결제_인원이_모자란_세부_투어를_마감하면_취소된다() {
        Tour cebu = findTour("세부 허니문 5박 6일");

        tourLifecycleService.closeTour(cebu.getTourId());

        assertThat(cebu.getTourStatus()).isEqualTo(TourStatus.CANCELLED);
    }

    @Test
    void 나트랑_투어는_결제_대기_예약까지_결제하면_확정_인원이_된다() {
        Tour nhaTrang = findTour("나트랑 힐링 리조트 4박 5일");

        int guestCount = bookingRepository.findAllByTourIdAndBookingStatus(nhaTrang.getTourId(), BookingStatus.PAID)
                .stream().mapToInt(Booking::getGuestCount).sum();
        int pendingGuestCount = bookingRepository.findAllByTourIdAndBookingStatus(nhaTrang.getTourId(), BookingStatus.PENDING)
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
