package com.ticketch.eventservice.application.port.out;

import com.ticketch.eventservice.domain.model.Seat;

import java.util.List;
import java.util.Optional;

/**
 * [Output Port] 좌석 조회 인터페이스.
 *
 * <p>구현체: {@link com.ticketch.eventservice.adapter.out.persistence.EventPersistenceAdapter}
 */
public interface LoadSeatPort {

    List<Seat> findByEventId(Long eventId);

    Optional<Seat> findSeatById(Long seatId);
}
