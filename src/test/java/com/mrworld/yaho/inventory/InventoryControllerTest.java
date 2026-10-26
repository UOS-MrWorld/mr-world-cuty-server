package com.mrworld.yaho.inventory;

import com.mrworld.yaho.common.enums.Role;
import com.mrworld.yaho.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryControllerTest {

    private static final String BASE_URL = "/api/v1/staff/inventory";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private InventoryItem scarf;

    @BeforeEach
    void setUp() {
        inventoryItemRepository.deleteAll();
        scarf = inventoryItemRepository.save(InventoryItem.create("스카프", 92));
        inventoryItemRepository.save(InventoryItem.create("텀블러", 30));
    }

    @Test
    void 토큰이_없으면_401_고객이면_403이다() throws Exception {
        mockMvc.perform(get(BASE_URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE_URL).header("Authorization", bearer(Role.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 목록을_물품명_검색어로_조회한다() throws Exception {
        mockMvc.perform(get(BASE_URL).header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get(BASE_URL).param("keyword", "스카").header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].itemName").value("스카프"))
                .andExpect(jsonPath("$.content[0].quantity").value(92))
                .andExpect(jsonPath("$.content[0].updatedAt").exists());
    }

    @Test
    void 물품을_등록하면_201이고_같은_이름은_409이다() throws Exception {
        mockMvc.perform(post(BASE_URL).header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemName\":\"에코백\",\"quantity\":100}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itemId").exists())
                .andExpect(jsonPath("$.itemName").value("에코백"))
                .andExpect(jsonPath("$.quantity").value(100))
                .andExpect(jsonPath("$.createdAt").exists());

        mockMvc.perform(post(BASE_URL).header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemName\":\" 스카프 \",\"quantity\":10}"))
                .andExpect(status().isConflict());
    }

    @Test
    void 등록_요청이_잘못되면_400이다() throws Exception {
        String[] bodies = {
                "{\"itemName\":\"  \",\"quantity\":10}",
                "{\"itemName\":\"에코백\",\"quantity\":-1}",
                "{\"itemName\":\"에코백\"}"
        };
        for (String body : bodies) {
            mockMvc.perform(post(BASE_URL).header("Authorization", bearer(Role.STAFF))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void 보낸_값만_수정한다() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/" + scarf.getItemId()).header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":120}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemName").value("스카프"))
                .andExpect(jsonPath("$.quantity").value(120))
                .andExpect(jsonPath("$.updatedAt").exists());

        // 자기 자신의 이름으로 바꾸는 건 중복이 아니다.
        mockMvc.perform(patch(BASE_URL + "/" + scarf.getItemId()).header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemName\":\"스카프\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void 수정_오류는_명세의_상태코드를_반환한다() throws Exception {
        String url = BASE_URL + "/" + scarf.getItemId();
        mockMvc.perform(patch(url).header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(url).header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":-5}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(url).header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"itemName\":\"텀블러\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(patch(BASE_URL + "/99999").header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":1}"))
                .andExpect(status().isNotFound());
    }

    private String bearer(Role role) {
        return "Bearer " + jwtTokenProvider.createToken(1L, role);
    }
}
