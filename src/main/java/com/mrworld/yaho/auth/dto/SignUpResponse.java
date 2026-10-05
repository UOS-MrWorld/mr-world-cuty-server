package com.mrworld.yaho.auth.dto;

import com.mrworld.yaho.member.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignUpResponse {
    private long memberId;
    private String name;
    private String loginId;
    private String phoneNum;
    private String address;
    private Member.Role role;
}
