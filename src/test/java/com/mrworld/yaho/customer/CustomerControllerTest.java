package com.mrworld.yaho.customer;

import com.mrworld.yaho.booking.Booking;
import com.mrworld.yaho.booking.BookingRepository;
import com.mrworld.yaho.common.enums.HotelGrade;
import com.mrworld.yaho.common.enums.LoyaltyGrade;
import com.mrworld.yaho.common.enums.MealType;
import com.mrworld.yaho.common.enums.Role;
import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourLevel;
import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.common.enums.Transport;
import com.mrworld.yaho.member.Member;
import com.mrworld.yaho.member.MemberRepository;
import com.mrworld.yaho.payment.PaymentRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerControllerTest {

    private static final String URL = "/api/v1/staff";

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // 다른 테스트가 만든 고객과 섞이지 않도록, 이 테스트의 고객 이름·아이디에 붙이는 표시
    private String tag;

    @BeforeEach
    void setUp() {
        cleanUp();
        tag = UUID.randomUUID().toString().substring(0, 8);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    void 고객_토큰이면_403이다() throws Exception {
        mockMvc.perform(get(URL + "/customers").header("Authorization", bearer(Role.CUSTOMER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(URL + "/loyalty-policy").header("Authorization", bearer(Role.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 단골_등급_기준을_조회한다() throws Exception {
        mockMvc.perform(get(URL + "/loyalty-policy").header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policies[0].grade").value("NORMAL"))
                .andExpect(jsonPath("$.policies[0].requiredTourCount").value(0))
                .andExpect(jsonPath("$.policies[0].discountRate").value(0))
                .andExpect(jsonPath("$.policies[1].grade").value("VIP"))
                .andExpect(jsonPath("$.policies[1].requiredTourCount").value(5))
                .andExpect(jsonPath("$.policies[1].discountRate").value(10));
    }

    @Test
    void 고객_목록은_검색어_등급_할인_여부로_거르고_완료_여행_횟수를_보여준다() throws Exception {
        Customer vip = saveCustomer("김단골", LoyaltyGrade.VIP, false);
        saveCompletedTrips(vip, 2);
        saveCustomer("이일반", LoyaltyGrade.NORMAL, false);
        saveCustomer("박할인", LoyaltyGrade.VIP, true);

        mockMvc.perform(get(URL + "/customers").param("keyword", tag)
                        .header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].customerId").value(vip.getMemberId()))
                .andExpect(jsonPath("$.content[0].name").value("김단골" + tag))
                .andExpect(jsonPath("$.content[0].phoneNum").value("01012345678"))
                .andExpect(jsonPath("$.content[0].completedTourCount").value(2))
                .andExpect(jsonPath("$.content[0].loyaltyGrade").value("VIP"))
                .andExpect(jsonPath("$.content[0].loyaltyDiscountApplied").value(false))
                .andExpect(jsonPath("$.content[1].completedTourCount").value(0));

        mockMvc.perform(get(URL + "/customers").param("keyword", "김단골" + tag)
                        .header("Authorization", bearer(Role.STAFF)))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get(URL + "/customers").param("keyword", tag)
                        .param("loyaltyGrade", "VIP").param("discountApplied", "false")
                        .header("Authorization", bearer(Role.STAFF)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].customerId").value(vip.getMemberId()));
    }

    @Test
    void 고객_목록의_필터나_페이지_값이_잘못되면_400이다() throws Exception {
        mockMvc.perform(get(URL + "/customers").param("loyaltyGrade", "GOLD")
                        .header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(URL + "/customers").param("page", "-1")
                        .header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 고객_상세를_조회한다() throws Exception {
        Customer customer = saveCustomer("정지윤", LoyaltyGrade.VIP, false);
        saveCompletedTrips(customer, 1);

        mockMvc.perform(get(URL + "/customers/" + customer.getMemberId())
                        .header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customer.getMemberId()))
                .andExpect(jsonPath("$.loginId").value("login-" + tag + "-정지윤"))
                .andExpect(jsonPath("$.name").value("정지윤" + tag))
                .andExpect(jsonPath("$.address").value("서울"))
                .andExpect(jsonPath("$.completedTourCount").value(1))
                .andExpect(jsonPath("$.loyaltyGrade").value("VIP"))
                .andExpect(jsonPath("$.loyaltyDiscountApplied").value(false))
                .andExpect(jsonPath("$.loyaltyDiscountAppliedAt").doesNotExist());

        mockMvc.perform(get(URL + "/customers/999999").header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isNotFound());
    }

    @Test
    void 등급으로_일괄_적용하면_미적용_고객만_켜고_이미_적용된_고객은_건너뛴다() throws Exception {
        Customer target = saveCustomer("김단골", LoyaltyGrade.VIP, false);
        saveCustomer("박할인", LoyaltyGrade.VIP, true);
        long vipCount = customerRepository.countByLoyaltyGrade(LoyaltyGrade.VIP);
        long unappliedVipCount = customerRepository
                .findAllByLoyaltyGradeAndLoyaltyDiscountAppliedFalse(LoyaltyGrade.VIP).size();

        mockMvc.perform(patch(URL + "/customers/loyalty-discount/by-grade")
                        .header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"loyaltyGrade\": \"VIP\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetCount").value(vipCount))
                .andExpect(jsonPath("$.appliedCount").value(unappliedVipCount))
                .andExpect(jsonPath("$.skippedCount").value(vipCount - unappliedVipCount));

        Customer updated = customerRepository.findById(target.getMemberId()).orElseThrow();
        assertThat(updated.isLoyaltyDiscountApplied()).isTrue();
        assertThat(updated.getLoyaltyDiscountAppliedAt()).isNotNull();
    }

    @Test
    void 등급이_없거나_적용할_고객이_없으면_400이다() throws Exception {
        saveCustomer("김단골", LoyaltyGrade.VIP, false);
        saveCustomer("이일반", LoyaltyGrade.NORMAL, false);
        String url = URL + "/customers/loyalty-discount/by-grade";

        mockMvc.perform(patch(url).header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        // NORMAL은 할인율이 0이라 적용 대상이 없다.
        mockMvc.perform(patch(url).header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"loyaltyGrade\": \"NORMAL\"}"))
                .andExpect(status().isBadRequest());

        // 이미 모든 VIP에게 할인이 켜져 있으면 다시 적용할 고객이 없다.
        mockMvc.perform(patch(url).header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"loyaltyGrade\": \"VIP\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch(url).header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"loyaltyGrade\": \"VIP\"}"))
                .andExpect(status().isBadRequest());
    }

    private Customer saveCustomer(String name, LoyaltyGrade grade, boolean discountApplied) {
        Member member = memberRepository.save(Member.create(
                "login-" + tag + "-" + name, "pw", name + tag, "01012345678", "서울", Role.CUSTOMER));
        Customer customer = Customer.create(member);
        ReflectionTestUtils.setField(customer, "loyaltyGrade", grade);
        ReflectionTestUtils.setField(customer, "loyaltyDiscountApplied", discountApplied);
        return customerRepository.save(customer);
    }

    // 결제하고 다녀온(완료된) 여행을 count번 만든다.
    private void saveCompletedTrips(Customer customer, int count) {
        for (int i = 0; i < count; i++) {
            LocalDate startDate = LocalDate.now().minusDays(30 + i * 10L);
            Tour tour = Tour.create(Theme.GOLF, "완료된 투어", null, startDate, startDate.plusDays(3),
                    500000, Transport.PREMIUM_VAN_10);
            ReflectionTestUtils.setField(tour, "tourStatus", TourStatus.COMPLETED);
            tour = tourRepository.save(tour);
            Booking booking = Booking.create(tour, customer, TourLevel.CLASSIC, 1,
                    HotelGrade.THREE_STAR, MealType.LUNCH_BOX, Transport.PREMIUM_VAN_10, 1_000_000, 0);
            booking.pay(0);
            bookingRepository.save(booking);
        }
    }

    private String bearer(Role role) {
        return "Bearer " + jwtTokenProvider.createToken(1L, role);
    }

    private void cleanUp() {
        databaseCleaner.clean();
    }
}
