package com.mrworld.yaho.tour;

import com.mrworld.yaho.common.enums.Theme;
import com.mrworld.yaho.common.enums.TourStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface TourRepository extends JpaRepository<Tour, Long> {

    Page<Tour> findByTourStatusIn(Collection<TourStatus> statuses, Pageable pageable);

    // 시작일이 기준일보다 빠른 투어를 조회한다. 마감이 지났는데 아직 처리되지 않은 모집 중 투어를 찾을 때 쓴다.
    List<Tour> findAllByTourStatusAndStartDateBefore(TourStatus status, LocalDate startDate);

    // 종료일이 기준일보다 빠른 투어를 조회한다. 여행이 끝났는데 아직 완료 처리되지 않은 확정 투어를 찾을 때 쓴다.
    List<Tour> findAllByTourStatusAndEndDateBefore(TourStatus status, LocalDate endDate);

    // 직원용 상품 현황 목록. 조건이 null이면 해당 조건은 무시한다.
    @Query("""
            select t from Tour t
            where (:status is null or t.tourStatus = :status)
              and (:startDateFrom is null or t.startDate >= :startDateFrom)
              and (:startDateTo is null or t.startDate <= :startDateTo)
            """)
    Page<Tour> searchForStaff(
            @Param("status") TourStatus status,
            @Param("startDateFrom") LocalDate startDateFrom,
            @Param("startDateTo") LocalDate startDateTo,
            Pageable pageable
    );

    // 조건이 null이면 해당 조건은 무시하고, 여러 조건은 AND로 검색한다.
    @Query("""
            select t from Tour t
            where t.tourStatus in :statuses
              and (:keyword is null
                   or lower(t.tourName) like lower(concat('%', :keyword, '%'))
                   or lower(t.description) like lower(concat('%', :keyword, '%')))
              and (:theme is null or t.theme = :theme)
              and (:maxPrice is null or t.price <= :maxPrice)
              and (:departureDate is null or t.startDate = :departureDate)
            """)
    Page<Tour> search(
            @Param("statuses") Collection<TourStatus> statuses,
            @Param("keyword") String keyword,
            @Param("theme") Theme theme,
            @Param("maxPrice") Integer maxPrice,
            @Param("departureDate") LocalDate departureDate,
            Pageable pageable
    );
}
