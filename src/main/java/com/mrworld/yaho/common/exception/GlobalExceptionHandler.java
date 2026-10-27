package com.mrworld.yaho.common.exception;

import com.mrworld.yaho.common.dto.ResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String INVALID_FORMAT = "요청 형식이 올바르지 않습니다.";

    // 검증 실패 메시지를 그대로 쓰되, 타입 변환 실패(예: 없는 enum 값)는 스프링 기본 문구 대신 공통 문구로 보낸다.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResultDto> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.contains(TypeMismatchException.class) ? INVALID_FORMAT : error.getDefaultMessage())
                .orElse("요청 값이 올바르지 않습니다.");

        return response(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ResultDto> handleBadRequest(BadRequestException e) {
        return response(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // JSON 본문을 읽을 수 없거나 경로 변수의 형식이 틀린 경우(예: /bookings/abc)
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ResultDto> handleInvalidFormat(Exception e) {
        return response(HttpStatus.BAD_REQUEST, INVALID_FORMAT);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ResultDto> handleAuthenticationFailed(AuthenticationFailedException e) {
        return response(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ResultDto> handleForbidden(ForbiddenException e) {
        return response(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ResultDto> handleNotFound(NotFoundException e) {
        return response(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ResultDto> handleConflict(ConflictException e) {
        return response(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultDto> handleUnexpected(Exception e) {
        log.error("예상하지 못한 오류", e);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
    }

    private ResponseEntity<ResultDto> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ResultDto.fail(status.value(), message));
    }
}
