package com.mrworld.yaho.tour;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;

public interface TourRepository extends JpaRepository<Tour, Long> {

    Page<Tour> findByTourStatusIn(Collection<Tour.Status> statuses, Pageable pageable);

    // 조건이 null이면 해당 조건은 무시하고, 여러 조건은 AND로 검색한다.
    @Query("""
            select t from Tour t
            where t.tourStatus in :statuses
              and (:keyword is null
                   or lower(t.tourName) like lower(concat('%', :keyword, '%'))
                   or lower(t.description) like lower(concat('%', :keyword, '%')))
              and (:theme is null or t.theme = :theme)
              and (:maxPrice is null or t.basePrice <= :maxPrice)
              and (:departureDate is null or t.startDate = :departureDate)
            """)
    Page<Tour> search(
            @Param("statuses") Collection<Tour.Status> statuses,
            @Param("keyword") String keyword,
            @Param("theme") Tour.Theme theme,
            @Param("maxPrice") Integer maxPrice,
            @Param("departureDate") LocalDate departureDate,
            Pageable pageable
    );
}
