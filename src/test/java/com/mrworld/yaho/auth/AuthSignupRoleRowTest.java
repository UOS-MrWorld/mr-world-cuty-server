package com.mrworld.yaho.auth;

import com.mrworld.yaho.common.enums.LoyaltyGrade;
import com.mrworld.yaho.customer.Customer;
import com.mrworld.yaho.customer.CustomerRepository;
import com.mrworld.yaho.member.StaffRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSignupRoleRowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Test
    void 고객으로_가입하면_Customer_행이_기본값으로_함께_저장된다() throws Exception {
        long memberId = signup("CUSTOMER");

        Customer customer = customerRepository.findById(memberId).orElseThrow();
        assertThat(customer.getLoyaltyGrade()).isEqualTo(LoyaltyGrade.NORMAL);
        assertThat(customer.isLoyaltyDiscountApplied()).isFalse();
        assertThat(customer.currentDiscountRate()).isZero();
        assertThat(staffRepository.existsById(memberId)).isFalse();
    }

    @Test
    void 직원으로_가입하면_Staff_행이_함께_저장된다() throws Exception {
        long memberId = signup("STAFF");

        assertThat(staffRepository.existsById(memberId)).isTrue();
        assertThat(customerRepository.existsById(memberId)).isFalse();
    }

    private long signup(String role) throws Exception {
        String loginId = "user-" + UUID.randomUUID().toString().substring(0, 8);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "테스터",
                                  "loginId": "%s",
                                  "password": "pw1234!",
                                  "confirmPassword": "pw1234!",
                                  "phoneNum": "010-0000-0000",
                                  "address": "서울",
                                  "role": "%s"
                                }
                                """.formatted(loginId, role)))
                .andExpect(status().isCreated())
                .andReturn();

        Matcher matcher = Pattern.compile("\"memberId\"\\s*:\\s*(\\d+)")
                .matcher(result.getResponse().getContentAsString());
        assertThat(matcher.find()).isTrue();
        return Long.parseLong(matcher.group(1));
    }
}
