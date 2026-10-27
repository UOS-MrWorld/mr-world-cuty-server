package com.mrworld.yaho.customer;

import com.mrworld.yaho.common.dto.PageResponse;
import com.mrworld.yaho.common.dto.ResultDto;
import com.mrworld.yaho.config.SwaggerExamples;
import com.mrworld.yaho.customer.dto.CustomerDetailResponse;
import com.mrworld.yaho.customer.dto.CustomerListRequest;
import com.mrworld.yaho.customer.dto.CustomerSummaryResponse;
import com.mrworld.yaho.customer.dto.DiscountRequest;
import com.mrworld.yaho.customer.dto.DiscountResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 직원용 고객 목록/상세 조회(/staff/customers), 단골 할인 등급별 일괄 적용, 단골 등급 기준 조회(/staff/loyalty-policy)
@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
@Tag(name = "Customer", description = "직원용 고객·단골 관리 API")
public class CustomerController {
    private final CustomerService customerService;

    @GetMapping("/loyalty-policy")
    @Operation(summary = "단골 등급 기준 조회", description = "코드에 고정된 단골 등급 기준 횟수와 할인율을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
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
    public ResponseEntity<LoyaltyPolicyResponse> getLoyaltyPolicy() {
        return ResponseEntity.ok(customerService.getLoyaltyPolicy());
    }

    @GetMapping("/customers")
    @Operation(summary = "고객 목록 조회",
            description = "이름·로그인 아이디로 고객을 검색하고 단골 등급·할인 적용 여부로 거릅니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "필터/페이지 값 오류",
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
    public ResponseEntity<PageResponse<CustomerSummaryResponse>> getCustomers(@ParameterObject @Valid CustomerListRequest request) {
        return ResponseEntity.ok(customerService.getCustomers(request));
    }

    @GetMapping("/customers/{customerId}")
    @Operation(summary = "고객 상세 조회", description = "고객 기본정보, 완료 여행 횟수, 등급과 할인 적용 상태를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
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
            @ApiResponse(responseCode = "404", description = "존재하지 않는 고객",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.NOT_FOUND)
                    )),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.INTERNAL_SERVER_ERROR)
                    ))
    })
    public ResponseEntity<CustomerDetailResponse> getCustomerDetail(@PathVariable Long customerId) {
        return ResponseEntity.ok(customerService.getCustomerDetail(customerId));
    }

    @PatchMapping("/customers/loyalty-discount/by-grade")
    @Operation(summary = "단골 할인 등급별 일괄 적용",
            description = "선택한 등급의 고객 중 단골 할인이 적용되지 않은 고객에게 할인을 일괄 활성화합니다. "
                    + "이미 적용된 고객은 건너뛰며, 할인율이 0인 등급(NORMAL)은 적용할 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "일괄 적용 완료", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "등급 누락 또는 적용 가능한 고객 없음",
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
    public ResponseEntity<DiscountResponse> activateDiscountByGrade(@RequestBody @Valid DiscountRequest request) {
        return ResponseEntity.ok(customerService.activateDiscountByGrade(request.getLoyaltyGrade()));
    }
}
