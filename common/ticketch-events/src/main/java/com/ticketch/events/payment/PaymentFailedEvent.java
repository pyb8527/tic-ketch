package com.ticketch.events.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 결제 실패 이벤트 DTO.
 *
 * <p>Payment Service → RabbitMQ → Reservation Service, Notification Service
 * <ul>
 *   <li>Reservation Service: 좌석 선점 해제 + 예약 상태를 CANCELLED로 변경</li>
 *   <li>Notification Service: 결제 실패 알림 발송</li>
 * </ul>
 *
 * <p>DLQ 설정: 3회 재시도 후 {@code payment.dlq}로 이동
 * <p>routing key: {@code payment.failed}
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent {

    private Long paymentId;
    private Long reservationId;
    private Long userId;
    private Long seatId;
    private Long eventId;
    /** 결제 실패 사유 (목업 PG 또는 실제 PG 에러 메시지) */
    private String reason;
    private LocalDateTime failedAt;
}
