package com.mrworld.yaho.auth.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 토큰 재발급 결과
@Getter
@Builder
public class TokenResponse {
    private String accessToken;
    private LocalDateTime accessTokenExpiresAt;

    public static TokenResponse of(String accessToken, LocalDateTime accessTokenExpiresAt) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .accessTokenExpiresAt(accessTokenExpiresAt)
                .build();
    }
}
