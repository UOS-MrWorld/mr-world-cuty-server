package com.mrworld.yaho.booking;

import com.mrworld.yaho.common.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // 투어별 특정 상태 예약의 인원 합. 예약이 없으면 0을 반환한다.
    @Query("""
            select coalesce(sum(b.guestCount), 0) from Booking b
            where b.tour.tourId = :tourId and b.bookingStatus = :status
            """)
    int sumGuestCountByTourIdAndBookingStatus(@Param("tourId") Long tourId, @Param("status") BookingStatus status);

    // 고객의 예약을 조회한다. status가 null이면 전체 상태를 조회한다.
    @EntityGraph(attributePaths = "tour")
    @Query("""
            select b from Booking b
            where b.customer.memberId = :customerId
              and (:status is null or b.bookingStatus = :status)
            """)
    Page<Booking> findMyBookings(
            @Param("customerId") Long customerId,
            @Param("status") BookingStatus status,
            Pageable pageable
    );
}
