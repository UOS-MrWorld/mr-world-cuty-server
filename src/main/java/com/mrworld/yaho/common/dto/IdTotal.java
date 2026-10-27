package com.mrworld.yaho.common.dto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// "ID별 합계·개수" 집계 쿼리의 결과 한 줄. 쿼리에서 id, total 별칭으로 고른다.
// 예: 투어별 결제 인원, 고객별 완료 여행 횟수, 물품별 사용 예정 수량
public interface IdTotal {
    Long getId();

    Long getTotal();

    // 집계 결과를 ID → 합계 맵으로 바꾼다. 결과에 없는 ID는 맵에도 없으므로 getOrDefault로 0을 쓴다.
    static Map<Long, Long> toMap(List<IdTotal> rows) {
        return rows.stream().collect(Collectors.toMap(IdTotal::getId, IdTotal::getTotal));
    }
}
