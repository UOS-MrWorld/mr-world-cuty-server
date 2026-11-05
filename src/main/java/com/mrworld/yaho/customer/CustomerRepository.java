package com.mrworld.yaho.customer;

import com.mrworld.yaho.common.enums.LoyaltyGrade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // 직원용 고객 목록. 이름·로그인 아이디 검색어와 등급으로 거르며, null인 조건은 무시한다.
    @EntityGraph(attributePaths = "member")
    @Query("""
            select c from Customer c join c.member m
            where (:keyword is null
                   or lower(m.name) like lower(concat('%', :keyword, '%'))
                   or lower(m.loginId) like lower(concat('%', :keyword, '%')))
              and (:loyaltyGrade is null or c.loyaltyGrade = :loyaltyGrade)
            """)
    Page<Customer> search(
            @Param("keyword") String keyword,
            @Param("loyaltyGrade") LoyaltyGrade loyaltyGrade,
            Pageable pageable
    );
}
