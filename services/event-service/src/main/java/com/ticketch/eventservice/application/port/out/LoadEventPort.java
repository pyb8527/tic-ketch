package com.ticketch.eventservice.application.port.out;

import com.ticketch.eventservice.domain.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * [Output Port] 공연 조회 인터페이스.
 *
 * <p>구현체: {@link com.ticketch.eventservice.adapter.out.persistence.EventPersistenceAdapter}
 */
public interface LoadEventPort {

    Page<Event> findAll(Pageable pageable);

    Optional<Event> findEventById(Long eventId);
}
