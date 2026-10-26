package com.mrworld.yaho.member;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 직원 회원. 직원 전용 속성은 없고 Member의 id를 그대로 식별자로 사용한다. (member.memberId를 참조하는 외래키)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Staff {

    @Id
    private Long memberId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "member_id")
    private Member member;

    public static Staff create(Member member) {
        Staff staff = new Staff();
        staff.member = member;
        return staff;
    }
}
