package com.ticketch.reservationservice.application.port.out;

/**
 * 좌석 보유 캐시 해제 포트.
 * Redis에서 임시 예약 정보를 제거합니다.
 */
public interface ReleaseSeatCachePort {

    /**
     * 예약 캐시 삭제.
     * Redis에서 해당 예약의 임시 정보를 제거합니다.
     *
     * @param reservationId 예약 ID
     */
    void release(Long reservationId);
}
