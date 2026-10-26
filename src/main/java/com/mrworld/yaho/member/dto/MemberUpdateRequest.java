package com.mrworld.yaho.member.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// 변경할 값만 보낸다. 둘 다 비어 있으면 서비스에서 400으로 처리한다.
@Getter
@Setter
public class MemberUpdateRequest {
    @Pattern(regexp = "^\\d{10,11}$", message = "전화번호는 숫자 10~11자리여야 합니다.")
    private String phoneNum;

    @Pattern(regexp = ".*\\S.*", message = "주소는 공백일 수 없습니다.")
    @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
    private String address;
}
