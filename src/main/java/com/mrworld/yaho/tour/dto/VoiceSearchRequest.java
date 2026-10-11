package com.mrworld.yaho.tour.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

// 클라이언트가 STT로 변환한 문장과, 음성에서 인식된 가격 상한·출발 날짜 조건
@Getter
@Setter
public class VoiceSearchRequest {
    @NotBlank(message = "검색 문장은 필수입니다.")
    private String queryText;

    @Min(value = 0, message = "최대 가격은 0 이상이어야 합니다.")
    private Integer maxPrice;

    private LocalDate departureDate;
}
