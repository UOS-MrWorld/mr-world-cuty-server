package com.mrworld.yaho.tour;

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
import com.mrworld.yaho.inventory.InventoryItem;
import com.mrworld.yaho.inventory.InventoryItemRepository;
import com.mrworld.yaho.member.Member;
import com.mrworld.yaho.member.MemberRepository;
import com.mrworld.yaho.payment.PaymentRepository;
import com.mrworld.yaho.security.JwtTokenProvider;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TourControllerStaffTest {

    private static final String URL = "/api/v1/staff/tours";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private InventoryItem scarf;
    private InventoryItem golfBall;
    private Tour golfTour;

    @BeforeEach
    void setUp() {
        cleanUp();
        scarf = inventoryItemRepository.save(InventoryItem.create("스카프", 50));
        golfBall = inventoryItemRepository.save(InventoryItem.create("골프공", 30));
        golfTour = saveTour("골프 챌린지 투어", LocalDate.now().plusDays(30), TourStatus.RECRUITING);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    void 고객_토큰이면_403이다() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", bearer(Role.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 현황_목록은_결제_완료_인원을_함께_보여주고_조건으로_거른다() throws Exception {
        Customer customer = saveCustomer("정지윤");
        saveBooking(golfTour, customer, 2, true);
        saveBooking(golfTour, customer, 1, true);
        saveBooking(golfTour, customer, 4, false);
        Tour cancelled = saveTour("취소된 투어", LocalDate.now().plusDays(60), TourStatus.CANCELLED);

        mockMvc.perform(get(URL).header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].tourId").value(golfTour.getTourId()))
                .andExpect(jsonPath("$.content[0].theme").value("GOLF"))
                .andExpect(jsonPath("$.content[0].tourStatus").value("RECRUITING"))
                .andExpect(jsonPath("$.content[0].paidGuestCount").value(3))
                .andExpect(jsonPath("$.content[1].tourId").value(cancelled.getTourId()))
                .andExpect(jsonPath("$.content[1].paidGuestCount").value(0));

        mockMvc.perform(get(URL).param("status", "CANCELLED").header("Authorization", bearer(Role.STAFF)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].tourId").value(cancelled.getTourId()));

        mockMvc.perform(get(URL)
                        .param("startDateFrom", LocalDate.now().plusDays(40).toString())
                        .param("startDateTo", LocalDate.now().plusDays(70).toString())
                        .header("Authorization", bearer(Role.STAFF)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].tourId").value(cancelled.getTourId()));
    }

    @Test
    void 현황_목록의_필터나_페이지_값이_잘못되면_400이다() throws Exception {
        mockMvc.perform(get(URL)
                        .param("startDateFrom", "2026-12-31")
                        .param("startDateTo", "2026-12-01")
                        .header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(URL).param("status", "DONE").header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(URL).param("size", "0").header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 상세_현황은_예약_현황과_기념품_재고를_보여준다() throws Exception {
        golfTour.addGift(golfBall, 2);
        golfTour = tourRepository.save(golfTour);
        Customer customer = saveCustomer("정지윤");
        Booking paid = saveBooking(golfTour, customer, 2, true);
        Booking pending = saveBooking(golfTour, customer, 1, false);

        mockMvc.perform(get(URL + "/" + golfTour.getTourId()).header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tour.tourId").value(golfTour.getTourId()))
                .andExpect(jsonPath("$.tour.tourName").value("골프 챌린지 투어"))
                .andExpect(jsonPath("$.tour.basePrice").value(500000))
                .andExpect(jsonPath("$.tour.tourStatus").value("RECRUITING"))
                .andExpect(jsonPath("$.paidGuestCount").value(2))
                .andExpect(jsonPath("$.bookings.length()").value(2))
                .andExpect(jsonPath("$.bookings[0].bookingId").value(paid.getBookingId()))
                .andExpect(jsonPath("$.bookings[0].customerId").value(customer.getMemberId()))
                .andExpect(jsonPath("$.bookings[0].name").value("정지윤"))
                .andExpect(jsonPath("$.bookings[0].guestCount").value(2))
                .andExpect(jsonPath("$.bookings[0].bookingStatus").value("PAID"))
                .andExpect(jsonPath("$.bookings[1].bookingId").value(pending.getBookingId()))
                .andExpect(jsonPath("$.bookings[1].bookingStatus").value("PENDING"))
                .andExpect(jsonPath("$.gifts[0].itemName").value("골프공"))
                .andExpect(jsonPath("$.gifts[0].quantityPerGuest").value(2))
                .andExpect(jsonPath("$.gifts[0].currentStock").value(30));

        mockMvc.perform(get(URL + "/999999").header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isNotFound());
    }

    @Test
    void 보낸_값만_수정하고_기념품_구성을_교체한다() throws Exception {
        golfTour.addGift(golfBall, 2);
        golfTour = tourRepository.save(golfTour);

        mockMvc.perform(patch(URL + "/" + golfTour.getTourId())
                        .header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tourName": "  골프 챌린지 스페셜  ",
                                  "basePrice": 480000,
                                  "gifts": [{"itemId": %d, "quantityPerGuest": 1}]
                                }
                                """.formatted(scarf.getItemId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tourId").value(golfTour.getTourId()))
                .andExpect(jsonPath("$.tourName").value("골프 챌린지 스페셜"))
                .andExpect(jsonPath("$.basePrice").value(480000))
                .andExpect(jsonPath("$.tourStatus").value("RECRUITING"));

        mockMvc.perform(get(URL + "/" + golfTour.getTourId()).header("Authorization", bearer(Role.STAFF)))
                .andExpect(jsonPath("$.tour.description").value("유명 골프 리조트"))
                .andExpect(jsonPath("$.tour.defaultTransport").value("PREMIUM_VAN_10"))
                .andExpect(jsonPath("$.gifts.length()").value(1))
                .andExpect(jsonPath("$.gifts[0].itemName").value("스카프"))
                .andExpect(jsonPath("$.gifts[0].quantityPerGuest").value(1));
    }

    @Test
    void 변경할_값이_없거나_기간_가격_기념품_수량이_잘못되면_400이다() throws Exception {
        String url = URL + "/" + golfTour.getTourId();

        mockMvc.perform(patch(url).header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        // 기존 시작일보다 이른 종료일
        mockMvc.perform(patch(url).header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endDate\": \"%s\"}".formatted(golfTour.getStartDate().minusDays(1))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch(url).header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"basePrice\": -1}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch(url).header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gifts\": [{\"itemId\": %d, \"quantityPerGuest\": 0}]}"
                                .formatted(scarf.getItemId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 모집_중이_아닌_투어는_409_없는_투어나_기념품은_404이다() throws Exception {
        Tour confirmed = saveTour("확정된 투어", LocalDate.now().plusDays(5), TourStatus.CONFIRMED);

        mockMvc.perform(patch(URL + "/" + confirmed.getTourId()).header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"basePrice\": 1}"))
                .andExpect(status().isConflict());

        mockMvc.perform(patch(URL + "/999999").header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"basePrice\": 1}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch(URL + "/" + golfTour.getTourId()).header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gifts\": [{\"itemId\": 999999, \"quantityPerGuest\": 1}]}"))
                .andExpect(status().isNotFound());
    }

    private Tour saveTour(String tourName, LocalDate startDate, TourStatus tourStatus) {
        Tour tour = Tour.create(Theme.GOLF, tourName, "유명 골프 리조트",
                startDate, startDate.plusDays(3), 500000, Transport.PREMIUM_VAN_10);
        ReflectionTestUtils.setField(tour, "tourStatus", tourStatus);
        return tourRepository.save(tour);
    }

    private Booking saveBooking(Tour tour, Customer customer, int guestCount, boolean paid) {
        Booking booking = Booking.create(tour, customer, TourLevel.CLASSIC, guestCount,
                HotelGrade.THREE_STAR, MealType.LUNCH_BOX, Transport.PREMIUM_VAN_10, 1_000_000, 0);
        if (paid) {
            booking.pay(0);
        }
        return bookingRepository.save(booking);
    }

    private Customer saveCustomer(String name) {
        Member member = memberRepository.save(Member.create(
                "staff-test-" + UUID.randomUUID(), "pw", name, "01012345678", "서울", Role.CUSTOMER));
        return customerRepository.save(Customer.create(member));
    }

    private String bearer(Role role) {
        return "Bearer " + jwtTokenProvider.createToken(1L, role);
    }

    private void cleanUp() {
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        tourRepository.deleteAll();
        inventoryItemRepository.deleteAll();
    }
}
