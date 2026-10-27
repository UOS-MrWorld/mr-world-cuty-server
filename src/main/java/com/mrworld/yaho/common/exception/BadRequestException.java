package com.mrworld.yaho.common.exception;

// 요청 값이 비즈니스 규칙에 맞지 않을 때(400). 예: 예약 마감, 허용되지 않는 옵션, 변경할 값 없음
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
