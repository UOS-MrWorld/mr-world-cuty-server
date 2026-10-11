package com.mrworld.yaho.tour;

import com.mrworld.yaho.common.enums.LoyaltyGrade;
import com.mrworld.yaho.common.enums.Role;
import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.common.enums.Transport;
import com.mrworld.yaho.customer.Customer;
import com.mrworld.yaho.customer.CustomerRepository;
import com.mrworld.yaho.member.Member;
import com.mrworld.yaho.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerTourQuoteControllerTest {

    private static final long MEMBER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DrinkOptionRepository drinkOptionRepository;

    @Autowired
    private JwtProvider jwtProvider;

    private Tour golfTour;
    private DrinkOption coffee;

    @BeforeEach
    void setUp() {
        tourRepository.deleteAll();
        customerRepository.deleteAll();
        drinkOptionRepository.deleteAll();

        LocalDate startDate = LocalDate.now().plusDays(30);
        golfTour = tourRepository.save(Tour.create(Theme.GOLF, "골프 챌린지 투어", "유명 골프 리조트",
                startDate, startDate.plusDays(3), 500000, Transport.PREMIUM_VAN_10));
        coffee = drinkOptionRepository.save(DrinkOption.create("COFFEE", 5000));
    }

    @Test
    void 토큰이_없으면_401이다() throws Exception {
        mockMvc.perform(post(quoteUrl(golfTour.getId())).contentType(MediaType.APPLICATION_JSON).content(body(2)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 직원_토큰이면_403이다() throws Exception {
        mockMvc.perform(post(quoteUrl(golfTour.getId()))
                        .header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON).content(body(2)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 가격을_계산한다() throws Exception {
        mockMvc.perform(post(quoteUrl(golfTour.getId()))
                        .header("Authorization", bearer(Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tourLevel": "GRAND",
                                  "guestCount": 2,
                                  "hotelGrade": "FOUR_STAR",
                                  "mealType": "LOCAL_RESTAURANT",
                                  "transport": "PREMIUM_VAN_10",
                                  "drinkOptions": [{"optionId": %d, "quantity": 2}]
                                }
                                """.formatted(coffee.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basePrice").value(1_000_000))
                .andExpect(jsonPath("$.optionPrice").value(410_000))
                .andExpect(jsonPath("$.discountRate").value(0))
                .andExpect(jsonPath("$.totalPrice").value(1_410_000))
                .andExpect(jsonPath("$.expectedPaymentAmount").value(1_410_000));
    }

    @Test
    void 할인이_적용된_단골_고객은_할인율이_반영된다() throws Exception {
        Customer customer = Customer.create(MEMBER_ID);
        ReflectionTestUtils.setField(customer, "loyaltyGrade", LoyaltyGrade.VIP);
        ReflectionTestUtils.setField(customer, "loyaltyDiscountApplied", true);
        customerRepository.save(customer);

        mockMvc.perform(post(quoteUrl(golfTour.getId()))
                        .header("Authorization", bearer(Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON).content(body(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountRate").value(10))
                .andExpect(jsonPath("$.totalPrice").value(1_600_000))
                .andExpect(jsonPath("$.expectedPaymentAmount").value(1_440_000));
    }

    @Test
    void 필수값이_없거나_인원이_1_미만이면_400이다() throws Exception {
        mockMvc.perform(post(quoteUrl(golfTour.getId()))
                        .header("Authorization", bearer(Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post(quoteUrl(golfTour.getId()))
                        .header("Authorization", bearer(Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON).content(body(0)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 알_수_없는_enum_값은_400이다() throws Exception {
        mockMvc.perform(post(quoteUrl(golfTour.getId()))
                        .header("Authorization", bearer(Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(2).replace("FIVE_STAR", "SIX_STAR")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 존재하지_않는_투어는_404이다() throws Exception {
        mockMvc.perform(post(quoteUrl(999999L))
                        .header("Authorization", bearer(Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON).content(body(2)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void 모집_중이_아닌_투어는_409이다() throws Exception {
        ReflectionTestUtils.setField(golfTour, "tourStatus", TourStatus.CANCELLED);
        tourRepository.save(golfTour);

        mockMvc.perform(post(quoteUrl(golfTour.getId()))
                        .header("Authorization", bearer(Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON).content(body(2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void 예약_마감된_투어는_400이다() throws Exception {
        ReflectionTestUtils.setField(golfTour, "startDate", LocalDate.now().plusDays(3));
        ReflectionTestUtils.setField(golfTour, "endDate", LocalDate.now().plusDays(6));
        tourRepository.save(golfTour);

        mockMvc.perform(post(quoteUrl(golfTour.getId()))
                        .header("Authorization", bearer(Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON).content(body(2)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 존재하지_않는_음료_옵션은_400이다() throws Exception {
        mockMvc.perform(post(quoteUrl(golfTour.getId()))
                        .header("Authorization", bearer(Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(2).replace("\"drinkOptions\": []",
                                "\"drinkOptions\": [{\"optionId\": 999999, \"quantity\": 1}]")))
                .andExpect(status().isBadRequest());
    }

    private String quoteUrl(Long tourId) {
        return "/api/v1/customer/tours/" + tourId + "/quote";
    }

    // 옵션 1인 추가금: 호텔 5성 250,000 + 식사 도시락 0 + 교통 밴 50,000 = 300,000
    private String body(int guestCount) {
        return """
                {
                  "tourLevel": "PREMIUM",
                  "guestCount": %d,
                  "hotelGrade": "FIVE_STAR",
                  "mealType": "LUNCH_BOX",
                  "transport": "PREMIUM_VAN_10",
                  "drinkOptions": []
                }
                """.formatted(guestCount);
    }

    private String bearer(Role role) {
        Member member = Member.create("tester", "pw", "테스터", "010-0000-0000", "서울", role);
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        return "Bearer " + jwtProvider.generateAccessToken(member);
    }
}
