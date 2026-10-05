package com.mrworld.yaho.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "refresh token은 필수 항목입니다.") String refreshToken
) {
}
