package com.mrworld.yaho.auth.dto;

import lombok.Builder;
import lombok.Getter;

// 토큰 재발급 결과
@Getter
@Builder
public class TokenResponse {
    private String accessToken;

    public static TokenResponse of(String accessToken) {
        return TokenResponse.builder().accessToken(accessToken).build();
    }
}
