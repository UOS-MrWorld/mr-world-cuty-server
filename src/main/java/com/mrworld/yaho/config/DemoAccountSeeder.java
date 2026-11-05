package com.mrworld.yaho.config;

import com.mrworld.yaho.customer.Customer;
import com.mrworld.yaho.customer.CustomerRepository;
import com.mrworld.yaho.member.Member;
import com.mrworld.yaho.member.MemberRepository;
import com.mrworld.yaho.member.Staff;
import com.mrworld.yaho.member.StaffRepository;
import com.mrworld.yaho.common.enums.Role;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoAccountSeeder {

    @Bean
    CommandLineRunner seedDemoAccounts(
            MemberRepository memberRepository,
            CustomerRepository customerRepository,
            StaffRepository staffRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (!memberRepository.existsByLoginId("customer")) {
                Member member = memberRepository.save(Member.create(
                        "customer", passwordEncoder.encode("customer"), "시연 고객",
                        "010-0000-0000", "서울시립대학교", Role.CUSTOMER));
                customerRepository.save(Customer.create(member));
            }
            if (!memberRepository.existsByLoginId("staff")) {
                Member member = memberRepository.save(Member.create(
                        "staff", passwordEncoder.encode("staffstaff"), "시연 직원",
                        "010-0000-0001", "서울시립대학교", Role.STAFF));
                staffRepository.save(Staff.create(member));
            }
        };
    }
}
