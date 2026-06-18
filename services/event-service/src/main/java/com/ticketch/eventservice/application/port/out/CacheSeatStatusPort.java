package com.ticketch.eventservice.application.port.out;

import com.ticketch.eventservice.domain.model.Seat;

import java.util.List;
import java.util.Optional;

/**
 * [Output Port] 좌석 상태 Redis 캐시 인터페이스.
 *
 * <p>구현체: {@link com.ticketch.eventservice.adapter.out.redis.SeatCacheRedisAdapter}
 * <p>Redis 키: {@code event:seats:{eventId}} — Hash(seatId → status), TTL 60초
 */
public interface CacheSeatStatusPort {

    /** 공연의 전체 좌석 상태를 Redis Hash로 캐시 */
    void cacheSeats(Long eventId, List<Seat> seats);

    /** Redis에서 좌석 목록 조회 (캐시 히트 시 사용) */
    Optional<List<Seat>> getCachedSeats(Long eventId);

    /**
     * 단일 좌석 상태를 Redis Hash에서 업데이트.
     * 전체 캐시 무효화 없이 변경된 좌석만 갱신한다.
     */
    void updateSeatStatus(Long eventId, Long seatId, Seat.SeatStatus newStatus);

    /** 공연 전체 좌석 캐시 무효화 */
    void evict(Long eventId);
}
