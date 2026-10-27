package com.mrworld.yaho.member;

import com.mrworld.yaho.common.enums.Role;
import com.mrworld.yaho.common.exception.BadRequestException;
import com.mrworld.yaho.common.exception.NotFoundException;
import com.mrworld.yaho.member.dto.MemberInfoResponse;
import com.mrworld.yaho.member.dto.MemberUpdateRequest;
import com.mrworld.yaho.member.dto.MemberUpdateResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    @Test
    void 내_회원정보를_조회한다() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member()));

        MemberInfoResponse response = memberService.getMyInfo(1L);

        assertThat(response.getMemberId()).isEqualTo(1L);
        assertThat(response.getLoginId()).isEqualTo("jiyoon01");
        assertThat(response.getPhoneNum()).isEqualTo("01012345678");
        assertThat(response.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void 존재하지_않는_회원은_예외가_발생한다() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMyInfo(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void 보낸_값만_수정한다() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member()));

        MemberUpdateResponse response = memberService.updateMyInfo(1L, request("01098765432", null));

        assertThat(response.getPhoneNum()).isEqualTo("01098765432");
        assertThat(response.getAddress()).isEqualTo("서울특별시 동대문구");
    }

    @Test
    void 수정값이_없으면_예외가_발생한다() {
        assertThatThrownBy(() -> memberService.updateMyInfo(1L, request(null, null)))
                .isInstanceOf(BadRequestException.class);
        verify(memberRepository, never()).findById(any());
    }

    private MemberUpdateRequest request(String phoneNum, String address) {
        MemberUpdateRequest request = new MemberUpdateRequest();
        request.setPhoneNum(phoneNum);
        request.setAddress(address);
        return request;
    }

    private Member member() {
        Member member = Member.create("jiyoon01", "encoded", "정지윤", "01012345678", "서울특별시 동대문구",
                Role.CUSTOMER);
        ReflectionTestUtils.setField(member, "memberId", 1L);
        return member;
    }
}
