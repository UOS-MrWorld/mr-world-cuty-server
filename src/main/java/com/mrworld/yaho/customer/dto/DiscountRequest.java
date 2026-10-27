package com.mrworld.yaho.customer.dto;

import com.mrworld.yaho.common.enums.LoyaltyGrade;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// 단골 할인 일괄 적용 대상 등급. 단골 할인은 등급 단위로만 적용한다.
@Getter
@Setter
public class DiscountRequest {
    @NotNull(message = "할인을 적용할 등급은 필수입니다.")
    private LoyaltyGrade loyaltyGrade;
}
