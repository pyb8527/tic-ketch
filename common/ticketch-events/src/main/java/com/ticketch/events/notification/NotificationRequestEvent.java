package com.ticketch.events.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 발송 요청 이벤트 DTO.
 *
 * <p>Payment/Reservation Service → RabbitMQ → Notification Service
 * <p>Notification Service는 이 이벤트를 소비하여 이메일(또는 푸시)을 발송한다.
 *
 * <p>routing key: {@code notification.request}
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestEvent {

    private Long userId;
    private NotificationType type;
    private String title;
    private String body;

    /** 알림 종류 — Notification Service에서 템플릿 선택에 사용 */
    public enum NotificationType {
        PAYMENT_COMPLETED,
        PAYMENT_FAILED,
        RESERVATION_EXPIRED,
        RESERVATION_CONFIRMED,
        RESERVATION_CANCELLED
    }
}
