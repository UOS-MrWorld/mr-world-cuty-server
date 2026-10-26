package com.mrworld.yaho.inventory;

import com.mrworld.yaho.common.enums.Role;
import com.mrworld.yaho.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    private String bearer(Role role) {
        return "Bearer " + jwtTokenProvider.createToken(1L, role);
    }
}
