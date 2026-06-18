package com.ticketch.eventservice.adapter.out.persistence;

import com.ticketch.eventservice.domain.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeatJpaRepository extends JpaRepository<SeatJpaEntity, Long> {

    List<SeatJpaEntity> findByEventId(Long eventId);

    /** Optimistic Lock과 함께 단일 쿼리로 좌석 상태 업데이트 */
    @Modifying
    @Query("UPDATE SeatJpaEntity s SET s.status = :status WHERE s.id = :seatId")
    int updateStatus(@Param("seatId") Long seatId, @Param("status") Seat.SeatStatus status);
}
