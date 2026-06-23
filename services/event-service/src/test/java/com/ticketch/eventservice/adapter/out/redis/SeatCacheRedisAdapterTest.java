package com.ticketch.eventservice.adapter.out.redis;

import com.ticketch.eventservice.domain.model.Seat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(SeatCacheRedisAdapterTest.class);

    @Autowired SeatCacheRedisAdapter seatCacheRedisAdapter;
    @Autowired StringRedisTemplate redisTemplate;

    private static final Long EVENT_ID = 9999L; // 테스트 전용 eventId

    @BeforeEach
    void clearCache() {
        // 테스트 간 격리 — 테스트 전용 키 초기화
        seatCacheRedisAdapter.evict(EVENT_ID);
        log.info("🧹 [준비] eventId={} 테스트 캐시 초기화 완료", EVENT_ID);
    }

    @Test
    @DisplayName("좌석 목록 캐시 저장 후 조회 시 같은 수의 좌석이 반환되어야 한다")
    void cacheAndGet() {
        log.info("──── [캐시 저장→조회] 시작 ────");
        List<Seat> seats = List.of(
                Seat.builder().id(1L).eventId(EVENT_ID).status(Seat.SeatStatus.AVAILABLE).build(),
                Seat.builder().id(2L).eventId(EVENT_ID).status(Seat.SeatStatus.HELD).build()
        );
        log.info("  💾 좌석 {}개 캐시 저장 (eventId={})", seats.size(), EVENT_ID);

        seatCacheRedisAdapter.cacheSeats(EVENT_ID, seats);
        Optional<List<Seat>> cached = seatCacheRedisAdapter.getCachedSeats(EVENT_ID);

        log.info("  🔍 캐시 조회 결과: present={}, size={}",
                cached.isPresent(), cached.map(List::size).orElse(0));

        assertThat(cached).isPresent();
        assertThat(cached.get()).hasSize(2);
        log.info("  ✅ 저장한 좌석 수와 조회된 좌석 수 일치");
    }

    @Test
    @DisplayName("캐시 미스 시 Optional.empty 반환")
    void cacheMiss() {
        log.info("──── [캐시 미스] 시작 ────");
        Optional<List<Seat>> result = seatCacheRedisAdapter.getCachedSeats(99999L);
        log.info("  🔍 캐시 없는 eventId=99999 조회 결과: empty={}", result.isEmpty());
        assertThat(result).isEmpty();
        log.info("  ✅ 캐시 미스 시 Optional.empty 반환 확인");
    }

    @Test
    @DisplayName("단일 좌석 상태 부분 갱신 후 변경된 상태가 조회되어야 한다")
    void partialUpdate() {
        log.info("──── [좌석 상태 부분 갱신] 시작 ────");
        seatCacheRedisAdapter.cacheSeats(EVENT_ID, List.of(
                Seat.builder().id(1L).eventId(EVENT_ID).status(Seat.SeatStatus.AVAILABLE).build()
        ));
        log.info("  💾 좌석 id=1 AVAILABLE 상태로 캐시 저장");

        // AVAILABLE → HELD 부분 갱신
        seatCacheRedisAdapter.updateSeatStatus(EVENT_ID, 1L, Seat.SeatStatus.HELD);
        log.info("  🔄 좌석 id=1 상태 갱신: AVAILABLE → HELD");

        Optional<List<Seat>> cached = seatCacheRedisAdapter.getCachedSeats(EVENT_ID);
        log.info("  🔍 갱신 후 조회된 좌석 상태: {}",
                cached.map(s -> s.get(0).getStatus()).orElse(null));
        assertThat(cached).isPresent();
        assertThat(cached.get().get(0).getStatus()).isEqualTo(Seat.SeatStatus.HELD);
        log.info("  ✅ 부분 갱신된 HELD 상태가 정확히 반영됨");
    }

    @Test
    @DisplayName("캐시 무효화 후 조회 시 Optional.empty 반환")
    void evict() {
        log.info("──── [캐시 무효화] 시작 ────");
        seatCacheRedisAdapter.cacheSeats(EVENT_ID, List.of(
                Seat.builder().id(1L).eventId(EVENT_ID).status(Seat.SeatStatus.AVAILABLE).build()
        ));
        log.info("  💾 좌석 1개 캐시 저장 (eventId={})", EVENT_ID);

        seatCacheRedisAdapter.evict(EVENT_ID);
        log.info("  🗑️  eventId={} 캐시 무효화 실행", EVENT_ID);

        Optional<List<Seat>> cached = seatCacheRedisAdapter.getCachedSeats(EVENT_ID);
        log.info("  🔍 무효화 후 조회 결과: empty={}", cached.isEmpty());
        assertThat(cached).isEmpty();
        log.info("  ✅ 무효화 후 캐시가 비어있음 확인");
    }
}
