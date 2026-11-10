package com.mrworld.yaho.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    // 인증 실패(401)처럼 Spring Security에서 끝나는 응답에도 보안 헤더가 붙어야 한다.
    @Test
    void 화면과_401_응답_모두에_보안_헤더를_붙인다() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpectAll(securityHeaders());
        mockMvc.perform(get("/api/v1/customer/tours"))
                .andExpect(status().isUnauthorized())
                .andExpectAll(securityHeaders());
    }

    private ResultMatcher[] securityHeaders() {
        return new ResultMatcher[]{
                header().string("X-Content-Type-Options", "nosniff"),
                header().string("X-Frame-Options", "DENY"),
                header().string("Referrer-Policy", "strict-origin-when-cross-origin"),
                header().string("Permissions-Policy", "camera=(), microphone=(self), geolocation=()")
        };
    }
}
