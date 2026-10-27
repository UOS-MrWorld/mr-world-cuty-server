package com.mrworld.yaho.booking;

import com.mrworld.yaho.common.enums.BookingStatus;
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
import com.mrworld.yaho.common.enums.PaymentStatus;
import com.mrworld.yaho.payment.Payment;
import com.mrworld.yaho.payment.PaymentRepository;
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
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    private PaymentRepository paymentRepository;

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

    @Test
    void 내_예약만_최근_순으로_조회하고_상태로_거른다() throws Exception {
        Booking first = bookingRepository.save(booking(1));
        savePaidBooking(2);
        bookingRepository.save(othersBooking());

        mockMvc.perform(get(URL).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].bookingStatus").value("PAID"))
                .andExpect(jsonPath("$.content[1].bookingId").value(first.getBookingId()))
                .andExpect(jsonPath("$.content[1].tourId").value(golfTour.getTourId()))
                .andExpect(jsonPath("$.content[1].tourName").value("골프 챌린지 투어"))
                .andExpect(jsonPath("$.content[1].startDate").value(golfTour.getStartDate().toString()));

        mockMvc.perform(get(URL).param("status", "PENDING").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].bookingStatus").value("PENDING"));
    }

    @Test
    void 목록_조회의_상태나_페이지_값이_잘못되면_400이다() throws Exception {
        mockMvc.perform(get(URL).param("status", "DONE").header("Authorization", bearer()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(URL).param("page", "-1").header("Authorization", bearer()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 예약_상세를_조회한다() throws Exception {
        Booking booking = booking(2);
        booking.addDrinkOption(coffee, 3);
        booking = bookingRepository.save(booking);

        mockMvc.perform(get(URL + "/" + booking.getBookingId()).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(booking.getBookingId()))
                .andExpect(jsonPath("$.tour.tourId").value(golfTour.getTourId()))
                .andExpect(jsonPath("$.tour.theme").value("GOLF"))
                .andExpect(jsonPath("$.tour.tourStatus").value("RECRUITING"))
                .andExpect(jsonPath("$.tourLevel").value("CLASSIC"))
                .andExpect(jsonPath("$.guestCount").value(2))
                .andExpect(jsonPath("$.drinkOptions[0].drinkName").value("COFFEE"))
                .andExpect(jsonPath("$.drinkOptions[0].quantity").value(3))
                .andExpect(jsonPath("$.drinkOptions[0].price").value(5000))
                .andExpect(jsonPath("$.bookingStatus").value("PENDING"))
                .andExpect(jsonPath("$.paymentStatus").doesNotExist());
    }

    @Test
    void 다른_고객의_예약은_403_없는_예약은_404이다() throws Exception {
        Booking othersBooking = bookingRepository.save(othersBooking());

        mockMvc.perform(get(URL + "/" + othersBooking.getBookingId()).header("Authorization", bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(patch(URL + "/" + othersBooking.getBookingId())
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"guestCount\": 2}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(URL + "/999999").header("Authorization", bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void 보낸_옵션만_바꾸고_금액을_다시_계산한다() throws Exception {
        Booking booking = booking(2);
        booking.addDrinkOption(coffee, 1);
        booking = bookingRepository.save(booking);

        // (500,000 + 호텔 4성 100,000 + 밴 50,000) * 3명 + 커피 5,000 * 1
        mockMvc.perform(patch(URL + "/" + booking.getBookingId())
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"guestCount\": 3, \"hotelGrade\": \"FOUR_STAR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingStatus").value("PENDING"))
                .andExpect(jsonPath("$.tourLevel").value("CLASSIC"))
                .andExpect(jsonPath("$.guestCount").value(3))
                .andExpect(jsonPath("$.hotelGrade").value("FOUR_STAR"))
                .andExpect(jsonPath("$.mealType").value("LUNCH_BOX"))
                .andExpect(jsonPath("$.totalPrice").value(1_955_000))
                .andExpect(jsonPath("$.expectedPaymentAmount").value(1_955_000));
    }

    @Test
    void 음료를_보내면_기존_음료_구성을_교체한다() throws Exception {
        Booking booking = booking(2);
        booking.addDrinkOption(coffee, 1);
        booking = bookingRepository.save(booking);
        DrinkOption wine = drinkOptionRepository.save(DrinkOption.create("WINE", 30000));

        // (500,000 + 밴 50,000) * 2명 + 와인 30,000 * 2
        mockMvc.perform(patch(URL + "/" + booking.getBookingId())
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"drinkOptions\": [{\"optionId\": %d, \"quantity\": 2}]}"
                                .formatted(wine.getOptionId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").value(1_160_000));

        mockMvc.perform(get(URL + "/" + booking.getBookingId()).header("Authorization", bearer()))
                .andExpect(jsonPath("$.drinkOptions.length()").value(1))
                .andExpect(jsonPath("$.drinkOptions[0].drinkName").value("WINE"));

        mockMvc.perform(patch(URL + "/" + booking.getBookingId())
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"drinkOptions\": []}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").value(1_100_000));
    }

    @Test
    void 변경할_값이_없거나_규칙에_맞지_않으면_400이다() throws Exception {
        Booking booking = bookingRepository.save(booking(2));

        mockMvc.perform(patch(URL + "/" + booking.getBookingId())
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        // 3명 이상은 2인 차량을 선택할 수 없다.
        mockMvc.perform(patch(URL + "/" + booking.getBookingId())
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"guestCount\": 3, \"transport\": \"PREMIUM_CAR_2\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 결제_완료된_예약은_수정할_수_없다() throws Exception {
        Booking booking = booking(2);
        ReflectionTestUtils.setField(booking, "bookingStatus", BookingStatus.PAID);
        booking = bookingRepository.save(booking);

        mockMvc.perform(patch(URL + "/" + booking.getBookingId())
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"guestCount\": 1}"))
                .andExpect(status().isConflict());
    }

    @Test
    void 인원을_늘려_기념품_재고가_부족하면_409이다() throws Exception {
        savePaidBooking(3);
        Booking booking = bookingRepository.save(booking(1));

        mockMvc.perform(patch(URL + "/" + booking.getBookingId())
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"guestCount\": 3}"))
                .andExpect(status().isConflict());

        mockMvc.perform(patch(URL + "/" + booking.getBookingId())
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"guestCount\": 2}"))
                .andExpect(status().isOk());
    }

    @Test
    void 결제_전_예약은_환불_없이_취소된다() throws Exception {
        Booking booking = bookingRepository.save(booking(2));

        mockMvc.perform(patch(URL + "/" + booking.getBookingId() + "/cancel").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(booking.getBookingId()))
                .andExpect(jsonPath("$.bookingStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.paymentStatus").doesNotExist());

        assertThat(bookingRepository.findById(booking.getBookingId()).orElseThrow().getBookingStatus())
                .isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void 결제_완료_예약을_취소하면_결제가_환불된다() throws Exception {
        Booking booking = booking(2);
        ReflectionTestUtils.setField(booking, "bookingStatus", BookingStatus.PAID);
        booking = bookingRepository.save(booking);
        Payment payment = paymentRepository.save(Payment.success(booking, 1_000_000, LocalDateTime.now()));

        mockMvc.perform(patch(URL + "/" + booking.getBookingId() + "/cancel").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.paymentStatus").value("REFUNDED"));

        assertThat(paymentRepository.findById(payment.getPaymentId()).orElseThrow().getPaymentStatus())
                .isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void 이미_취소했거나_취소_마감이_지난_예약은_400이다() throws Exception {
        Booking booking = bookingRepository.save(booking(2));
        mockMvc.perform(patch(URL + "/" + booking.getBookingId() + "/cancel").header("Authorization", bearer()))
                .andExpect(status().isOk());

        mockMvc.perform(patch(URL + "/" + booking.getBookingId() + "/cancel").header("Authorization", bearer()))
                .andExpect(status().isBadRequest());

        Booking closed = bookingRepository.save(booking(1));
        ReflectionTestUtils.setField(golfTour, "startDate", LocalDate.now().plusDays(6));
        ReflectionTestUtils.setField(golfTour, "endDate", LocalDate.now().plusDays(9));
        tourRepository.save(golfTour);

        mockMvc.perform(patch(URL + "/" + closed.getBookingId() + "/cancel").header("Authorization", bearer()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 다른_고객의_예약은_취소할_수_없다() throws Exception {
        Booking othersBooking = bookingRepository.save(othersBooking());

        mockMvc.perform(patch(URL + "/" + othersBooking.getBookingId() + "/cancel").header("Authorization", bearer()))
                .andExpect(status().isForbidden());
        assertThat(bookingRepository.findById(othersBooking.getBookingId()).orElseThrow().getBookingStatus())
                .isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void 여행_내역은_결제한_예약_중_취소되지_않은_투어를_최근_순으로_보여준다() throws Exception {
        Tour completedTour = saveTourWithStatus("완료된 투어", LocalDate.now().minusDays(30), TourStatus.COMPLETED);
        Tour cancelledTour = saveTourWithStatus("취소된 투어", LocalDate.now().plusDays(3), TourStatus.CANCELLED);
        Booking upcoming = savePaid(golfTour, 2, 10);
        Booking completed = savePaid(completedTour, 1, 0);
        savePaid(cancelledTour, 1, 0);
        bookingRepository.save(booking(1));
        Booking othersPaid = othersBooking();
        othersPaid.pay(0);
        bookingRepository.save(othersPaid);

        mockMvc.perform(get("/api/v1/customer/travel-history").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].bookingId").value(upcoming.getBookingId()))
                .andExpect(jsonPath("$.content[0].tourName").value("골프 챌린지 투어"))
                .andExpect(jsonPath("$.content[0].startDate").value(golfTour.getStartDate().toString()))
                .andExpect(jsonPath("$.content[0].endDate").value(golfTour.getEndDate().toString()))
                .andExpect(jsonPath("$.content[0].tourLevel").value("CLASSIC"))
                .andExpect(jsonPath("$.content[0].paidAmount").value(900_000))
                .andExpect(jsonPath("$.content[0].tourStatus").value("RECRUITING"))
                .andExpect(jsonPath("$.content[1].bookingId").value(completed.getBookingId()))
                .andExpect(jsonPath("$.content[1].tourStatus").value("COMPLETED"));
    }

    @Test
    void 여행_내역_페이지_값이_잘못되면_400_토큰이_없으면_401이다() throws Exception {
        mockMvc.perform(get("/api/v1/customer/travel-history").param("size", "0").header("Authorization", bearer()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/customer/travel-history"))
                .andExpect(status().isUnauthorized());
    }

    // 할인 전 1,000,000원을 discountRate로 결제한 예약
    private Booking savePaid(Tour tour, int guestCount, int discountRate) {
        Booking booking = Booking.create(tour, customer, TourLevel.CLASSIC, guestCount,
                HotelGrade.THREE_STAR, MealType.LUNCH_BOX, Transport.PREMIUM_VAN_10, 1_000_000, 0);
        booking.pay(discountRate);
        return bookingRepository.save(booking);
    }

    private Tour saveTourWithStatus(String tourName, LocalDate startDate, TourStatus tourStatus) {
        Tour tour = Tour.create(Theme.GOLF, tourName, null, startDate, startDate.plusDays(3),
                500000, Transport.PREMIUM_VAN_10);
        ReflectionTestUtils.setField(tour, "tourStatus", tourStatus);
        return tourRepository.save(tour);
    }

    private Booking othersBooking() {
        Member member = memberRepository.save(Member.create(
                "other-" + UUID.randomUUID(), "pw", "다른고객", "010-1111-1111", "부산", Role.CUSTOMER));
        Customer other = customerRepository.save(Customer.create(member));
        return Booking.create(golfTour, other, TourLevel.CLASSIC, 1,
                HotelGrade.THREE_STAR, MealType.LUNCH_BOX, Transport.PREMIUM_VAN_10, 0, 0);
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
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        tourRepository.deleteAll();
        inventoryItemRepository.deleteAll();
        drinkOptionRepository.deleteAll();
    }
}
