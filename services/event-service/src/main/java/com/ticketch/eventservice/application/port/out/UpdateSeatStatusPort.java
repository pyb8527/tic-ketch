package com.ticketch.eventservice.application.port.out;

import com.ticketch.eventservice.domain.model.Seat;

/**
 * [Output Port] 좌석 상태 DB 업데이트 인터페이스.
 *
 * <p>구현체: {@link com.ticketch.eventservice.adapter.out.persistence.EventPersistenceAdapter}
 * <p>JPA Optimistic Lock({@code @Version})으로 동시 업데이트 충돌을 감지한다.
 */
public interface UpdateSeatStatusPort {

    void updateStatus(Long seatId, Seat.SeatStatus newStatus);
}
