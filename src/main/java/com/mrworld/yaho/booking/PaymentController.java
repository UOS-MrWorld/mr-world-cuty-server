package com.mrworld.yaho.booking;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Mock 결제. 결제 성공 시 여행확정 판정은 BookingService에서 처리
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
}
