package com.mrworld.yaho.auth.dto;

import com.mrworld.yaho.member.Member;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignUpRequest {
    @NotBlank(message = "사용자 이름은 필수 항목입니다.")
    private String name;

    @NotBlank(message = "사용자 ID는 필수 항목입니다.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수 항목입니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수 항목입니다.")
    private String confirmPassword;

    @NotBlank(message = "전화번호는 필수 항목입니다.")
    private String phoneNum;

    @NotBlank(message = "주소는 필수 항목입니다.")
    private String address;

    @NotNull(message = "사용자 역할은 필수 항목입니다.")
    private Member.Role role;
}
