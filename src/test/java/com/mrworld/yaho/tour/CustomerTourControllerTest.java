package com.mrworld.yaho.tour;

import com.mrworld.yaho.member.Member;
import com.mrworld.yaho.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerTourControllerTest {

    private static final String BASE_URL = "/api/v1/customer/tours";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private JwtProvider jwtProvider;

    private Tour golfTour;

    @BeforeEach
    void setUp() {
        tourRepository.deleteAll();
        golfTour = tourRepository.save(Tour.create(Tour.Theme.GOLF, "골프 챌린지 투어", "유명 골프 리조트",
                LocalDate.parse("2026-10-20"), LocalDate.parse("2026-10-23"), 500000, Tour.Transport.PREMIUM_VAN_10));
        tourRepository.save(Tour.create(Tour.Theme.HEALING, "효도 힐링 투어", "제주 온천 휴양",
                LocalDate.parse("2026-11-20"), LocalDate.parse("2026-11-23"), 450000, Tour.Transport.PREMIUM_CAR_2));
    }

    @Test
    void 토큰이_없으면_401이다() throws Exception {
        mockMvc.perform(get(BASE_URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE_URL + "/search")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE_URL + "/" + golfTour.getId())).andExpect(status().isUnauthorized());
    }

    @Test
    void 직원_토큰이면_403이다() throws Exception {
        mockMvc.perform(get(BASE_URL).header("Authorization", bearer(Member.Role.STAFF)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 목록을_조회한다() throws Exception {
        mockMvc.perform(get(BASE_URL).header("Authorization", bearer(Member.Role.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].tourName").value("골프 챌린지 투어"))
                .andExpect(jsonPath("$.content[0].tourStatus").value("RECRUITING"));
    }

    @Test
    void 잘못된_페이지_값은_400이다() throws Exception {
        mockMvc.perform(get(BASE_URL).param("page", "-1").header("Authorization", bearer(Member.Role.CUSTOMER)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE_URL).param("size", "0").header("Authorization", bearer(Member.Role.CUSTOMER)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 검색은_search_경로로_동작하고_조건이_AND로_적용된다() throws Exception {
        mockMvc.perform(get(BASE_URL + "/search")
                        .param("theme", "GOLF").param("maxPrice", "500000").param("departureDate", "2026-10-20")
                        .header("Authorization", bearer(Member.Role.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].theme").value("GOLF"));

        mockMvc.perform(get(BASE_URL + "/search")
                        .param("theme", "GOLF").param("maxPrice", "100000")
                        .header("Authorization", bearer(Member.Role.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void 잘못된_검색_조건은_400이다() throws Exception {
        mockMvc.perform(get(BASE_URL + "/search").param("maxPrice", "-1")
                        .header("Authorization", bearer(Member.Role.CUSTOMER)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE_URL + "/search").param("theme", "UNKNOWN")
                        .header("Authorization", bearer(Member.Role.CUSTOMER)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE_URL + "/search").param("departureDate", "20261020")
                        .header("Authorization", bearer(Member.Role.CUSTOMER)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 상세를_조회한다() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + golfTour.getId()).header("Authorization", bearer(Member.Role.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tourId").value(golfTour.getId()))
                .andExpect(jsonPath("$.defaultTransport").value("PREMIUM_VAN_10"))
                .andExpect(jsonPath("$.availableTourLevels.length()").value(3));
    }

    @Test
    void 존재하지_않는_투어는_404이다() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999").header("Authorization", bearer(Member.Role.CUSTOMER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    private String bearer(Member.Role role) {
        Member member = Member.create("tester", "pw", "테스터", "010-0000-0000", "서울", role);
        ReflectionTestUtils.setField(member, "id", 1L);
        return "Bearer " + jwtProvider.generateAccessToken(member);
    }
}
