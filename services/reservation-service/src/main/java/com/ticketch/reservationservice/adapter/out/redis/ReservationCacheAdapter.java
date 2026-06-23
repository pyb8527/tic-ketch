package com.ticketch.reservationservice.adapter.out.redis;

import com.ticketch.reservationservice.application.port.out.HoldSeatCachePort;
import com.ticketch.reservationservice.application.port.out.ReleaseSeatCachePort;
import com.ticketch.reservationservice.domain.model.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * [Adapter] Redis Hash 기반 예약 캐시 어댑터.
 *
 * <p>임시 예약 정보를 Redis Hash로 저장하고, TTL을 통해 자동으로 만료됩니다.
 * 예약 ID를 키로 사용하며, reservationId, seatId, eventId, userId를 필드로 저장합니다.
 */
@Component
@RequiredArgsConstructor
public class ReservationCacheAdapter implements HoldSeatCachePort, ReleaseSeatCachePort {

    private final StringRedisTemplate redisTemplate;

    /**
     * 예약을 Redis Hash로 캐시 저장합니다.
     *
     * <p>키: "reservation:temp:{reservationId}"
     * 필드: reservationId, seatId, eventId, userId (모두 String 값)
     * TTL을 설정하여 자동 만료됩니다.
     *
     * @param reservation 캐시할 예약 객체
     * @param ttl 캐시 유지 시간
     */
    @Override
    public void hold(Reservation reservation, Duration ttl) {
        String key = "reservation:temp:" + reservation.getId();
        Map<String, String> data = new HashMap<>();
        data.put("reservationId", reservation.getId().toString());
        data.put("seatId", reservation.getSeatId().toString());
        data.put("eventId", reservation.getEventId().toString());
        data.put("userId", reservation.getUserId().toString());

        redisTemplate.opsForHash().putAll(key, data);
        redisTemplate.expire(key, ttl);
    }

    /**
     * 예약 캐시를 Redis에서 삭제합니다.
     *
     * <p>키: "reservation:temp:{reservationId}"
     * 해당 예약의 임시 정보를 제거합니다.
     *
     * @param reservationId 예약 ID
     */
    @Override
    public void release(Long reservationId) {
        String key = "reservation:temp:" + reservationId;
        redisTemplate.delete(key);
    }
}
