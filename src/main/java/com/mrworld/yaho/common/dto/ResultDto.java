package com.mrworld.yaho.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResultDto {

    @Schema(description = "요청 성공 여부", example = "false")
    private boolean success;

    @Schema(description = "응답 메시지", example = "요청 값이 올바르지 않습니다.")
    private String message;

    @Schema(description = "HTTP 상태 코드", example = "400")
    private int code;

    public static ResultDto fail(int code, String message) {
        return ResultDto.builder()
                .success(false)
                .message(message)
                .code(code)
                .build();
    }
}
