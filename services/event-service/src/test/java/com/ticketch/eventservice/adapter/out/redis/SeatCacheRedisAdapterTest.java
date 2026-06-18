package com.ticketch.eventservice.adapter.out.redis;

import com.ticketch.eventservice.domain.model.Seat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [통합 테스트] SeatCacheRedisAdapter — 실행 중인 Redis 컨테이너로 검증.
 *
 * <p>docker-compose로 실행 중인 Redis(:6379)를 사용한다.
 * 테스트 격리를 위해 각 테스트 전에 해당 공연 캐시를 초기화한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class SeatCacheRedisAdapterTest {

    @Autowired SeatCacheRedisAdapter seatCacheRedisAdapter;
    @Autowired StringRedisTemplate redisTemplate;

    private static final Long EVENT_ID = 9999L; // 테스트 전용 eventId

    @BeforeEach
    void clearCache() {
        // 테스트 간 격리 — 테스트 전용 키 초기화
        seatCacheRedisAdapter.evict(EVENT_ID);
    }

    @Test
    @DisplayName("좌석 목록 캐시 저장 후 조회 시 같은 수의 좌석이 반환되어야 한다")
    void cacheAndGet() {
        List<Seat> seats = List.of(
                Seat.builder().id(1L).eventId(EVENT_ID).status(Seat.SeatStatus.AVAILABLE).build(),
                Seat.builder().id(2L).eventId(EVENT_ID).status(Seat.SeatStatus.HELD).build()
        );

        seatCacheRedisAdapter.cacheSeats(EVENT_ID, seats);
        Optional<List<Seat>> cached = seatCacheRedisAdapter.getCachedSeats(EVENT_ID);

        assertThat(cached).isPresent();
        assertThat(cached.get()).hasSize(2);
    }

    @Test
    @DisplayName("캐시 미스 시 Optional.empty 반환")
    void cacheMiss() {
        Optional<List<Seat>> result = seatCacheRedisAdapter.getCachedSeats(99999L);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("단일 좌석 상태 부분 갱신 후 변경된 상태가 조회되어야 한다")
    void partialUpdate() {
        seatCacheRedisAdapter.cacheSeats(EVENT_ID, List.of(
                Seat.builder().id(1L).eventId(EVENT_ID).status(Seat.SeatStatus.AVAILABLE).build()
        ));

        // AVAILABLE → HELD 부분 갱신
        seatCacheRedisAdapter.updateSeatStatus(EVENT_ID, 1L, Seat.SeatStatus.HELD);

        Optional<List<Seat>> cached = seatCacheRedisAdapter.getCachedSeats(EVENT_ID);
        assertThat(cached).isPresent();
        assertThat(cached.get().get(0).getStatus()).isEqualTo(Seat.SeatStatus.HELD);
    }

    @Test
    @DisplayName("캐시 무효화 후 조회 시 Optional.empty 반환")
    void evict() {
        seatCacheRedisAdapter.cacheSeats(EVENT_ID, List.of(
                Seat.builder().id(1L).eventId(EVENT_ID).status(Seat.SeatStatus.AVAILABLE).build()
        ));

        seatCacheRedisAdapter.evict(EVENT_ID);

        assertThat(seatCacheRedisAdapter.getCachedSeats(EVENT_ID)).isEmpty();
    }
}
