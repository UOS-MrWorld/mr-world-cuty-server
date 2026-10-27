package com.mrworld.yaho.booking;

import com.mrworld.yaho.booking.dto.BookingDetailResponse;
import com.mrworld.yaho.booking.dto.BookingListRequest;
import com.mrworld.yaho.booking.dto.BookingListResponse;
import com.mrworld.yaho.booking.dto.BookingRequest;
import com.mrworld.yaho.booking.dto.BookingResponse;
import com.mrworld.yaho.booking.dto.BookingUpdateRequest;
import com.mrworld.yaho.booking.dto.BookingUpdateResponse;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 고객 예약 API. 예약 생성(createBooking), 목록/상세 조회(getBookings, getBookingDetail),
// 결제 전 옵션 수정(updateBookingOptions)을 제공하며, 취소·여행 내역 조회는 같은 경로 아래에 추가한다.
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

    @GetMapping
    @Operation(summary = "내 예약 목록 조회", description = "로그인 고객의 예약 목록을 상태별로 최근 순 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "상태/페이지 값 오류",
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
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.INTERNAL_SERVER_ERROR)
                    ))
    })
    public ResponseEntity<BookingListResponse> getBookings(
            @AuthenticationPrincipal UserDetails userDetails,
            @ParameterObject @Valid BookingListRequest request
    ) {
        Long customerId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(bookingService.getBookings(customerId, request));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "예약 상세 조회", description = "본인의 예약 상세, 최종 옵션, 결제 및 투어 상태를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED)
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
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.INTERNAL_SERVER_ERROR)
                    ))
    })
    public ResponseEntity<BookingDetailResponse> getBookingDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long bookingId
    ) {
        Long customerId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(bookingService.getBookingDetail(customerId, bookingId));
    }

    @PatchMapping("/{bookingId}")
    @Operation(summary = "PENDING 예약 수정",
            description = "결제 전 예약의 투어 등급, 인원, 호텔, 식사, 교통, 추가 음료 옵션을 수정하고 금액을 다시 계산합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "변경할 값 없음, 옵션/인원/등급 오류 또는 예약 마감",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST)
                    )),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.UNAUTHORIZED)
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
            @ApiResponse(responseCode = "409", description = "결제 전 예약이 아님, 모집 중이 아닌 투어 또는 기념품 재고 부족",
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
    public ResponseEntity<BookingUpdateResponse> updateBookingOptions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long bookingId,
            @RequestBody @Valid BookingUpdateRequest request
    ) {
        Long customerId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(bookingService.updateBookingOptions(customerId, bookingId, request));
    }
}
