package com.mrworld.yaho.inventory;

import com.mrworld.yaho.common.dto.ResultDto;
import com.mrworld.yaho.config.SwaggerExamples;
import com.mrworld.yaho.inventory.dto.InventoryListRequest;
import com.mrworld.yaho.inventory.dto.InventoryListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 직원용 기념품 재고 목록 조회/등록/수정
@RestController
@RequestMapping("/api/v1/staff/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "직원용 재고 관리 API")
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping
    @Operation(summary = "재고 목록 조회", description = "창고 재고 목록을 물품명 검색어와 페이지 단위로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "페이지 값 오류",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST)
                    )),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED)
                    )),
            @ApiResponse(responseCode = "403", description = "직원 권한이 아님",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)
                    )),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.INTERNAL_SERVER_ERROR)
                    ))
    })
    public ResponseEntity<InventoryListResponse> getItems(@ParameterObject @Valid InventoryListRequest request) {
        return ResponseEntity.ok(inventoryService.getItems(request));
    }
}
