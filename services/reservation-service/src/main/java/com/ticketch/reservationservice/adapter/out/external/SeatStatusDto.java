package com.ticketch.reservationservice.adapter.out.external;

/**
 * Event Service에서 반환한 좌석 상태 정보.
 * event-service SeatResponse와 필드 호환하며 Jackson record 역직렬화 지원.
 */
public record SeatStatusDto(
    Long id,
    String rowName,
    Integer seatNumber,
    String status
) {
}
