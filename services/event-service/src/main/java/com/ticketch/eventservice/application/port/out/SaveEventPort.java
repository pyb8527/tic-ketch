package com.ticketch.eventservice.application.port.out;

import com.ticketch.eventservice.domain.model.Event;
import com.ticketch.eventservice.domain.model.Seat;

import java.util.List;

/**
 * [Output Port] 공연·좌석 저장 인터페이스.
 *
 * <p>구현체: {@link com.ticketch.eventservice.adapter.out.persistence.EventPersistenceAdapter}
 */
public interface SaveEventPort {

    Long saveEvent(Event event);

    List<Long> saveSeats(List<Seat> seats);
}
