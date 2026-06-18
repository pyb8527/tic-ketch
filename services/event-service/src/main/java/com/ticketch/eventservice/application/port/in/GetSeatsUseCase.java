package com.ticketch.eventservice.application.port.in;

import com.ticketch.eventservice.domain.model.Seat;

import java.util.List;

/**
 * [Input Port] 좌석 목록 조회 유스케이스.
 *
 * <p>구현체: {@link com.ticketch.eventservice.application.service.EventQueryService}
 * <p>Redis 캐시 우선 조회 → 캐시 미스 시 DB 조회 후 캐시 갱신.
 */
public interface GetSeatsUseCase {

    /**
     * 공연의 전체 좌석 목록 조회.
     * 결과는 {@code event:seats:{eventId}} Redis Hash에 60초 캐시된다.
     */
    List<Seat> getSeats(Long eventId);
}
