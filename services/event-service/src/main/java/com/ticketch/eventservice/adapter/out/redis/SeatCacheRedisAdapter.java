package com.ticketch.eventservice.adapter.out.redis;

import com.ticketch.eventservice.application.port.out.CacheSeatStatusPort;
import com.ticketch.eventservice.domain.model.Seat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * [Redis Adapter] 좌석 상태 캐시 구현체.
 *
 * <p>구현 포트: {@link CacheSeatStatusPort}
 *
 * <p>Redis 키 구조:
 * <pre>
 * Key  : event:seats:{eventId}   (Hash)
 * Field: seatId (String)
 * Value: status (AVAILABLE / HELD / SOLD)
 * TTL  : 60초
 * </pre>
 *
 * <p>Hash를 사용하는 이유:
 * 단일 좌석 상태 변경 시 전체 캐시를 무효화하지 않고 해당 필드만 업데이트할 수 있어
 * Cache Stampede를 줄이고 Redis 부하를 최소화한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatCacheRedisAdapter implements CacheSeatStatusPort {

    private static final String SEAT_CACHE_KEY = "event:seats:";
    private static final Duration TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;

    /** 공연 전체 좌석 목록을 Redis Hash로 캐시 */
    @Override
    public void cacheSeats(Long eventId, List<Seat> seats) {
        if (seats.isEmpty()) return;

        String key = SEAT_CACHE_KEY + eventId;

        // 모든 좌석을 Hash로 한 번에 저장 (O(n) 단일 명령)
        Map<String, String> seatMap = seats.stream()
                .collect(Collectors.toMap(
                        s -> String.valueOf(s.getId()),
                        s -> s.getStatus().name()
                ));

        redisTemplate.opsForHash().putAll(key, seatMap);
        redisTemplate.expire(key, TTL);
        log.debug("좌석 캐시 저장 eventId={}, count={}", eventId, seats.size());
    }

    /** Redis Hash에서 공연 좌석 목록 조회 */
    @Override
    public Optional<List<Seat>> getCachedSeats(Long eventId) {
        String key = SEAT_CACHE_KEY + eventId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        if (entries.isEmpty()) {
            log.debug("좌석 캐시 미스 eventId={}", eventId);
            return Optional.empty();
        }

        // Redis Hash 값 → Seat 도메인 객체 변환 (id, status만 복원)
        List<Seat> seats = entries.entrySet().stream()
                .map(e -> Seat.builder()
                        .id(Long.parseLong((String) e.getKey()))
                        .status(Seat.SeatStatus.valueOf((String) e.getValue()))
                        .eventId(eventId)
                        .build())
                .collect(Collectors.toList());

        log.debug("좌석 캐시 히트 eventId={}, count={}", eventId, seats.size());
        return Optional.of(seats);
    }

    /**
     * 단일 좌석 상태만 Hash에서 업데이트 (전체 캐시 무효화 없이 부분 갱신).
     * 좌석 상태 변경 시 호출된다.
     */
    @Override
    public void updateSeatStatus(Long eventId, Long seatId, Seat.SeatStatus newStatus) {
        String key = SEAT_CACHE_KEY + eventId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.opsForHash().put(key, String.valueOf(seatId), newStatus.name());
            log.debug("좌석 캐시 부분 갱신 eventId={}, seatId={}, status={}", eventId, seatId, newStatus);
        }
    }

    /** 공연 전체 좌석 캐시 삭제 */
    @Override
    public void evict(Long eventId) {
        redisTemplate.delete(SEAT_CACHE_KEY + eventId);
        log.debug("좌석 캐시 무효화 eventId={}", eventId);
    }
}
