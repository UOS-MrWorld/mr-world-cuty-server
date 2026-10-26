package com.mrworld.yaho.member;

import com.mrworld.yaho.common.enums.Role;
import com.mrworld.yaho.security.JwtTokenProvider;
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
class MemberControllerTest {

    private static final String URL = "/api/v1/members/me";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Member staff;

    @BeforeEach
    void setUp() {
        // 다른 테스트와 같은 DB를 쓰므로 loginId가 겹치지 않게 만든다.
        staff = memberRepository.save(Member.create("staff-" + UUID.randomUUID(), "encoded", "김직원",
                "01012345678", "서울특별시 동대문구", Role.STAFF));
    }

    @Test
    void 토큰이_없으면_401이다() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(patch(URL).contentType(MediaType.APPLICATION_JSON).content("{\"address\":\"부산\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 직원도_내_회원정보를_조회한다() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", bearer(staff)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(staff.getMemberId()))
                .andExpect(jsonPath("$.loginId").value(staff.getLoginId()))
                .andExpect(jsonPath("$.name").value("김직원"))
                .andExpect(jsonPath("$.role").value("STAFF"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void 전화번호와_주소를_수정한다() throws Exception {
        mockMvc.perform(patch(URL).header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNum\":\"01098765432\",\"address\":\"부산광역시 해운대구\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(staff.getMemberId()))
                .andExpect(jsonPath("$.phoneNum").value("01098765432"))
                .andExpect(jsonPath("$.address").value("부산광역시 해운대구"));

        Member updated = memberRepository.findById(staff.getMemberId()).orElseThrow();
        assertThat(updated.getPhoneNum()).isEqualTo("01098765432");
        assertThat(updated.getAddress()).isEqualTo("부산광역시 해운대구");
    }

    @Test
    void 형식이_틀리거나_수정값이_없으면_400이다() throws Exception {
        String[] bodies = {
                "{}",
                "{\"phoneNum\":\"010-9876-5432\"}",
                "{\"address\":\"   \"}"
        };
        for (String body : bodies) {
            mockMvc.perform(patch(URL).header("Authorization", bearer(staff))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    private String bearer(Member member) {
        return "Bearer " + jwtTokenProvider.createToken(member.getMemberId(), member.getRole());
    }
}
