package com.mrworld.yaho.tour;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 매일 자정(한국 시간)에 예약 마감이 지난 모집 중 투어를 확정·취소하고, 여행이 끝난 확정 투어를 완료 처리한다.
@Component
@RequiredArgsConstructor
public class TourScheduler {
    private final TourLifecycleService tourLifecycleService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void checkTourDeadline() {
        tourLifecycleService.closeRecruitingTours();
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void checkTourEnded() {
        tourLifecycleService.completeEndedTours();
    }
}
