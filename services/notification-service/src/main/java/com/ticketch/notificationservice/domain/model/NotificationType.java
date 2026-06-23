package com.ticketch.notificationservice.domain.model;

/**
 * 알림 타입 열거형. 결제 완료, 결제 실패, 예약 만료 등의 알림 종류를 정의합니다.
 */
public enum NotificationType {
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    RESERVATION_EXPIRED
}
