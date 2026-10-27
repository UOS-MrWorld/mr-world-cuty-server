package com.mrworld.yaho.booking;

import com.mrworld.yaho.common.enums.BookingStatus;
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
}
