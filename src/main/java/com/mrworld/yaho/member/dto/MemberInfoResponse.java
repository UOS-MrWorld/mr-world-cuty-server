package com.mrworld.yaho.member.dto;

import com.mrworld.yaho.common.enums.Role;
import com.mrworld.yaho.member.Member;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 로그인 사용자의 공통 회원정보 (비밀번호는 내려주지 않는다)
@Getter
@Builder
public class MemberInfoResponse {
    private Long memberId;
    private String loginId;
    private String name;
    private String phoneNum;
    private String address;
    private Role role;
    private LocalDateTime createdAt;

    public static MemberInfoResponse from(Member member) {
        return MemberInfoResponse.builder()
                .memberId(member.getMemberId())
                .loginId(member.getLoginId())
                .name(member.getName())
                .phoneNum(member.getPhoneNum())
                .address(member.getAddress())
                .role(member.getRole())
                .createdAt(member.getCreatedAt())
                .build();
    }
}
