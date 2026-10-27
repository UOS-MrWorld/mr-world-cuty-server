package com.mrworld.yaho.booking;

import com.mrworld.yaho.common.enums.BookingStatus;
import com.mrworld.yaho.common.enums.HotelGrade;
import com.mrworld.yaho.common.enums.MealType;
import com.mrworld.yaho.common.enums.Role;
import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourLevel;
import com.mrworld.yaho.common.enums.Transport;
import com.mrworld.yaho.customer.Customer;
import com.mrworld.yaho.customer.CustomerRepository;
import com.mrworld.yaho.inventory.InventoryItem;
import com.mrworld.yaho.inventory.InventoryItemRepository;
import com.mrworld.yaho.member.Member;
import com.mrworld.yaho.member.MemberRepository;
import com.mrworld.yaho.security.JwtTokenProvider;
import com.mrworld.yaho.tour.DrinkOption;
import com.mrworld.yaho.tour.DrinkOptionRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerTest {

    private static final String URL = "/api/v1/customer/bookings";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private DrinkOptionRepository drinkOptionRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Customer customer;
    private Tour golfTour;
    private DrinkOption coffee;

    // 스카프 10개, 1인당 2개 지급 → 결제 완료 인원 포함 5명까지 예약 가능
    @BeforeEach
    void setUp() {
        cleanUp();

        Member member = memberRepository.save(Member.create(
                "booker-" + UUID.randomUUID(), "pw", "예약자", "010-0000-0000", "서울", Role.CUSTOMER));
        customer = customerRepository.save(Customer.create(member));

        InventoryItem scarf = inventoryItemRepository.save(InventoryItem.create("스카프", 10));
        LocalDate startDate = LocalDate.now().plusDays(30);
        Tour tour = Tour.create(Theme.GOLF, "골프 챌린지 투어", "유명 골프 리조트",
                startDate, startDate.plusDays(3), 500000, Transport.PREMIUM_VAN_10);
        tour.addGift(scarf, 2);
        golfTour = tourRepository.save(tour);
        coffee = drinkOptionRepository.save(DrinkOption.create("COFFEE", 5000));
    }

    // 예약이 투어·고객을 참조하므로, 다른 테스트가 투어·고객을 지울 수 있게 예약부터 정리한다.
    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    void 토큰이_없으면_401이다() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body(golfTour.getTourId(), 2)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 직원_토큰이면_403이다() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + jwtTokenProvider.createToken(1L, Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON).content(body(golfTour.getTourId(), 2)))
                .andExpect(status().isForbidden());
    }

    @Test
    void PENDING_예약을_생성한다() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tourId": %d,
                                  "tourLevel": "GRAND",
                                  "guestCount": 2,
                                  "hotelGrade": "FOUR_STAR",
                                  "mealType": "LOCAL_RESTAURANT",
                                  "transport": "PREMIUM_VAN_10",
                                  "drinkOptions": [{"optionId": %d, "quantity": 2}]
                                }
                                """.formatted(golfTour.getTourId(), coffee.getOptionId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").isNumber())
                .andExpect(jsonPath("$.bookingStatus").value("PENDING"))
                .andExpect(jsonPath("$.totalPrice").value(1_410_000))
                .andExpect(jsonPath("$.discountRate").value(0))
                .andExpect(jsonPath("$.expectedPaymentAmount").value(1_410_000));

        assertThat(bookingRepository.findAll()).hasSize(1);
    }

    @Test
    void 결제_완료_인원과_합쳐_기념품_재고가_부족하면_409이다() throws Exception {
        savePaidBooking(4);

        mockMvc.perform(post(URL)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(body(golfTour.getTourId(), 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        mockMvc.perform(post(URL)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(body(golfTour.getTourId(), 1)))
                .andExpect(status().isCreated());
    }

    @Test
    void 결제_전_예약은_재고_계산에_포함하지_않는다() throws Exception {
        bookingRepository.save(booking(4));

        mockMvc.perform(post(URL)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(body(golfTour.getTourId(), 2)))
                .andExpect(status().isCreated());
    }

    @Test
    void 투어_ID가_없으면_400이다() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(golfTour.getTourId(), 2).replace("\"tourId\": " + golfTour.getTourId() + ",", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void 존재하지_않는_투어는_404이다() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(body(999999L, 2)))
                .andExpect(status().isNotFound());
    }

    private void savePaidBooking(int guestCount) {
        Booking booking = booking(guestCount);
        ReflectionTestUtils.setField(booking, "bookingStatus", BookingStatus.PAID);
        bookingRepository.save(booking);
    }

    private Booking booking(int guestCount) {
        return Booking.create(golfTour, customer, TourLevel.CLASSIC, guestCount,
                HotelGrade.THREE_STAR, MealType.LUNCH_BOX, Transport.PREMIUM_VAN_10, 0, 0);
    }

    private String body(Long tourId, int guestCount) {
        return """
                {
                  "tourId": %d,
                  "tourLevel": "CLASSIC",
                  "guestCount": %d,
                  "hotelGrade": "THREE_STAR",
                  "mealType": "LUNCH_BOX",
                  "transport": "PREMIUM_VAN_10"
                }
                """.formatted(tourId, guestCount);
    }

    private String bearer() {
        return "Bearer " + jwtTokenProvider.createToken(customer.getMemberId(), Role.CUSTOMER);
    }

    private void cleanUp() {
        bookingRepository.deleteAll();
        tourRepository.deleteAll();
        inventoryItemRepository.deleteAll();
        drinkOptionRepository.deleteAll();
    }
}
