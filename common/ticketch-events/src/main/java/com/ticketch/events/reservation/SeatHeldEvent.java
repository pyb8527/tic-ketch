package com.ticketch.events.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 좌석 임시 선점 완료 이벤트 DTO.
 *
 * <p>Reservation Service → RabbitMQ → Event Service
 * <ul>
 *   <li>Event Service: 좌석 상태를 HELD로 변경 + SSE로 클라이언트에 푸시</li>
 * </ul>
 *
 * <p>Redis TTL 만료 시 {@link SeatReleasedEvent}로 후속 처리됨.
 * <p>routing key: {@code seat.held}
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatHeldEvent {

    private Long reservationId;
    private Long seatId;
    private Long eventId;
    private Long userId;
    /** Redis TTL 만료 시각 (선점 후 5분) */
    private LocalDateTime expiresAt;
}
