package com.ticketch.reservationservice.application.port.out;

/**
 * 좌석 검증 포트.
 * Event Service와의 통신으로 좌석 가용성을 확인합니다.
 */
public interface ValidateSeatPort {

    /**
     * 좌석 가용 여부 확인.
     * status이 AVAILABLE인지 확인합니다.
     *
     * @param eventId 이벤트 ID
     * @param seatId 좌석 ID
     * @return 가용 여부
     */
    boolean isAvailable(Long eventId, Long seatId);
}
