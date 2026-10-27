package com.mrworld.yaho.booking;

import com.mrworld.yaho.booking.dto.BookingRequest;
import com.mrworld.yaho.booking.dto.BookingResponse;
import com.mrworld.yaho.common.dto.ResultDto;
import com.mrworld.yaho.config.SwaggerExamples;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 고객 예약 API. 예약 생성(createBooking)을 제공하며,
// 옵션 수정·목록/상세 조회·취소·여행 내역 조회는 같은 경로 아래에 추가한다.
@RestController
@RequestMapping("/api/v1/customer/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking", description = "여행 예약 API")
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "여행 상품 예약 생성",
            description = "GUI 또는 음성으로 선택한 옵션을 저장하고 결제 전(PENDING) 예약을 생성합니다. 결제는 수행하지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "예약 생성 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "옵션/인원/등급 오류 또는 예약 마감",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST)
                    )),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED)
                    )),
            @ApiResponse(responseCode = "403", description = "고객 권한이 아님",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.FORBIDDEN)
                    )),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 투어",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.NOT_FOUND)
                    )),
            @ApiResponse(responseCode = "409", description = "모집 중이 아닌 투어 또는 기념품 재고 부족",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.CONFLICT)
                    )),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.INTERNAL_SERVER_ERROR)
                    ))
    })
    public ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid BookingRequest request
    ) {
        Long customerId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(customerId, request));
    }
}
