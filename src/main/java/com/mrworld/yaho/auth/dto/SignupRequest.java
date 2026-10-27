package com.mrworld.yaho.auth.dto;

import com.mrworld.yaho.common.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
    @NotBlank(message = "사용자 이름은 필수 항목입니다.")
    private String name;

    @NotBlank(message = "사용자 ID는 필수 항목입니다.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수 항목입니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수 항목입니다.")
    private String confirmPassword;

    // 회원정보 수정(MemberUpdateRequest)과 같은 형식: 하이픈 없는 숫자 10~11자리
    @NotBlank(message = "전화번호는 필수 항목입니다.")
    @Pattern(regexp = "^\\d{10,11}$", message = "전화번호는 숫자 10~11자리여야 합니다.")
    private String phoneNum;

    @NotBlank(message = "주소는 필수 항목입니다.")
    private String address;

    @NotNull(message = "사용자 역할은 필수 항목입니다.")
    private Role role;
}
