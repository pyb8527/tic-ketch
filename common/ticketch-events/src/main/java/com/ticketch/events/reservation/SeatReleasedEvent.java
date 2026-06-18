package com.ticketch.events.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 좌석 선점 해제 이벤트 DTO.
 *
 * <p>Reservation Service → RabbitMQ → Event Service
 * <ul>
 *   <li>Event Service: 좌석 상태를 AVAILABLE로 복원 + SSE 푸시</li>
 * </ul>
 *
 * <p>해제 사유에 따라 처리 방식이 달라질 수 있음:
 * <ul>
 *   <li>EXPIRED: TTL 만료 — Spring Scheduler 또는 Redis keyspace notification</li>
 *   <li>CANCELLED: 사용자 직접 취소</li>
 *   <li>PAYMENT_FAILED: 결제 실패로 인한 자동 해제</li>
 * </ul>
 *
 * <p>routing key: {@code seat.released}
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatReleasedEvent {

    private Long reservationId;
    private Long seatId;
    private Long eventId;
    private ReleaseReason reason;

    /** 좌석 해제 사유 */
    public enum ReleaseReason {
        EXPIRED,        // Redis TTL 만료
        CANCELLED,      // 사용자 직접 취소
        PAYMENT_FAILED  // 결제 실패
    }
}
