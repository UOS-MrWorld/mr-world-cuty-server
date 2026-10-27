package com.mrworld.yaho.tour.dto;

import com.mrworld.yaho.common.enums.Transport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

// 바꿀 값만 보낸다. gifts를 보내면 기존 기념품 구성 전체를 그 목록으로 교체한다(빈 배열이면 모두 삭제).
// 테마는 상품의 성격 자체라 수정할 수 없다.
@Getter
@Setter
public class TourUpdateRequest {
    @Pattern(regexp = ".*\\S.*", message = "투어 이름은 공백일 수 없습니다.")
    @Size(max = 255, message = "투어 이름은 255자 이하여야 합니다.")
    private String tourName;

    @Size(max = 255, message = "투어 설명은 255자 이하여야 합니다.")
    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    @Min(value = 0, message = "기본 가격은 0 이상이어야 합니다.")
    private Integer basePrice;

    private Transport defaultTransport;

    @Valid
    private List<TourCreateRequest.GiftItem> gifts;

    public boolean hasNoUpdate() {
        return tourName == null && description == null && startDate == null && endDate == null
                && basePrice == null && defaultTransport == null && gifts == null;
    }
}
