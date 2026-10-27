package com.mrworld.yaho.demo;

import com.mrworld.yaho.demo.DemoService.DemoTourResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 데모 전용 API. 발표 현장에서 투어 확정·완료를 바로 보여 주기 위해 둔다.
@RestController
@RequestMapping("/api/v1/staff/demo/tours")
@RequiredArgsConstructor
@Tag(name = "Demo", description = "데모 전용: 투어 마감·완료 즉시 실행")
public class DemoController {

    private final DemoService demoService;

    @PostMapping("/{tourId}/close")
    @Operation(summary = "[데모] 투어 마감 즉시 실행",
            description = "날짜와 상관없이 모집 중 투어를 마감합니다. 결제 인원 3명 이상이면 확정, 미만이면 취소·환불합니다. 404: 투어 없음, 409: 모집 중이 아님")
    public ResponseEntity<DemoTourResponse> closeTour(@PathVariable Long tourId) {
        return ResponseEntity.ok(demoService.closeTour(tourId));
    }

    @PostMapping("/{tourId}/complete")
    @Operation(summary = "[데모] 투어 완료 즉시 실행",
            description = "날짜와 상관없이 확정 투어를 완료하고 참가 고객의 단골 등급을 다시 정합니다. 404: 투어 없음, 409: 확정이 아님")
    public ResponseEntity<DemoTourResponse> completeTour(@PathVariable Long tourId) {
        return ResponseEntity.ok(demoService.completeTour(tourId));
    }
}
