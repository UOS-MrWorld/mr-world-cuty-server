package com.mrworld.yaho.customer.dto;

import com.mrworld.yaho.common.enums.LoyaltyGrade;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

// 직원용 고객 목록 조건. 모두 선택값이며 여러 조건은 AND로 적용한다.
@Getter
@Setter
public class CustomerListRequest {
    // 이름 또는 로그인 아이디 검색어
    private String keyword;

    private LoyaltyGrade loyaltyGrade;

    private Boolean discountApplied;

    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    private int page = 0;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    private int size = 20;

    public Pageable toPageable() {
        return PageRequest.of(page, size, Sort.by("memberId"));
    }
}
