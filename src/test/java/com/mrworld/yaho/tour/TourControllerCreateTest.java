package com.mrworld.yaho.tour;

import com.jayway.jsonpath.JsonPath;
import com.mrworld.yaho.common.enums.Role;
import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.inventory.InventoryItem;
import com.mrworld.yaho.inventory.InventoryItemRepository;
import com.mrworld.yaho.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TourControllerCreateTest {

    private static final String URL = "/api/v1/staff/tours";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private InventoryItem scarf;

    @BeforeEach
    void setUp() {
        tourRepository.deleteAll();
        inventoryItemRepository.deleteAll();
        scarf = inventoryItemRepository.save(InventoryItem.create("스카프", 50));
    }

    // 기념품이 재고를 참조하므로, 다른 테스트가 재고를 지울 수 있게 투어부터 정리한다.
    @AfterEach
    void tearDown() {
        tourRepository.deleteAll();
        inventoryItemRepository.deleteAll();
    }

    @Test
    @Transactional
    void 상품을_기념품_구성과_함께_모집_중으로_등록한다() throws Exception {
        String response = mockMvc.perform(post(URL).header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("2026-11-20", "2026-11-23", 450000, "PREMIUM_VAN_10",
                                "[{\"itemId\":" + scarf.getItemId() + ",\"quantityPerGuest\":2}]")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tourId").exists())
                .andExpect(jsonPath("$.tourStatus").value("RECRUITING"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn().getResponse().getContentAsString();

        Long tourId = ((Number) JsonPath.read(response, "$.tourId")).longValue();
        Tour tour = tourRepository.findById(tourId).orElseThrow();
        assertThat(tour.getTourStatus()).isEqualTo(TourStatus.RECRUITING);
        assertThat(tour.getPrice()).isEqualTo(450000);
        assertThat(tour.getTourGifts()).hasSize(1);
        assertThat(tour.getTourGifts().get(0).getInventoryItem().getItemId()).isEqualTo(scarf.getItemId());
        assertThat(tour.getTourGifts().get(0).getQuantityPerGuest()).isEqualTo(2);
    }

    @Test
    void 기념품_없이도_등록할_수_있다() throws Exception {
        mockMvc.perform(post(URL).header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("2026-11-20", "2026-11-20", 0, "PREMIUM_CAR_2", null)))
                .andExpect(status().isCreated());
    }

    @Test
    void 토큰이_없으면_401_고객이면_403이다() throws Exception {
        String request = body("2026-11-20", "2026-11-23", 450000, "PREMIUM_VAN_10", null);
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(URL).header("Authorization", bearer(Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
    }

    @Test
    void 날짜_가격_교통_기념품_수량이_잘못되면_400이다() throws Exception {
        String gift = "[{\"itemId\":" + scarf.getItemId() + ",\"quantityPerGuest\":";
        String[] bodies = {
                body("2026-11-23", "2026-11-20", 450000, "PREMIUM_VAN_10", null),
                body("2026-11-20", "2026-11-23", -1, "PREMIUM_VAN_10", null),
                body("2026-11-20", "2026-11-23", 450000, "BUS", null),
                body("2026-11-20", "2026-11-23", 450000, "PREMIUM_VAN_10", gift + "0}]"),
                body("2026-11-20", "2026-11-23", 450000, "PREMIUM_VAN_10",
                        "[{\"itemId\":" + scarf.getItemId() + ",\"quantityPerGuest\":1},"
                                + "{\"itemId\":" + scarf.getItemId() + ",\"quantityPerGuest\":1}]")
        };
        for (String request : bodies) {
            mockMvc.perform(post(URL).header("Authorization", bearer(Role.STAFF))
                            .contentType(MediaType.APPLICATION_JSON).content(request))
                    .andExpect(status().isBadRequest());
        }
        assertThat(tourRepository.count()).isZero();
    }

    @Test
    void 없는_기념품_재고를_지정하면_404이다() throws Exception {
        mockMvc.perform(post(URL).header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("2026-11-20", "2026-11-23", 450000, "PREMIUM_VAN_10",
                                "[{\"itemId\":99999,\"quantityPerGuest\":1}]")))
                .andExpect(status().isNotFound());
        assertThat(tourRepository.count()).isZero();
    }

    private String body(String startDate, String endDate, int basePrice, String transport, String gifts) {
        return "{\"theme\":\"TREKKING\",\"tourName\":\"아웃도어 트레킹 투어\",\"description\":\"산악 테마 트레킹 투어\","
                + "\"startDate\":\"" + startDate + "\",\"endDate\":\"" + endDate + "\","
                + "\"basePrice\":" + basePrice + ",\"defaultTransport\":\"" + transport + "\""
                + (gifts == null ? "" : ",\"gifts\":" + gifts) + "}";
    }

    private String bearer(Role role) {
        return "Bearer " + jwtTokenProvider.createToken(1L, role);
    }
}
