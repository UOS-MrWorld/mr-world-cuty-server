package com.mrworld.yaho.member.dto;

import com.mrworld.yaho.member.Member;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberUpdateResponse {
    private Long memberId;
    private String phoneNum;
    private String address;

    public static MemberUpdateResponse from(Member member) {
        return MemberUpdateResponse.builder()
                .memberId(member.getMemberId())
                .phoneNum(member.getPhoneNum())
                .address(member.getAddress())
                .build();
    }
}
