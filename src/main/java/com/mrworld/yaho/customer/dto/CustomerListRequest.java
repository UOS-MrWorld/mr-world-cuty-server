package com.mrworld.yaho.customer.dto;

import com.mrworld.yaho.common.dto.PagingRequest;
import com.mrworld.yaho.common.enums.LoyaltyGrade;
import lombok.Getter;
import lombok.Setter;

// 직원용 고객 목록 조건. 모두 선택값이며 여러 조건은 AND로 적용한다.
@Getter
@Setter
public class CustomerListRequest extends PagingRequest {
    // 이름 또는 로그인 아이디 검색어
    private String keyword;

    private LoyaltyGrade loyaltyGrade;

    private Boolean discountApplied;
}
