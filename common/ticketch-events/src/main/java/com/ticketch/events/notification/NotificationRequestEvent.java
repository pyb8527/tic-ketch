package com.ticketch.events.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestEvent {

    private Long userId;
    private NotificationType type;
    private String title;
    private String body;

    public enum NotificationType {
        PAYMENT_COMPLETED,
        PAYMENT_FAILED,
        RESERVATION_EXPIRED,
        RESERVATION_CONFIRMED,
        RESERVATION_CANCELLED
    }
}
