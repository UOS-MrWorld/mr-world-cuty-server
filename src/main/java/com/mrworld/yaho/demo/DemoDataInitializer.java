package com.mrworld.yaho.demo;

import com.mrworld.yaho.booking.Booking;
import com.mrworld.yaho.booking.BookingRepository;
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
import com.mrworld.yaho.member.Staff;
import com.mrworld.yaho.member.StaffRepository;
import com.mrworld.yaho.payment.Payment;
import com.mrworld.yaho.payment.PaymentRepository;
import com.mrworld.yaho.tour.DrinkOption;
import com.mrworld.yaho.tour.DrinkOptionInitializer;
import com.mrworld.yaho.tour.DrinkOptionRepository;
import com.mrworld.yaho.tour.Tour;
import com.mrworld.yaho.tour.TourRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// 데모 전용. demo 프로필로 실행할 때만 발표에 쓸 회원·투어·예약·결제·재고를 만든다.
// 계정은 고객 customer / customer (추가 고객 customer2~5, 비밀번호 동일), 직원 staff / staffstaff 이다.
// customer2가 있으면 이미 넣은 것으로 보고 다시 만들지 않는다. customer·staff 계정이 먼저 있으면 그 계정을 그대로 쓴다.
// 날짜는 실행일 기준이고, 모집 중 투어는 발표 전에 마감되지 않도록 멀리 잡는다.
//
// 발표 시나리오
// - 단골 할인: customer는 완료 5회 VIP이고 할인 미적용. 직원이 VIP 일괄 적용하면 이후 결제부터 10% 할인된다.
// - 확정 문자: 속초는 결제 2명. customer의 결제 전 예약 1명을 결제한 뒤 마감하면 3명이 되어 확정된다.
// - 미달 취소: 거제는 결제 2명뿐이라 마감하면 취소·환불된다.
// - 등급 상승: customer3은 완료 4회. 평창을 완료 처리하면 5회가 되어 VIP가 된다.
@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

    private static final String CUSTOMER_LOGIN_ID = "customer";
    private static final String SEEDED_MARKER_LOGIN_ID = "customer2";
    private static final String STAFF_LOGIN_ID = "staff";
    private static final String STAFF_PASSWORD = "staffstaff";

    private final MemberRepository memberRepository;
    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final TourRepository tourRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final DrinkOptionRepository drinkOptionRepository;
    private final DrinkOptionInitializer drinkOptionInitializer;
    private final PasswordEncoder passwordEncoder;

    // 확정 문자가 실제로 나갈 수 있어, 모든 데모 회원의 번호를 이 값 하나로 둔다.
    @Value("${demo.phone-num:01000000000}")
    private String phoneNum;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (memberRepository.existsByLoginId(SEEDED_MARKER_LOGIN_ID)) {
            log.info("데모 데이터가 이미 있어 다시 만들지 않습니다.");
            return;
        }

        // 러너 실행 순서는 정해져 있지 않아, 음료 옵션을 쓰기 전에 먼저 채워 둔다.
        drinkOptionInitializer.run(args);
        List<DrinkOption> drinkOptions = drinkOptionRepository.findAll();
        DrinkOption coffee = drinkOptions.get(0);
        DrinkOption champagne = drinkOptions.get(1);

        createStaffIfAbsent();
        Customer customer = findOrCreateCustomer(CUSTOMER_LOGIN_ID, "김민준", "서울특별시 동대문구 서울시립대로 163");
        Customer customer2 = findOrCreateCustomer(SEEDED_MARKER_LOGIN_ID, "이서연", "서울특별시 강남구 테헤란로 152");
        Customer customer3 = findOrCreateCustomer("customer3", "박지호", "부산광역시 해운대구 센텀중앙로 79");
        Customer customer4 = findOrCreateCustomer("customer4", "최수아", "대전광역시 유성구 대학로 99");
        findOrCreateCustomer("customer5", "정우진", "제주특별자치도 제주시 첨단로 242");

        InventoryItem pouch = findOrCreateItem("여행용 파우치", 200);
        InventoryItem golfBall = findOrCreateItem("골프공 세트", 80);
        InventoryItem socks = findOrCreateItem("트레킹 양말", 120);
        InventoryItem aromaOil = findOrCreateItem("아로마 오일 세트", 40);
        InventoryItem mug = findOrCreateItem("커플 머그컵", 5);

        LocalDate today = LocalDate.now();

        // 지난 여행
        Tour namhae = createTour(Theme.GOLF, "남해 골프 3박 4일", "남해 바다를 끼고 도는 명문 코스 3라운드",
                today.minusDays(170), 3, 790_000, Transport.PREMIUM_VAN_10, pouch, golfBall);
        Tour jeju = createTour(Theme.TREKKING, "제주 올레길 트레킹 2박 3일", "올레 7코스와 한라산 영실 코스를 걷는 일정",
                today.minusDays(140), 2, 450_000, Transport.PREMIUM_VAN_10, pouch, socks);
        Tour damyang = createTour(Theme.HEALING, "담양 힐링 4박 5일", "죽녹원 대숲 산책과 스파로 쉬는 일정",
                today.minusDays(105), 4, 690_000, Transport.PREMIUM_VAN_10, pouch, aromaOil);
        Tour seorak = createTour(Theme.TREKKING, "설악산 트레킹 4박 5일", "대청봉 능선과 척산온천을 함께 즐기는 일정",
                today.minusDays(70), 4, 590_000, Transport.PREMIUM_VAN_10, pouch, socks);
        Tour jejuHoneymoon = createTour(Theme.HONEYMOON, "제주 허니문 5박 6일", "오션뷰 풀빌라에서 보내는 둘만의 시간",
                today.minusDays(35), 5, 1_190_000, Transport.PREMIUM_CAR_2, pouch, mug);
        for (Tour tour : List.of(namhae, jeju, damyang, seorak, jejuHoneymoon)) {
            tour.confirm();
            tour.complete();
        }
        Tour yeosu = createTour(Theme.GOLF, "여수 골프 3박 4일", "인원 미달로 취소된 일정",
                today.minusDays(20), 3, 690_000, Transport.PREMIUM_VAN_10, pouch, golfBall);
        yeosu.cancel();

        pay(book(namhae, customer, TourLevel.GRAND, 2));
        pay(book(jeju, customer, TourLevel.CLASSIC, 2));
        pay(book(damyang, customer, TourLevel.PREMIUM, 2, champagne, 2));
        pay(book(seorak, customer, TourLevel.GRAND, 2));
        pay(book(jejuHoneymoon, customer, TourLevel.PREMIUM, 2, champagne, 1));

        pay(book(namhae, customer2, TourLevel.PREMIUM, 2));
        pay(book(jeju, customer2, TourLevel.GRAND, 4, coffee, 4));
        pay(book(damyang, customer2, TourLevel.GRAND, 2));
        pay(book(seorak, customer2, TourLevel.PREMIUM, 3));
        pay(book(jejuHoneymoon, customer2, TourLevel.GRAND, 2));

        pay(book(namhae, customer3, TourLevel.CLASSIC, 1));
        pay(book(jeju, customer3, TourLevel.CLASSIC, 1));
        pay(book(damyang, customer3, TourLevel.GRAND, 1));
        pay(book(seorak, customer3, TourLevel.CLASSIC, 1));

        pay(book(jeju, customer4, TourLevel.GRAND, 2));
        refund(book(yeosu, customer4, TourLevel.CLASSIC, 2));

        updateLoyaltyGrade(customer);
        updateLoyaltyGrade(customer2);
        updateLoyaltyGrade(customer3);
        updateLoyaltyGrade(customer4);
        customer2.activateDiscount(LocalDateTime.now().minusDays(7));

        // 확정된 투어
        Tour pyeongchang = createTour(Theme.GOLF, "평창 골프 4박 5일", "대관령 고원의 두 코스를 도는 일정",
                today.plusDays(45), 4, 890_000, Transport.PREMIUM_VAN_10, pouch, golfBall);
        pyeongchang.confirm();
        pay(book(pyeongchang, customer, TourLevel.GRAND, 2));
        pay(book(pyeongchang, customer2, TourLevel.PREMIUM, 2, champagne, 1));
        pay(book(pyeongchang, customer3, TourLevel.CLASSIC, 1));

        // 모집 중인 투어
        Tour sokcho = createTour(Theme.HEALING, "속초 힐링 리조트 4박 5일", "온천 스파와 오션뷰 객실에서 쉬어 가는 일정",
                today.plusDays(60), 4, 740_000, Transport.PREMIUM_VAN_10, pouch, aromaOil);
        pay(book(sokcho, customer2, TourLevel.PREMIUM, 2, coffee, 2));
        book(sokcho, customer, TourLevel.GRAND, 1);

        Tour geoje = createTour(Theme.HONEYMOON, "거제 허니문 5박 6일", "외도 보타니아와 요트 투어를 즐기는 일정",
                today.plusDays(75), 5, 1_090_000, Transport.PREMIUM_CAR_2, pouch, mug);
        pay(book(geoje, customer4, TourLevel.GRAND, 2));

        Tour gijang = createTour(Theme.GOLF, "부산 기장 골프 3박 4일", "해안 절벽을 끼고 도는 라운드",
                today.plusDays(90), 3, 750_000, Transport.PREMIUM_VAN_10, pouch, golfBall);
        refund(book(gijang, customer3, TourLevel.CLASSIC, 2));

        createTour(Theme.TREKKING, "백두대간 종주 트레킹 9박 10일", "지리산 천왕봉부터 설악산 대청봉까지 주요 구간을 걷는 일정",
                today.plusDays(105), 9, 1_390_000, Transport.PREMIUM_VAN_10, pouch, socks);
        createTour(Theme.HONEYMOON, "울릉도 허니문 6박 7일", "성인봉과 해안 일주 도로를 함께 둘러보는 일정",
                today.plusDays(120), 6, 1_290_000, Transport.PREMIUM_CAR_2, pouch, mug);
        createTour(Theme.HEALING, "전주 한옥 힐링 3박 4일", "한옥 스테이와 전통 찻집에서 머무는 일정",
                today.plusDays(135), 3, 590_000, Transport.PREMIUM_VAN_10, pouch, aromaOil);
    }

    private void createStaffIfAbsent() {
        if (memberRepository.existsByLoginId(STAFF_LOGIN_ID)) {
            return;
        }
        Member member = memberRepository.save(Member.create(
                STAFF_LOGIN_ID, passwordEncoder.encode(STAFF_PASSWORD), "한직원", phoneNum,
                "서울특별시 중구 세종대로 110", Role.STAFF));
        staffRepository.save(Staff.create(member));
    }

    private Customer findOrCreateCustomer(String loginId, String name, String address) {
        return memberRepository.findByLoginId(loginId)
                .map(member -> customerRepository.findById(member.getMemberId()).orElseThrow())
                .orElseGet(() -> {
                    Member member = memberRepository.save(Member.create(
                            loginId, passwordEncoder.encode(CUSTOMER_LOGIN_ID), name, phoneNum, address, Role.CUSTOMER));
                    return customerRepository.save(Customer.create(member));
                });
    }

    // 물품명은 유일해야 하므로, 직원이 이미 등록한 물품은 그대로 쓴다.
    private InventoryItem findOrCreateItem(String itemName, int quantity) {
        return inventoryItemRepository.findAll().stream()
                .filter(item -> item.getItemName().equals(itemName))
                .findFirst()
                .orElseGet(() -> inventoryItemRepository.save(InventoryItem.create(itemName, quantity)));
    }

    private Tour createTour(Theme theme, String tourName, String description, LocalDate startDate, int nights,
                            int price, Transport defaultTransport, InventoryItem... gifts) {
        Tour tour = Tour.create(theme, tourName, description, startDate, startDate.plusDays(nights),
                price, defaultTransport);
        for (InventoryItem gift : gifts) {
            tour.addGift(gift, 1);
        }
        return tourRepository.save(tour);
    }

    private Booking book(Tour tour, Customer customer, TourLevel tourLevel, int guestCount) {
        return book(tour, customer, tourLevel, guestCount, null, 0);
    }

    // 호텔·식사는 투어 등급의 기본 구성을 쓰고, 금액은 BookingService와 같은 식으로 계산한다.
    private Booking book(Tour tour, Customer customer, TourLevel tourLevel, int guestCount,
                         DrinkOption drinkOption, int drinkQuantity) {
        HotelGrade hotelGrade = switch (tourLevel) {
            case CLASSIC -> HotelGrade.THREE_STAR;
            case GRAND -> HotelGrade.FOUR_STAR;
            case PREMIUM -> HotelGrade.FIVE_STAR;
        };
        MealType mealType = switch (tourLevel) {
            case CLASSIC -> MealType.LUNCH_BOX;
            case GRAND -> MealType.LOCAL_RESTAURANT;
            case PREMIUM -> MealType.FINE_DINING;
        };
        Transport transport = tour.getDefaultTransport();

        int perGuestPrice = tour.getPrice() + hotelGrade.getPrice() + mealType.getPrice() + transport.getPrice();
        int drinkPrice = drinkOption != null ? drinkOption.getDrinkPrice() * drinkQuantity : 0;

        Booking booking = Booking.create(tour, customer, tourLevel, guestCount, hotelGrade, mealType, transport,
                perGuestPrice * guestCount + drinkPrice, customer.currentDiscountRate());
        if (drinkOption != null) {
            booking.addDrinkOption(drinkOption, drinkQuantity);
        }
        return bookingRepository.save(booking);
    }

    private Payment pay(Booking booking) {
        booking.pay(booking.getCustomer().currentDiscountRate());

        // 출발 30일 전에 결제한 것으로 두되, 미래 시각은 되지 않게 한다.
        LocalDate paidDate = booking.getTour().getStartDate().minusDays(30);
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime paidAt = (paidDate.isAfter(yesterday) ? yesterday : paidDate).atTime(10, 0);

        return paymentRepository.save(Payment.success(booking, booking.expectedPaymentAmount(), paidAt));
    }

    private void refund(Booking booking) {
        pay(booking).refund();
        booking.cancel();
    }

    private void updateLoyaltyGrade(Customer customer) {
        // 등급 계산 쿼리 전에 투어·예약 상태 변경을 DB에 내보낸다.
        bookingRepository.flush();
        customer.updateLoyaltyGrade(bookingRepository.countCompletedToursByCustomerId(customer.getMemberId()));
    }
}
