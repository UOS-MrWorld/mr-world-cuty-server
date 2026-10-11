package com.mrworld.yaho.auth.dto;

import com.mrworld.yaho.common.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private long memberId;

    private String memberName;

    private Role role;

    private String accessToken;

    private String refreshToken;

    private String tokenType;
}
