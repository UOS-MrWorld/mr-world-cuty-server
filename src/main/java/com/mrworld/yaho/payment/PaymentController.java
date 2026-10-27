package com.mrworld.yaho.payment;

import com.mrworld.yaho.common.dto.ResultDto;
import com.mrworld.yaho.config.SwaggerExamples;
import com.mrworld.yaho.payment.dto.PaymentRequest;
import com.mrworld.yaho.payment.dto.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.mrworld.yaho.security.LoginMemberId;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 고객 Mock 결제 API
@RestController
@RequestMapping("/api/v1/customer/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "여행 상품 결제 API")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "여행 상품 결제",
            description = "결제 전(PENDING) 예약을 결제 시점 단골 할인율로 계산한 금액으로 Mock 결제합니다. "
                    + "카드번호가 \"1\"이면 승인, 그 외는 거절(FAILED)됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "결제 시도 처리 완료 (승인 또는 거절)",
                    useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "결제할 수 없는 예약, 카드번호 누락 또는 결제 마감",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST)
                    )),
            @ApiResponse(responseCode = "403", description = "고객 권한이 아니거나 본인 예약이 아님",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)
                    )),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 예약",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.NOT_FOUND)
                    )),
            @ApiResponse(responseCode = "409", description = "이미 결제가 완료된 예약",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.CONFLICT)
                    ))
    })
    public ResponseEntity<PaymentResponse> pay(
            @LoginMemberId Long customerId,
            @RequestBody @Valid PaymentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.pay(customerId, request));
    }
}
