package com.mrworld.yaho.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "사용자 ID는 필수 항목입니다.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수 항목입니다.")
    private String password;
}
