package com.mrworld.yaho.booking;

import com.mrworld.yaho.common.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // 투어별로 특정 상태인 예약을 조회한다.
    @Query("select b from Booking b where b.tour.tourId = :tourId and b.bookingStatus = :status")
    List<Booking> findAllByTourIdAndBookingStatus(@Param("tourId") Long tourId, @Param("status") BookingStatus status);

    // 투어별 특정 상태 예약의 인원 합. 예약이 없으면 0을 반환한다.
    @Query("""
            select coalesce(sum(b.guestCount), 0) from Booking b
            where b.tour.tourId = :tourId and b.bookingStatus = :status
            """)
    int sumGuestCountByTourIdAndBookingStatus(@Param("tourId") Long tourId, @Param("status") BookingStatus status);

    // 고객이 결제하고 다녀온(완료된) 투어 수. 같은 투어를 여러 건 예약했어도 한 번으로 센다.
    @Query("""
            select count(distinct b.tour) from Booking b
            where b.customer.memberId = :customerId
              and b.bookingStatus = com.mrworld.yaho.common.enums.BookingStatus.PAID
              and b.tour.tourStatus = com.mrworld.yaho.common.enums.TourStatus.COMPLETED
            """)
    int countCompletedToursByCustomerId(@Param("customerId") Long customerId);

    // 고객의 여행 내역: 결제 완료 예약 중 투어가 모집 중·확정·완료인 것 (취소된 투어 제외)
    @EntityGraph(attributePaths = "tour")
    @Query("""
            select b from Booking b
            where b.customer.memberId = :customerId
              and b.bookingStatus = com.mrworld.yaho.common.enums.BookingStatus.PAID
              and b.tour.tourStatus <> com.mrworld.yaho.common.enums.TourStatus.CANCELLED
            """)
    Page<Booking> findTravelHistoryByCustomerId(@Param("customerId") Long customerId, Pageable pageable);

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
