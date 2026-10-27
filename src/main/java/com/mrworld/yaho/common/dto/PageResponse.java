package com.mrworld.yaho.common.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

// 목록 API가 함께 쓰는 페이지 응답. content는 현재 페이지 항목, totalElements는 조건에 맞는 전체 개수다.
@Getter
@Builder
public class PageResponse<T> {
    private List<T> content;
    private long totalElements;

    // 조회한 엔티티 페이지를 응답 항목으로 바꿔 감싼다.
    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return PageResponse.<T>builder()
                .content(page.getContent().stream().map(mapper).toList())
                .totalElements(page.getTotalElements())
                .build();
    }
}
