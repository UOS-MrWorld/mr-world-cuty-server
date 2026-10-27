package com.mrworld.yaho.auth.dto;

import com.mrworld.yaho.common.enums.Role;
import com.mrworld.yaho.member.Member;
import lombok.Builder;
import lombok.Getter;

// 회원가입 결과 (비밀번호는 내려주지 않는다)
@Getter
@Builder
public class SignupResponse {
    private Long memberId;
    private String name;
    private String loginId;
    private String phoneNum;
    private String address;
    private Role role;

    public static SignupResponse from(Member member) {
        return SignupResponse.builder()
                .memberId(member.getMemberId())
                .name(member.getName())
                .loginId(member.getLoginId())
                .phoneNum(member.getPhoneNum())
                .address(member.getAddress())
                .role(member.getRole())
                .build();
    }
}
