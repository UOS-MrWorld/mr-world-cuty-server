package com.mrworld.yaho.common.exception;

import com.mrworld.yaho.common.dto.ResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResultDto> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("요청 값이 올바르지 않습니다.");

        return response(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ResultDto> handleBadRequest(Exception e) {
        String message = e instanceof IllegalArgumentException
                ? e.getMessage()
                : "요청 형식이 올바르지 않습니다.";

        return response(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ResultDto> handleAuthenticationFailed(AuthenticationFailedException e) {
        return response(HttpStatus.UNAUTHORIZED, e.getMessage());
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
