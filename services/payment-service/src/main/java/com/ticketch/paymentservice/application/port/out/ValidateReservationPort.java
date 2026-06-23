package com.ticketch.paymentservice.application.port.out;

/**
 * 예약 정보 조회 Output Port (Feign → reservation-service).
 * 외부 예약 서비스에서 예약 정보를 검증한다.
 */
public interface ValidateReservationPort {
	/**
	 * 예약 정보를 조회한다.
	 *
	 * @param reservationId 예약 ID
	 * @param userId 사용자 ID
	 * @return 예약 정보
	 */
	ReservationInfo getReservation(Long reservationId, Long userId);

	/**
	 * 예약 정보.
	 *
	 * @param seatId 좌석 ID
	 * @param eventId 이벤트 ID
	 * @param status 예약 상태
	 */
	record ReservationInfo(Long seatId, Long eventId, String status) {}
}
