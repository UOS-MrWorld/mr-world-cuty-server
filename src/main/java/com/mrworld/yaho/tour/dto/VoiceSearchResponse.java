package com.mrworld.yaho.tour.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class VoiceSearchResponse {
    // 검색에 사용한 문장
    private String recognizedText;
    private List<TourSummaryResponse> tours;
}
