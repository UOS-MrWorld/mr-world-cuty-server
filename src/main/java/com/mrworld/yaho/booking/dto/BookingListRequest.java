package com.mrworld.yaho.booking.dto;

import com.mrworld.yaho.common.dto.PagingRequest;
import com.mrworld.yaho.common.enums.BookingStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingListRequest extends PagingRequest {
    // 예약 상태 필터. 비어 있으면 전체 상태를 조회한다.
    private BookingStatus status;
}
