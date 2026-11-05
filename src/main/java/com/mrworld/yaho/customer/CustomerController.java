package com.mrworld.yaho.customer;

import com.mrworld.yaho.common.dto.PageResponse;
import com.mrworld.yaho.common.dto.ResultDto;
import com.mrworld.yaho.config.SwaggerExamples;
import com.mrworld.yaho.customer.dto.CustomerListRequest;
import com.mrworld.yaho.customer.dto.CustomerSummaryResponse;
import com.mrworld.yaho.customer.dto.LoyaltyGradeBatchUpdateRequest;
import com.mrworld.yaho.customer.dto.LoyaltyGradeBatchUpdateResponse;
import com.mrworld.yaho.customer.dto.LoyaltyPolicyResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 직원용 고객 목록과 고객별 단골 등급 일괄 변경, 단골 등급 기준 조회
@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
@Tag(name = "Customer", description = "직원용 고객·단골 관리 API")
public class CustomerController {
    private final CustomerService customerService;

    @GetMapping("/loyalty-policy")
    @Operation(summary = "단골 등급 기준 조회", description = "코드에 고정된 단골 등급 기준 횟수와 할인율을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true)
    })
    public ResponseEntity<LoyaltyPolicyResponse> getLoyaltyPolicy() {
        return ResponseEntity.ok(customerService.getLoyaltyPolicy());
    }

    @GetMapping("/customers")
    @Operation(summary = "고객 목록 조회",
            description = "이름·로그인 아이디로 고객을 검색하고 단골 등급으로 거릅니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "필터/페이지 값 오류",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST)
                    ))
    })
    public ResponseEntity<PageResponse<CustomerSummaryResponse>> getCustomers(@ParameterObject @Valid CustomerListRequest request) {
        return ResponseEntity.ok(customerService.getCustomers(request));
    }

    @PatchMapping("/customers/loyalty-grades")
    @Operation(summary = "고객별 단골 등급 일괄 변경",
            description = "여러 고객의 단골 등급을 한 요청과 한 트랜잭션으로 변경합니다. 할인율은 변경된 등급에서 자동으로 결정됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "일괄 변경 완료", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "빈 요청, 중복 고객 또는 등급 누락",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST)
                    ))
    })
    public ResponseEntity<LoyaltyGradeBatchUpdateResponse> updateLoyaltyGrades(
            @RequestBody @Valid LoyaltyGradeBatchUpdateRequest request
    ) {
        return ResponseEntity.ok(customerService.updateLoyaltyGrades(request));
    }
}
