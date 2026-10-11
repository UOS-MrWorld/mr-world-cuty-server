package com.mrworld.yaho.customer;

import com.mrworld.yaho.common.enums.LoyaltyGrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // 해당 등급이면서 단골 할인이 아직 적용되지 않은 고객을 조회한다.
    List<Customer> findAllByLoyaltyGradeAndLoyaltyDiscountAppliedFalse(LoyaltyGrade loyaltyGrade);
}
