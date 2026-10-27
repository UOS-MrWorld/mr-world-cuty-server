package com.mrworld.yaho.demo;

import com.mrworld.yaho.common.enums.TourStatus;
import com.mrworld.yaho.common.exception.ConflictException;
import com.mrworld.yaho.common.exception.NotFoundException;
import com.mrworld.yaho.tour.Tour;
import com.mrworld.yaho.tour.TourLifecycleService;
import com.mrworld.yaho.tour.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 데모 전용. 자정 스케줄러가 날짜에 맞춰 하는 마감·완료 처리를 특정 투어에 바로 실행한다.
// 데모가 끝나면 demo 패키지를 통째로 지우면 된다.
@Service
@RequiredArgsConstructor
public class DemoService {

    private final TourRepository tourRepository;
    private final TourLifecycleService tourLifecycleService;

    // 모집 중 투어를 마감한다. 결제 인원 3명 이상이면 확정(재고 차감·문자), 미만이면 취소·환불
    @Transactional
    public DemoTourResponse closeTour(Long tourId) {
        Tour tour = findTour(tourId, TourStatus.RECRUITING);
        tourLifecycleService.closeTour(tourId);
        return new DemoTourResponse(tourId, tour.getTourStatus());
    }

    // 확정 투어를 완료하고, 참가 고객의 단골 등급을 다시 정한다.
    @Transactional
    public DemoTourResponse completeTour(Long tourId) {
        Tour tour = findTour(tourId, TourStatus.CONFIRMED);
        tourLifecycleService.completeTour(tourId);
        return new DemoTourResponse(tourId, tour.getTourStatus());
    }

    private Tour findTour(Long tourId, TourStatus requiredStatus) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new NotFoundException("투어를 찾을 수 없습니다."));
        if (tour.getTourStatus() != requiredStatus) {
            throw new ConflictException(requiredStatus + " 상태의 투어만 처리할 수 있습니다.");
        }
        return tour;
    }

    public record DemoTourResponse(Long tourId, TourStatus tourStatus) {
    }
}
