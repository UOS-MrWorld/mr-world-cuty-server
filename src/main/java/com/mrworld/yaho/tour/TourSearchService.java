package com.mrworld.yaho.tour;

import com.mrworld.yaho.common.dto.PageResponse;
import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.exception.BadRequestException;
import com.mrworld.yaho.tour.dto.TourSearchRequest;
import com.mrworld.yaho.tour.dto.TourSummaryResponse;
import com.mrworld.yaho.tour.dto.VoiceSearchRequest;
import com.mrworld.yaho.tour.dto.VoiceSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 고객의 여행 상품 검색. GUI 조건 검색과, STT로 변환된 음성 문장 검색을 맡는다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TourSearchService {

    // 음성 검색 결과는 한 번에 최대 이만큼만 돌려준다.
    private static final int VOICE_SEARCH_LIMIT = 20;

    // 음성 문장에서 테마를 찾기 위한 단어 목록
    private static final Map<Theme, List<String>> THEME_KEYWORDS = new EnumMap<>(Map.of(
            Theme.HONEYMOON, List.of("허니문", "신혼"),
            Theme.HEALING, List.of("힐링", "효도", "부모님", "휴양", "온천"),
            Theme.GOLF, List.of("골프"),
            Theme.TREKKING, List.of("트레킹", "등산")
    ));

    // 모든 상품에 들어 있어 검색 단서가 되지 않는 단어
    private static final Set<String> GENERIC_WORDS = Set.of("투어", "여행", "상품");

    private final TourRepository tourRepository;

    public PageResponse<TourSummaryResponse> searchTours(TourSearchRequest request) {
        String keyword = request.getKeyword() == null || request.getKeyword().isBlank()
                ? null
                : request.getKeyword().trim();

        return PageResponse.of(tourRepository.search(
                Tour.CUSTOMER_VISIBLE_STATUSES,
                keyword,
                request.getTheme(),
                request.getMaxPrice(),
                request.getDepartureDate(),
                request.toPageable(Tour.DEFAULT_SORT)
        ), TourSummaryResponse::from);
    }

    // 음성 인식 문장에서 테마를 찾아 필터로 쓰고, 테마가 없으면 문장에 상품명·설명의 단어가 들어 있는 상품을 찾는다.
    public VoiceSearchResponse voiceSearch(VoiceSearchRequest request) {
        String queryText = request.getQueryText() == null ? "" : request.getQueryText().trim();
        if (queryText.isEmpty()) {
            throw new BadRequestException("검색 문장이 비어 있습니다.");
        }

        List<Theme> themes = detectThemes(queryText);
        // 테마가 없으면 조건(가격·날짜)만으로 후보를 모은 뒤 문장과 비교한다.
        List<Theme> searchThemes = themes.isEmpty() ? Collections.singletonList(null) : themes;
        // 문장 비교는 조회 후에 하므로, 테마가 없을 때는 후보를 자르지 않고 모두 가져온 뒤 마지막에 개수를 제한한다.
        Pageable pageable = themes.isEmpty()
                ? Pageable.unpaged(Tour.DEFAULT_SORT)
                : PageRequest.of(0, VOICE_SEARCH_LIMIT, Tour.DEFAULT_SORT);

        Map<Long, Tour> found = new LinkedHashMap<>();
        for (Theme theme : searchThemes) {
            tourRepository.search(Tour.CUSTOMER_VISIBLE_STATUSES, null, theme, request.getMaxPrice(),
                            request.getDepartureDate(), pageable)
                    .forEach(tour -> found.putIfAbsent(tour.getTourId(), tour));
        }

        List<TourSummaryResponse> tours = found.values().stream()
                .filter(tour -> !themes.isEmpty() || mentions(queryText, tour))
                .sorted(Comparator.comparing(Tour::getStartDate).thenComparing(Tour::getTourId))
                .limit(VOICE_SEARCH_LIMIT)
                .map(TourSummaryResponse::from)
                .toList();

        return VoiceSearchResponse.of(queryText, tours);
    }

    private List<Theme> detectThemes(String queryText) {
        return THEME_KEYWORDS.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(queryText::contains))
                .map(Map.Entry::getKey)
                .toList();
    }

    // 문장이 상품명·설명의 의미 있는 단어(2자 이상, 공통 단어 제외)를 하나라도 포함하는지 확인한다.
    private boolean mentions(String queryText, Tour tour) {
        String source = tour.getTourName() + " " + (tour.getDescription() == null ? "" : tour.getDescription());
        return Arrays.stream(source.split("[^\\p{L}\\p{N}]+"))
                .filter(word -> word.length() >= 2 && !GENERIC_WORDS.contains(word))
                .anyMatch(queryText::contains);
    }
}
