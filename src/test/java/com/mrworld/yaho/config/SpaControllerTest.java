package com.mrworld.yaho.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SpaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 프론트_주소는_인증_없이_index로_전달한다() throws Exception {
        for (String path : new String[]{
                "/login", "/tours", "/tours/1", "/tours/1/checkout", "/my-trips", "/staff/tours"
        }) {
            mockMvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(forwardedUrl("/index.html"));
        }
    }

    // 배포 스크립트(deploy.yml)는 이 주소가 200을 반환할 때만 배포 성공으로 본다.
    @Test
    void 배포_헬스체크_주소는_인증_없이_UP을_반환하고_세부_정보는_숨긴다() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void 보호_API는_계속_인증이_필요하다() throws Exception {
        mockMvc.perform(get("/api/v1/customer/tours"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isUnauthorized());
    }
}
