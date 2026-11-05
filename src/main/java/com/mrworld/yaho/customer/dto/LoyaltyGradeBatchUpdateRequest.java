package com.mrworld.yaho.customer.dto;

import com.mrworld.yaho.common.enums.LoyaltyGrade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LoyaltyGradeBatchUpdateRequest {
    @NotEmpty(message = "변경할 고객 등급은 한 건 이상이어야 합니다.")
    @Valid
    private List<Change> changes;

    @Getter
    @Setter
    public static class Change {
        @NotNull(message = "고객 ID는 필수입니다.")
        private Long customerId;

        @NotNull(message = "단골 등급은 필수입니다.")
        private LoyaltyGrade loyaltyGrade;
    }
}
