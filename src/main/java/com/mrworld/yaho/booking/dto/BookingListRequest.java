package com.mrworld.yaho.booking.dto;

import com.mrworld.yaho.common.enums.BookingStatus;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Setter
public class BookingListRequest {
    // 예약 상태 필터. 비어 있으면 전체 상태를 조회한다.
    private BookingStatus status;

    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    private int page = 0;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    private int size = 20;

    // 최근에 만든 예약부터 보여준다.
    public Pageable toPageable() {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "bookingId"));
    }
}
