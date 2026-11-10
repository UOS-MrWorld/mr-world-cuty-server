package com.mrworld.yaho.customer;

import com.mrworld.yaho.common.enums.LoyaltyGrade;
import com.mrworld.yaho.common.enums.Role;
import com.mrworld.yaho.member.Member;
import com.mrworld.yaho.member.MemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 등급이_같고_할인이_적용되지_않은_고객만_조회한다() {
        Member first = saveMember("customer-a");
        Member applied = saveMember("customer-b");
        Member normal = saveMember("customer-c");
        Member last = saveMember("customer-d");

        customerRepository.save(customer(first, LoyaltyGrade.VIP, false));
        customerRepository.save(customer(applied, LoyaltyGrade.VIP, true));
        customerRepository.save(customer(normal, LoyaltyGrade.NORMAL, false));
        customerRepository.save(customer(last, LoyaltyGrade.VIP, false));

        var result = customerRepository.findAllByLoyaltyGradeAndLoyaltyDiscountAppliedFalse(LoyaltyGrade.VIP);

        assertThat(result).extracting(Customer::getMemberId)
                .containsExactlyInAnyOrder(first.getMemberId(), last.getMemberId());
    }

    @Test
    void Customer의_식별자는_회원의_memberId를_그대로_사용한다() {
        Member member = saveMember("customer-e");

        Customer saved = customerRepository.saveAndFlush(Customer.create(member));

        assertThat(saved.getMemberId()).isEqualTo(member.getMemberId());
    }

    @Test
    void 존재하지_않는_회원의_Customer_행은_외래키로_저장할_수_없다() {
        assertThatThrownBy(() -> entityManager.createNativeQuery(
                        "insert into customer (member_id, loyalty_grade, loyalty_discount_applied) "
                                + "values (999999, 'NORMAL', false)")
                .executeUpdate())
                .isInstanceOf(PersistenceException.class);
    }

    private Member saveMember(String loginId) {
        return memberRepository.save(Member.create(loginId, "pw", "테스터", "010-0000-0000", "서울", Role.CUSTOMER));
    }

    private Customer customer(Member member, LoyaltyGrade grade, boolean discountApplied) {
        Customer customer = Customer.create(member);
        ReflectionTestUtils.setField(customer, "loyaltyGrade", grade);
        ReflectionTestUtils.setField(customer, "loyaltyDiscountApplied", discountApplied);
        return customer;
    }
}
