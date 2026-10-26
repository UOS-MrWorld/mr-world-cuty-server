package com.mrworld.yaho.member;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 직원 회원. 직원 전용 속성은 없고 Member의 id를 그대로 식별자로 사용한다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Staff {

    @Id
    private Long memberId;

    public static Staff create(Long memberId) {
        Staff staff = new Staff();
        staff.memberId = memberId;
        return staff;
    }
}
