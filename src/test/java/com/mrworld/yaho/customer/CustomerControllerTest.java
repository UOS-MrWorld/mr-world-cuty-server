package com.mrworld.yaho.customer;

import com.mrworld.yaho.common.enums.LoyaltyGrade;
import com.mrworld.yaho.common.enums.Role;
import com.mrworld.yaho.member.Member;
import com.mrworld.yaho.member.MemberRepository;
import com.mrworld.yaho.security.JwtTokenProvider;
import com.mrworld.yaho.support.DatabaseCleaner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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

    @Autowired private DatabaseCleaner databaseCleaner;
    @Autowired private MockMvc mockMvc;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String tag;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        tag = UUID.randomUUID().toString().substring(0, 8);
    }

    @AfterEach
    void tearDown() {
        databaseCleaner.clean();
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
                .andExpect(jsonPath("$.policies[0].discountRate").value(0))
                .andExpect(jsonPath("$.policies[1].grade").value("VIP"))
                .andExpect(jsonPath("$.policies[1].discountRate").value(10));
    }

    @Test
    void 고객_목록은_최소_정보와_단골_등급만_보여준다() throws Exception {
        Customer vip = saveCustomer("김단골", LoyaltyGrade.VIP);
        saveCustomer("이일반", LoyaltyGrade.NORMAL);

        mockMvc.perform(get(URL + "/customers").param("keyword", tag)
                        .header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].customerId").value(vip.getMemberId()))
                .andExpect(jsonPath("$.content[0].name").value("김단골" + tag))
                .andExpect(jsonPath("$.content[0].loyaltyGrade").value("VIP"))
                .andExpect(jsonPath("$.content[0].phoneNum").doesNotExist())
                .andExpect(jsonPath("$.content[0].completedTourCount").doesNotExist());

        mockMvc.perform(get(URL + "/customers").param("keyword", tag)
                        .param("loyaltyGrade", "VIP")
                        .header("Authorization", bearer(Role.STAFF)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void 여러_고객의_등급을_한_요청으로_변경한다() throws Exception {
        Customer first = saveCustomer("첫째", LoyaltyGrade.NORMAL);
        Customer second = saveCustomer("둘째", LoyaltyGrade.VIP);
        String body = """
                {"changes": [
                  {"customerId": %d, "loyaltyGrade": "VIP"},
                  {"customerId": %d, "loyaltyGrade": "NORMAL"}
                ]}
                """.formatted(first.getMemberId(), second.getMemberId());

        mockMvc.perform(patch(URL + "/customers/loyalty-grades")
                        .header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(2))
                .andExpect(jsonPath("$.customers[0].loyaltyGrade").value("VIP"))
                .andExpect(jsonPath("$.customers[1].loyaltyGrade").value("NORMAL"));

        // 등급 변경과 할인 활성화는 별도 정책이다. 등급만 바꾼 직후에는 할인율이 0이다.
        assertThat(customerRepository.findById(first.getMemberId()).orElseThrow().currentDiscountRate()).isZero();
        assertThat(customerRepository.findById(second.getMemberId()).orElseThrow().currentDiscountRate()).isZero();
    }

    @Test
    void 중복되거나_없는_고객이_있으면_전체_요청을_거절한다() throws Exception {
        Customer customer = saveCustomer("고객", LoyaltyGrade.NORMAL);
        String duplicate = """
                {"changes": [
                  {"customerId": %d, "loyaltyGrade": "VIP"},
                  {"customerId": %d, "loyaltyGrade": "NORMAL"}
                ]}
                """.formatted(customer.getMemberId(), customer.getMemberId());
        mockMvc.perform(patch(URL + "/customers/loyalty-grades")
                        .header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON).content(duplicate))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch(URL + "/customers/loyalty-grades")
                        .header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changes\":[{\"customerId\":999999,\"loyaltyGrade\":\"VIP\"}]}"))
                .andExpect(status().isNotFound());

        assertThat(customerRepository.findById(customer.getMemberId()).orElseThrow().getLoyaltyGrade())
                .isEqualTo(LoyaltyGrade.NORMAL);
    }

    private Customer saveCustomer(String name, LoyaltyGrade grade) {
        Member member = memberRepository.save(Member.create(
                "login-" + tag + "-" + name, "pw", name + tag, "01012345678", "서울", Role.CUSTOMER));
        Customer customer = Customer.create(member);
        customer.updateLoyaltyGrade(grade);
        return customerRepository.save(customer);
    }

    private String bearer(Role role) {
        return "Bearer " + jwtTokenProvider.createToken(1L, role);
    }
}
