package com.mrworld.yaho.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// 토큰 재발급·로그아웃 요청
@Getter
@Setter
public class RefreshTokenRequest {
    @NotBlank(message = "refresh token은 필수 항목입니다.")
    private String refreshToken;
}
