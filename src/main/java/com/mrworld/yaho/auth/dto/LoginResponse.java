package com.mrworld.yaho.auth.dto;

import com.mrworld.yaho.member.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private long memberId;

    private String memberName;

    private Member.Role role;

    private String accessToken;

    private String refreshToken;

    private String tokenType;
}
