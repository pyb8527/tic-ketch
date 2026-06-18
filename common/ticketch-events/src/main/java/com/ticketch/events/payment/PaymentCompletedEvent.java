package com.ticketch.events.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 결제 완료 이벤트 DTO.
 *
 * <p>Payment Service → RabbitMQ → Reservation Service, Notification Service
 * <ul>
 *   <li>Reservation Service: 예약 상태를 CONFIRMED로 변경</li>
 *   <li>Notification Service: 결제 완료 이메일 발송</li>
 * </ul>
 *
 * <p>routing key: {@code payment.completed}
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {

    private Long paymentId;
    private Long reservationId;
    private Long userId;
    private Long seatId;
    private Long eventId;
    private Integer amount;
    private LocalDateTime paidAt;
}
