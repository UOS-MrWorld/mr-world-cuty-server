package com.mrworld.yaho.common.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

// 목록 API가 함께 쓰는 page/size 쿼리. 필터가 있는 목록은 이 클래스를 상속해 조건만 더한다.
// 정렬 기준은 화면마다 달라서 서비스가 정한다.
@Getter
@Setter
public class PagingRequest {
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    private int page = 0;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    private int size = 20;

    public Pageable toPageable(Sort sort) {
        return PageRequest.of(page, size, sort);
    }
}
