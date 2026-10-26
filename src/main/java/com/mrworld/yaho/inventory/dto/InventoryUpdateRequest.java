package com.mrworld.yaho.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// 변경할 값만 보낸다. 둘 다 비어 있으면 서비스에서 400으로 처리한다.
@Getter
@Setter
public class InventoryUpdateRequest {
    @Pattern(regexp = ".*\\S.*", message = "물품명은 공백일 수 없습니다.")
    @Size(max = 255, message = "물품명은 255자 이하여야 합니다.")
    private String itemName;

    @Min(value = 0, message = "재고 수량은 0 이상이어야 합니다.")
    private Integer quantity;
}
