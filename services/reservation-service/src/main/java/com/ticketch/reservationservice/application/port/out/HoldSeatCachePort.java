package com.ticketch.reservationservice.application.port.out;

import com.ticketch.reservationservice.domain.model.Reservation;
import java.time.Duration;

/**
 * 좌석 보유 캐시 포트.
 * Redis를 이용하여 임시 예약 정보를 캐싱합니다.
 */
public interface HoldSeatCachePort {

    /**
     * 예약을 Redis Hash로 캐시 저장.
     * TTL을 설정하여 자동 만료됩니다.
     *
     * @param reservation 캐시할 예약 객체
     * @param ttl 캐시 유지 시간
     */
    void hold(Reservation reservation, Duration ttl);
}
