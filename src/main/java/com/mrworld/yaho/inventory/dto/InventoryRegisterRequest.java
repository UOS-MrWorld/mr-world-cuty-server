package com.mrworld.yaho.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryRegisterRequest {
    @NotBlank(message = "물품명은 필수입니다.")
    @Size(max = 255, message = "물품명은 255자 이하여야 합니다.")
    private String itemName;

    @NotNull(message = "초기 재고 수량은 필수입니다.")
    @Min(value = 0, message = "재고 수량은 0 이상이어야 합니다.")
    private Integer quantity;
}
