package com.mrworld.yaho.member;

import com.mrworld.yaho.common.dto.ResultDto;
import com.mrworld.yaho.config.SwaggerExamples;
import com.mrworld.yaho.member.dto.MemberInfoResponse;
import com.mrworld.yaho.member.dto.MemberUpdateRequest;
import com.mrworld.yaho.member.dto.MemberUpdateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.mrworld.yaho.security.LoginMemberId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 내 회원정보 조회/수정. 고객·직원 모두 사용한다.
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Tag(name = "Member", description = "내 회원정보 조회/수정 API")
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/me")
    @Operation(summary = "내 회원정보 조회", description = "로그인 사용자의 공통 회원정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true)
    })
    public ResponseEntity<MemberInfoResponse> getMyInfo(@LoginMemberId Long memberId) {
        return ResponseEntity.ok(memberService.getMyInfo(memberId));
    }

    @PatchMapping("/me")
    @Operation(summary = "내 회원정보 수정", description = "전화번호와 주소 중 보낸 값만 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "전화번호/주소 형식 오류 또는 수정값 없음",
                    content = @Content(
                            schema = @Schema(implementation = ResultDto.class),
                            examples = @ExampleObject(value = SwaggerExamples.BAD_REQUEST)
                    ))
    })
    public ResponseEntity<MemberUpdateResponse> updateMyInfo(
            @LoginMemberId Long memberId,
            @RequestBody @Valid MemberUpdateRequest request
    ) {
        return ResponseEntity.ok(memberService.updateMyInfo(memberId, request));
    }
}
