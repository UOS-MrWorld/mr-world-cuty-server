package com.mrworld.yaho.tour;

import com.mrworld.yaho.booking.BookingService;
import com.mrworld.yaho.booking.dto.QuoteRequest;
import com.mrworld.yaho.booking.dto.QuoteResponse;
import com.mrworld.yaho.common.dto.PageResponse;
import com.mrworld.yaho.common.dto.PagingRequest;
import com.mrworld.yaho.common.dto.ResultDto;
import com.mrworld.yaho.config.SwaggerExamples;
import com.mrworld.yaho.tour.dto.StaffTourDetailResponse;
import com.mrworld.yaho.tour.dto.StaffTourListRequest;
import com.mrworld.yaho.tour.dto.StaffTourSummaryResponse;
import com.mrworld.yaho.tour.dto.TourCreateRequest;
import com.mrworld.yaho.tour.dto.TourDetailResponse;
import com.mrworld.yaho.tour.dto.TourResponse;
import com.mrworld.yaho.tour.dto.TourSearchRequest;
import com.mrworld.yaho.tour.dto.TourSummaryResponse;
import com.mrworld.yaho.tour.dto.TourUpdateRequest;
import com.mrworld.yaho.tour.dto.VoiceSearchRequest;
import com.mrworld.yaho.tour.dto.VoiceSearchResponse;
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

// 여행 상품 API. 고객용(getTours, searchTours, voiceSearch, getTourDetail, getQuote)은 /api/v1/customer/tours,
// 직원용(getStaffTours, getStaffTourDetail, createTour, updateTour)은 /api/v1/staff/tours 아래에 있다.
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Tour", description = "여행 상품 조회/검색/가격 계산 API")
public class TourController {
    private final TourService tourService;
    private final TourSearchService tourSearchService;
    private final BookingService bookingService;

    @GetMapping("/customer/tours")
    @Operation(summary = "여행 상품 목록 조회", description = "고객이 조회 가능한 여행 상품 목록을 페이지 단위로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "페이지 값 오류",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST)
                    ))
    })
    public ResponseEntity<PageResponse<TourSummaryResponse>> getTours(@ParameterObject @Valid PagingRequest request) {
        return ResponseEntity.ok(tourService.getTours(request));
    }

    @GetMapping("/customer/tours/search")
    @Operation(summary = "여행 상품 검색",
            description = "키워드·테마·최대 가격·출발 날짜 조건으로 검색합니다. 조건은 모두 선택값이며 AND로 적용됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "검색 조건 또는 페이지 값 오류",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST)
                    ))
    })
    public ResponseEntity<PageResponse<TourSummaryResponse>> searchTours(@ParameterObject @Valid TourSearchRequest request) {
        return ResponseEntity.ok(tourSearchService.searchTours(request));
    }

    @GetMapping("/customer/tours/{tourId}")
    @Operation(summary = "여행 상품 상세 조회", description = "투어 정보와 선택 가능한 투어 등급을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 투어",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.NOT_FOUND)
                    ))
    })
    public ResponseEntity<TourDetailResponse> getTourDetail(@PathVariable Long tourId) {
        return ResponseEntity.ok(tourService.getTourDetail(tourId));
    }

    @PostMapping("/customer/tours/voice-search")
    @Operation(summary = "음성 기반 여행 상품 검색",
            description = "클라이언트가 STT로 변환한 문장과 인식된 최대 가격/출발 날짜 조건으로 여행 상품을 검색합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "검색 문장 또는 검색 조건 오류",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST)
                    ))
    })
    public ResponseEntity<VoiceSearchResponse> voiceSearch(@RequestBody @Valid VoiceSearchRequest request) {
        return ResponseEntity.ok(tourSearchService.voiceSearch(request));
    }

    @PostMapping("/customer/tours/{tourId}/quote")
    @Operation(summary = "여행 옵션 가격 계산",
            description = "선택한 옵션 기준 예상 가격과 단골 할인 적용 금액을 계산합니다. 예약은 생성하지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "가격 계산 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "옵션/인원/등급 오류 또는 예약 마감",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST)
                    )),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 투어",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.NOT_FOUND)
                    )),
            @ApiResponse(responseCode = "409", description = "모집 중이 아닌 투어",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.CONFLICT)
                    ))
    })
    public ResponseEntity<QuoteResponse> getQuote(
            @PathVariable Long tourId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid QuoteRequest request
    ) {
        Long customerId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(bookingService.getQuote(tourId, customerId, request));
    }

    @PostMapping("/staff/tours")
    @Operation(summary = "여행 상품 등록",
            description = "직원이 투어 일정·가격·기본 교통·기념품 구성을 등록합니다. 등록된 상품은 모집 중 상태로 시작합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "날짜 범위·가격·교통·기념품 수량 오류",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST)
                    )),
            @ApiResponse(responseCode = "404", description = "지정한 기념품 재고가 존재하지 않음",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.NOT_FOUND)
                    ))
    })
    public ResponseEntity<TourResponse> createTour(@RequestBody @Valid TourCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tourService.createTour(request));
    }

    @GetMapping("/staff/tours")
    @Operation(summary = "여행 상품 현황 목록 조회",
            description = "직원이 등록 투어의 상태와 결제 완료 인원을 조회합니다. 상태·시작일 범위로 거를 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "필터/페이지 값 오류",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST)
                    ))
    })
    public ResponseEntity<PageResponse<StaffTourSummaryResponse>> getStaffTours(@ParameterObject @Valid StaffTourListRequest request) {
        return ResponseEntity.ok(tourService.getStaffTours(request));
    }

    @GetMapping("/staff/tours/{tourId}")
    @Operation(summary = "여행 상품 상세 현황 조회",
            description = "직원이 투어 상세, 결제 완료 인원, 예약 현황, 기념품 구성과 현재 재고를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 투어",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.NOT_FOUND)
                    ))
    })
    public ResponseEntity<StaffTourDetailResponse> getStaffTourDetail(@PathVariable Long tourId) {
        return ResponseEntity.ok(tourService.getStaffTourDetail(tourId));
    }

    @PatchMapping("/staff/tours/{tourId}")
    @Operation(summary = "여행 상품 수정",
            description = "모집 중인 투어의 정보와 기념품 구성을 수정합니다. 보낸 값만 바꾸고, gifts를 보내면 기념품 구성 전체를 교체합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "변경할 값 없음, 기간·가격·기념품 수량 오류",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST)
                    )),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 투어 또는 기념품 재고",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.NOT_FOUND)
                    )),
            @ApiResponse(responseCode = "409", description = "모집 중이 아니어서 수정할 수 없는 투어",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.CONFLICT)
                    ))
    })
    public ResponseEntity<TourResponse> updateTour(
            @PathVariable Long tourId,
            @RequestBody @Valid TourUpdateRequest request
    ) {
        return ResponseEntity.ok(tourService.updateTour(tourId, request));
    }
}
