package com.mrworld.yaho.auth.dto;

import com.mrworld.yaho.common.enums.Role;
import com.mrworld.yaho.member.Member;
import lombok.Builder;
import lombok.Getter;

// 로그인 결과. tokenType은 항상 "Bearer"다.
@Getter
@Builder
public class LoginResponse {
    private static final String TOKEN_TYPE = "Bearer";

    private Long memberId;
    private String memberName;
    private Role role;
    private String accessToken;
    private String refreshToken;
    private String tokenType;

    public static LoginResponse of(Member member, String accessToken, String refreshToken) {
        return LoginResponse.builder()
                .memberId(member.getMemberId())
                .memberName(member.getName())
                .role(member.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(TOKEN_TYPE)
                .build();
    }
}
