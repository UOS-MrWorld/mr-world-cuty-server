package com.mrworld.yaho.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// 투어 마감 처리(TourScheduler) 같은 정기 작업을 켠다.
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
