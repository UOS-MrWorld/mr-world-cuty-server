package com.mrworld.yaho.demo;

import com.mrworld.yaho.booking.Booking;
import com.mrworld.yaho.booking.BookingRepository;
import com.mrworld.yaho.common.enums.HotelGrade;
import com.mrworld.yaho.common.enums.LoyaltyGrade;
import com.mrworld.yaho.common.enums.MealType;
import com.mrworld.yaho.common.enums.Role;
import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourLevel;
import com.mrworld.yaho.common.enums.Transport;
import com.mrworld.yaho.customer.Customer;
import com.mrworld.yaho.customer.CustomerRepository;
import com.mrworld.yaho.member.Member;
import com.mrworld.yaho.member.MemberRepository;
import com.mrworld.yaho.security.JwtTokenProvider;
import com.mrworld.yaho.support.DatabaseCleaner;
import com.mrworld.yaho.tour.Tour;
import com.mrworld.yaho.tour.TourRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DemoControllerTest {

    private static final String URL = "/api/v1/staff/demo/tours/";

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
    }

    @AfterEach
    void tearDown() {
        databaseCleaner.clean();
    }

    @Test
    void 날짜와_상관없이_마감하면_확정되고_완료하면_단골_등급이_다시_정해진다() throws Exception {
        // 시작일이 한참 남아 스케줄러라면 마감하지 않을 투어
        Tour tour = saveTour(LocalDate.now().plusDays(60));
        Customer customer = saveCustomer();
        savePaidBooking(tour, customer, 3);

        mockMvc.perform(post(URL + tour.getTourId() + "/close").header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tourId").value(tour.getTourId()))
                .andExpect(jsonPath("$.tourStatus").value("CONFIRMED"));

        mockMvc.perform(post(URL + tour.getTourId() + "/complete").header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tourStatus").value("COMPLETED"));

        // 완료 여행 1번이라 NORMAL 그대로다.
        assertThat(customerRepository.findById(customer.getMemberId()).orElseThrow().getLoyaltyGrade())
                .isEqualTo(LoyaltyGrade.NORMAL);
    }

    @Test
    void 결제_인원이_3명_미만이면_마감_시_취소된다() throws Exception {
        Tour tour = saveTour(LocalDate.now().plusDays(60));

        mockMvc.perform(post(URL + tour.getTourId() + "/close").header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tourStatus").value("CANCELLED"));
    }

    @Test
    void 상태가_맞지_않으면_409_투어가_없으면_404_고객이면_403이다() throws Exception {
        Tour tour = saveTour(LocalDate.now().plusDays(60));

        mockMvc.perform(post(URL + tour.getTourId() + "/complete").header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isConflict());
        mockMvc.perform(post(URL + "999999/close").header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(URL + tour.getTourId() + "/close").header("Authorization", bearer(Role.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    private Tour saveTour(LocalDate startDate) {
        return tourRepository.save(Tour.create(Theme.GOLF, "데모 투어", null, startDate, startDate.plusDays(3),
                500000, Transport.PREMIUM_VAN_10));
    }

    private Customer saveCustomer() {
        Member member = memberRepository.save(Member.create(
                "demo-" + UUID.randomUUID(), "pw", "데모고객", "01012345678", "서울", Role.CUSTOMER));
        return customerRepository.save(Customer.create(member));
    }

    private void savePaidBooking(Tour tour, Customer customer, int guestCount) {
        Booking booking = Booking.create(tour, customer, TourLevel.CLASSIC, guestCount,
                HotelGrade.THREE_STAR, MealType.LUNCH_BOX, Transport.PREMIUM_VAN_10, 1_000_000, 0);
        booking.pay(0);
        bookingRepository.save(booking);
    }

    private String bearer(Role role) {
        return "Bearer " + jwtTokenProvider.createToken(1L, role);
    }
}
