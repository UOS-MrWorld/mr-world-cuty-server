package com.mrworld.yaho.inventory.dto;

import com.mrworld.yaho.common.dto.PagingRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryListRequest extends PagingRequest {
    // 물품명 검색어. 비어 있으면 전체를 조회한다.
    private String keyword;
}
