package com.ticketch.eventservice.application.service;

import com.ticketch.common.exception.BusinessException;
import com.ticketch.common.exception.ErrorCode;
import com.ticketch.eventservice.application.port.in.GetEventUseCase;
import com.ticketch.eventservice.application.port.in.GetSeatsUseCase;
import com.ticketch.eventservice.application.port.out.CacheSeatStatusPort;
import com.ticketch.eventservice.application.port.out.LoadEventPort;
import com.ticketch.eventservice.application.port.out.LoadSeatPort;
import com.ticketch.eventservice.domain.model.Event;
import com.ticketch.eventservice.domain.model.Seat;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * [Application Service] 공연·좌석 조회 유스케이스 구현체.
 *
 * <p>좌석 조회는 Redis 캐시를 우선 사용하고, 캐시 미스 시 DB를 조회 후 캐시에 저장한다.
 * (Cache-Aside 패턴)
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EventQueryService implements GetEventUseCase, GetSeatsUseCase {

    private final LoadEventPort loadEventPort;
    private final LoadSeatPort loadSeatPort;
    private final CacheSeatStatusPort cacheSeatStatusPort;

    @Override
    public Page<Event> getEvents(Pageable pageable) {
        return loadEventPort.findAll(pageable);
    }

    @Override
    public Event getEvent(Long eventId) {
        return loadEventPort.findEventById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
    }

    /**
     * 좌석 목록 조회 — Cache-Aside 패턴.
     *
     * <ol>
     *   <li>Redis에서 캐시 조회</li>
     *   <li>캐시 미스: DB 조회 → Redis 캐시 갱신</li>
     *   <li>캐시 히트: 바로 반환</li>
     * </ol>
     */
    @Override
    public List<Seat> getSeats(Long eventId) {
        // 1. Redis 캐시 조회
        return cacheSeatStatusPort.getCachedSeats(eventId)
                .orElseGet(() -> {
                    // 2. 캐시 미스: DB 조회 후 캐시 갱신
                    List<Seat> seats = loadSeatPort.findByEventId(eventId);
                    cacheSeatStatusPort.cacheSeats(eventId, seats);
                    return seats;
                });
    }
}
