package com.mrworld.yaho.payment;

import com.mrworld.yaho.booking.Booking;
import com.mrworld.yaho.booking.BookingRepository;
import com.mrworld.yaho.common.enums.BookingStatus;
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
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {

    private static final String URL = "/api/v1/customer/payments";

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private MockMvc mockMvc;

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

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Customer customer;
    private Tour golfTour;

    @BeforeEach
    void setUp() {
        cleanUp();

        customer = saveCustomer();
        LocalDate startDate = LocalDate.now().plusDays(30);
        golfTour = tourRepository.save(Tour.create(Theme.GOLF, "골프 챌린지 투어", "유명 골프 리조트",
                startDate, startDate.plusDays(3), 500000, Transport.PREMIUM_VAN_10));
    }

    // 결제가 예약을, 예약이 투어·고객을 참조하므로 결제부터 정리한다.
    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    void 토큰이_없으면_401이다() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body(1L, "1")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 카드번호가_1이면_승인되고_예약이_결제_완료된다() throws Exception {
        Booking booking = savePendingBooking(customer);

        mockMvc.perform(post(URL)
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content(body(booking.getBookingId(), "1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").isNumber())
                .andExpect(jsonPath("$.bookingId").value(booking.getBookingId()))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(1_000_000))
                .andExpect(jsonPath("$.paidAt").isNotEmpty())
                .andExpect(jsonPath("$.bookingStatus").value("PAID"));

        assertThat(bookingRepository.findById(booking.getBookingId()).orElseThrow().getBookingStatus())
                .isEqualTo(BookingStatus.PAID);
    }

    @Test
    void 결제_금액은_결제_시점_할인율로_계산하고_예약_할인율도_갱신한다() throws Exception {
        // 예약할 때는 할인이 없었고, 결제 전에 VIP 할인(10%)이 적용된 고객
        Booking booking = savePendingBooking(customer);
        ReflectionTestUtils.setField(customer, "loyaltyGrade", LoyaltyGrade.VIP);
        ReflectionTestUtils.setField(customer, "loyaltyDiscountApplied", true);
        customerRepository.save(customer);

        mockMvc.perform(post(URL)
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content(body(booking.getBookingId(), "1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(900_000));

        assertThat(bookingRepository.findById(booking.getBookingId()).orElseThrow().getDiscountRate())
                .isEqualTo(10);
    }

    @Test
    void 카드번호가_1이_아니면_거절되고_다시_결제할_수_있다() throws Exception {
        Booking booking = savePendingBooking(customer);

        mockMvc.perform(post(URL)
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content(body(booking.getBookingId(), "4111111111111111")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"))
                .andExpect(jsonPath("$.paidAt").doesNotExist())
                .andExpect(jsonPath("$.bookingStatus").value("PENDING"));

        mockMvc.perform(get("/api/v1/customer/bookings/" + booking.getBookingId())
                        .header("Authorization", bearer(customer)))
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"));

        mockMvc.perform(post(URL)
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content(body(booking.getBookingId(), "1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));

        mockMvc.perform(get("/api/v1/customer/bookings/" + booking.getBookingId())
                        .header("Authorization", bearer(customer)))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));
        assertThat(paymentRepository.findAll()).hasSize(2);
    }

    @Test
    void 이미_결제한_예약은_409이다() throws Exception {
        Booking booking = savePendingBooking(customer);
        mockMvc.perform(post(URL)
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content(body(booking.getBookingId(), "1")))
                .andExpect(status().isCreated());

        mockMvc.perform(post(URL)
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content(body(booking.getBookingId(), "1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void 취소된_예약이나_마감된_투어는_400이다() throws Exception {
        Booking cancelled = savePendingBooking(customer);
        ReflectionTestUtils.setField(cancelled, "bookingStatus", BookingStatus.CANCELLED);
        bookingRepository.save(cancelled);

        mockMvc.perform(post(URL)
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content(body(cancelled.getBookingId(), "1")))
                .andExpect(status().isBadRequest());

        Booking pending = savePendingBooking(customer);
        ReflectionTestUtils.setField(golfTour, "startDate", LocalDate.now().plusDays(6));
        ReflectionTestUtils.setField(golfTour, "endDate", LocalDate.now().plusDays(9));
        tourRepository.save(golfTour);

        mockMvc.perform(post(URL)
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content(body(pending.getBookingId(), "1")))
                .andExpect(status().isBadRequest());
        assertThat(paymentRepository.findAll()).isEmpty();
    }

    @Test
    void 다른_고객의_예약은_403_없는_예약은_404_카드번호가_없으면_400이다() throws Exception {
        Booking othersBooking = savePendingBooking(saveCustomer());

        mockMvc.perform(post(URL)
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content(body(othersBooking.getBookingId(), "1")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(URL)
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content(body(999999L, "1")))
                .andExpect(status().isNotFound());

        mockMvc.perform(post(URL)
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content(body(othersBooking.getBookingId(), " ")))
                .andExpect(status().isBadRequest());
    }

    // 결제 금액을 알아보기 쉽게 할인 전 총액을 1,000,000원, 예약 시점 할인율을 0으로 저장한다.
    private Booking savePendingBooking(Customer owner) {
        return bookingRepository.save(Booking.create(golfTour, owner, TourLevel.CLASSIC, 2,
                HotelGrade.THREE_STAR, MealType.LUNCH_BOX, Transport.PREMIUM_VAN_10, 1_000_000, 0));
    }

    private Customer saveCustomer() {
        Member member = memberRepository.save(Member.create(
                "payer-" + UUID.randomUUID(), "pw", "결제자", "010-0000-0000", "서울", Role.CUSTOMER));
        return customerRepository.save(Customer.create(member));
    }

    private String body(Long bookingId, String cardNumber) {
        return """
                {"bookingId": %d, "cardNumber": "%s"}
                """.formatted(bookingId, cardNumber);
    }

    private String bearer(Customer owner) {
        return "Bearer " + jwtTokenProvider.createToken(owner.getMemberId(), Role.CUSTOMER);
    }

    private void cleanUp() {
        databaseCleaner.clean();
    }
}
