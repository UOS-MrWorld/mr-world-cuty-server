package com.mrworld.yaho.customer;

import com.mrworld.yaho.common.enums.Role;
import com.mrworld.yaho.member.Member;
import com.mrworld.yaho.member.MemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

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
    void Customer의_식별자는_회원의_memberId를_그대로_사용한다() {
        Member member = saveMember("customer-e");

        Customer saved = customerRepository.saveAndFlush(Customer.create(member));

        assertThat(saved.getMemberId()).isEqualTo(member.getMemberId());
    }

    @Test
    void 존재하지_않는_회원의_Customer_행은_외래키로_저장할_수_없다() {
        assertThatThrownBy(() -> entityManager.createNativeQuery(
                        "insert into customer (member_id, loyalty_grade) values (999999, 'NORMAL')")
                .executeUpdate())
                .isInstanceOf(PersistenceException.class);
    }

    private Member saveMember(String loginId) {
        return memberRepository.save(Member.create(loginId, "pw", "테스터", "010-0000-0000", "서울", Role.CUSTOMER));
    }
}
