package com.ticketch.reservationservice.adapter.out.redis;

import com.ticketch.reservationservice.application.port.out.ManageQueuePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * [Adapter] Redis Sorted Set 기반 대기열 관리 어댑터.
 *
 * <p>이벤트별 사용자 대기열을 Redis Sorted Set으로 관리합니다.
 * score가 낮을수록 높은 우선순위를 가집니다.
 */
@Component
@RequiredArgsConstructor
public class QueueRedisAdapter implements ManageQueuePort {

    private final StringRedisTemplate redisTemplate;

    /**
     * 사용자를 대기열에 추가합니다.
     *
     * <p>키: "queue:{eventId}"
     * score는 우선순위를 나타내며, 낮을수록 높은 우선순위입니다.
     *
     * @param eventId 이벤트 ID
     * @param userId 사용자 ID
     * @param score 우선순위 점수 (낮을수록 높은 우선순위)
     */
    @Override
    public void add(Long eventId, Long userId, long score) {
        String key = "queue:" + eventId;
        redisTemplate.opsForZSet().add(key, userId.toString(), (double) score);
    }

    /**
     * 대기열에서 사용자의 순위를 조회합니다.
     *
     * <p>키: "queue:{eventId}"
     * Redis의 rank는 0-based이므로 1을 더해 1-based 순위를 반환합니다.
     * 대기열에 없으면 -1을 반환합니다.
     *
     * @param eventId 이벤트 ID
     * @param userId 사용자 ID
     * @return 순위 (1-based) 또는 -1 (미존재)
     */
    @Override
    public long rank(Long eventId, Long userId) {
        String key = "queue:" + eventId;
        Long rank = redisTemplate.opsForZSet().rank(key, userId.toString());
        return rank == null ? -1 : rank + 1;
    }

    /**
     * 대기열의 크기를 조회합니다.
     *
     * <p>키: "queue:{eventId}"
     * 대기 중인 사용자 수를 반환합니다.
     *
     * @param eventId 이벤트 ID
     * @return 대기 중인 사용자 수
     */
    @Override
    public long size(Long eventId) {
        String key = "queue:" + eventId;
        Long size = redisTemplate.opsForZSet().zCard(key);
        return size == null ? 0 : size;
    }
}
