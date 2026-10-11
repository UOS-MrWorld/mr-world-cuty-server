package com.mrworld.yaho.customer;

import com.mrworld.yaho.common.enums.LoyaltyGrade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void 등급이_같고_할인이_적용되지_않은_고객만_조회한다() {
        customerRepository.save(customer(1L, LoyaltyGrade.VIP, false));
        customerRepository.save(customer(2L, LoyaltyGrade.VIP, true));
        customerRepository.save(customer(3L, LoyaltyGrade.NORMAL, false));
        customerRepository.save(customer(4L, LoyaltyGrade.VIP, false));

        var result = customerRepository.findAllByLoyaltyGradeAndLoyaltyDiscountAppliedFalse(LoyaltyGrade.VIP);

        assertThat(result).extracting(Customer::getMemberId).containsExactlyInAnyOrder(1L, 4L);
    }

    private Customer customer(Long memberId, LoyaltyGrade grade, boolean discountApplied) {
        Customer customer = Customer.create(memberId);
        ReflectionTestUtils.setField(customer, "loyaltyGrade", grade);
        ReflectionTestUtils.setField(customer, "loyaltyDiscountApplied", discountApplied);
        return customer;
    }
}
