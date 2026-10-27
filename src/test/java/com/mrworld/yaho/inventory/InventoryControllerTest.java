package com.mrworld.yaho.inventory;

import com.mrworld.yaho.booking.Booking;
import com.mrworld.yaho.booking.BookingRepository;
import com.mrworld.yaho.common.enums.HotelGrade;
import com.mrworld.yaho.common.enums.MealType;
import com.mrworld.yaho.common.enums.Role;
import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourLevel;
import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.common.enums.Transport;
import com.mrworld.yaho.customer.Customer;
import com.mrworld.yaho.customer.CustomerRepository;
import com.mrworld.yaho.member.Member;
import com.mrworld.yaho.member.MemberRepository;
import com.mrworld.yaho.payment.PaymentRepository;
import com.mrworld.yaho.security.JwtTokenProvider;
import com.mrworld.yaho.support.DatabaseCleaner;
import com.mrworld.yaho.tour.Tour;
import com.mrworld.yaho.tour.TourRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

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
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MemberRepository memberRepository;

    private InventoryItem scarf;

    @BeforeEach
    void setUp() {
        cleanUp();
        scarf = inventoryItemRepository.save(InventoryItem.create("스카프", 92));
        inventoryItemRepository.save(InventoryItem.create("텀블러", 30));
    }

    // 기념품이 재고를, 예약이 투어를 참조하므로 결제·예약·투어부터 정리한다.
    @AfterEach
    void tearDown() {
        cleanUp();
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

    @Test
    void 목록은_모집_중_투어의_결제_인원에게_줄_사용_예정_수량을_함께_보여준다() throws Exception {
        Customer customer = saveCustomer();
        // 모집 중 투어: 결제 3명 + 결제 전 4명 → 결제한 3명 × 1인당 2개 = 6개만 사용 예정
        Tour recruiting = saveTourWithScarf(TourStatus.RECRUITING);
        saveBooking(recruiting, customer, 3, true);
        saveBooking(recruiting, customer, 4, false);
        // 확정된 투어는 이미 재고에서 차감했으므로 사용 예정에 넣지 않는다.
        Tour confirmed = saveTourWithScarf(TourStatus.CONFIRMED);
        saveBooking(confirmed, customer, 5, true);

        mockMvc.perform(get(BASE_URL).param("keyword", "스카프").header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].quantity").value(92))
                .andExpect(jsonPath("$.content[0].requiredQuantity").value(6));

        mockMvc.perform(get(BASE_URL).param("keyword", "텀블러").header("Authorization", bearer(Role.STAFF)))
                .andExpect(jsonPath("$.content[0].requiredQuantity").value(0));
    }

    private Tour saveTourWithScarf(TourStatus tourStatus) {
        LocalDate startDate = LocalDate.now().plusDays(30);
        Tour tour = Tour.create(Theme.TREKKING, "아웃도어 트레킹 투어", null,
                startDate, startDate.plusDays(3), 500000, Transport.PREMIUM_VAN_10);
        tour.addGift(scarf, 2);
        ReflectionTestUtils.setField(tour, "tourStatus", tourStatus);
        return tourRepository.save(tour);
    }

    private void saveBooking(Tour tour, Customer customer, int guestCount, boolean paid) {
        Booking booking = Booking.create(tour, customer, TourLevel.CLASSIC, guestCount,
                HotelGrade.THREE_STAR, MealType.LUNCH_BOX, Transport.PREMIUM_VAN_10, 1_000_000, 0);
        if (paid) {
            booking.pay(0);
        }
        bookingRepository.save(booking);
    }

    private Customer saveCustomer() {
        Member member = memberRepository.save(Member.create(
                "inventory-" + UUID.randomUUID(), "pw", "고객", "01012345678", "서울", Role.CUSTOMER));
        return customerRepository.save(Customer.create(member));
    }

    private void cleanUp() {
        databaseCleaner.clean();
    }

    private String bearer(Role role) {
        return "Bearer " + jwtTokenProvider.createToken(1L, role);
    }
}
