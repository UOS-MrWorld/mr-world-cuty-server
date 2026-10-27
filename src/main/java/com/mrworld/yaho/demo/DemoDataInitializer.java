package com.mrworld.yaho.demo;

import com.mrworld.yaho.booking.Booking;
import com.mrworld.yaho.booking.BookingRepository;
import com.mrworld.yaho.common.enums.HotelGrade;
import com.mrworld.yaho.common.enums.LoyaltyGrade;
import com.mrworld.yaho.common.enums.MealType;
import com.mrworld.yaho.common.enums.Role;
import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourLevel;
import com.mrworld.yaho.common.enums.Transport;
import com.mrworld.yaho.customer.Customer;
import com.mrworld.yaho.customer.CustomerRepository;
import com.mrworld.yaho.member.Member;
import com.mrworld.yaho.member.MemberRepository;
import com.mrworld.yaho.tour.Tour;
import com.mrworld.yaho.tour.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

// 데모 전용. demo 프로필로 실행할 때만, 완료 여행 5번을 채운 VIP 고객(할인 미적용)을 만든다.
// 비밀번호는 환경변수 DEMO_PASSWORD로 받는다. 이미 있으면 다시 만들지 않는다.
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

    private static final String VIP_LOGIN_ID = "demo-vip";

    private final MemberRepository memberRepository;
    private final CustomerRepository customerRepository;
    private final TourRepository tourRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${demo.password}")
    private String password;

    @Value("${demo.phone-num:01000000000}")
    private String phoneNum;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (memberRepository.existsByLoginId(VIP_LOGIN_ID)) {
            return;
        }

        Member member = memberRepository.save(Member.create(
                VIP_LOGIN_ID, passwordEncoder.encode(password), "데모단골", phoneNum, "서울", Role.CUSTOMER));
        Customer customer = customerRepository.save(Customer.create(member));

        int completedCount = LoyaltyGrade.VIP.getRequiredTourCount();
        for (int i = 1; i <= completedCount; i++) {
            LocalDate startDate = LocalDate.now().minusMonths(i);
            Tour tour = Tour.create(Theme.GOLF, "[데모] 지난 여행 " + i, null,
                    startDate, startDate.plusDays(3), 1_000_000, Transport.PREMIUM_VAN_10);
            tour.confirm();
            tour.complete();
            tourRepository.save(tour);

            Booking booking = Booking.create(tour, customer, TourLevel.CLASSIC, 1,
                    HotelGrade.THREE_STAR, MealType.LUNCH_BOX, Transport.PREMIUM_VAN_10, 1_000_000, 0);
            booking.pay(0);
            bookingRepository.save(booking);
        }
        customer.updateLoyaltyGrade(completedCount);
    }
}
