package com.mrworld.yaho.customer;

import com.mrworld.yaho.common.enums.LoyaltyGrade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // 해당 등급이면서 단골 할인이 아직 적용되지 않은 고객을 조회한다.
    List<Customer> findAllByLoyaltyGradeAndLoyaltyDiscountAppliedFalse(LoyaltyGrade loyaltyGrade);

    long countByLoyaltyGrade(LoyaltyGrade loyaltyGrade);

    // 직원용 고객 목록. 이름·로그인 아이디 검색어와 등급·할인 적용 여부로 거르며, null인 조건은 무시한다.
    @EntityGraph(attributePaths = "member")
    @Query("""
            select c from Customer c join c.member m
            where (:keyword is null
                   or lower(m.name) like lower(concat('%', :keyword, '%'))
                   or lower(m.loginId) like lower(concat('%', :keyword, '%')))
              and (:loyaltyGrade is null or c.loyaltyGrade = :loyaltyGrade)
              and (:discountApplied is null or c.loyaltyDiscountApplied = :discountApplied)
            """)
    Page<Customer> search(
            @Param("keyword") String keyword,
            @Param("loyaltyGrade") LoyaltyGrade loyaltyGrade,
            @Param("discountApplied") Boolean discountApplied,
            Pageable pageable
    );
}
