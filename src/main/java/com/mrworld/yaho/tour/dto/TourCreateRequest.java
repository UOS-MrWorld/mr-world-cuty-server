package com.mrworld.yaho.tour.dto;

import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.Transport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

// 등급별 기본 호텔/식사 구성은 전역 고정 규칙이라 받지 않는다.
@Getter
@Setter
public class TourCreateRequest {
    @NotNull(message = "테마는 필수입니다.")
    private Theme theme;

    @NotBlank(message = "투어 이름은 필수입니다.")
    @Size(max = 255, message = "투어 이름은 255자 이하여야 합니다.")
    private String tourName;

    @Size(max = 255, message = "투어 설명은 255자 이하여야 합니다.")
    private String description;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다.")
    private LocalDate endDate;

    @NotNull(message = "기본 가격은 필수입니다.")
    @Min(value = 0, message = "기본 가격은 0 이상이어야 합니다.")
    private Integer basePrice;

    @NotNull(message = "기본 교통은 필수입니다.")
    private Transport defaultTransport;

    @Valid
    private List<GiftItem> gifts;

    @Getter
    @Setter
    public static class GiftItem {
        @NotNull(message = "기념품 재고 ID는 필수입니다.")
        private Long itemId;

        @NotNull(message = "1인당 지급 수량은 필수입니다.")
        @Min(value = 1, message = "1인당 지급 수량은 1 이상이어야 합니다.")
        private Integer quantityPerGuest;
    }
}
