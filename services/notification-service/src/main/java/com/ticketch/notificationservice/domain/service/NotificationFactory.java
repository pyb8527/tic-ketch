package com.ticketch.notificationservice.domain.service;

import com.ticketch.notificationservice.domain.model.Notification;
import com.ticketch.notificationservice.domain.model.NotificationType;
import org.springframework.stereotype.Component;

/**
 * 알림 생성 팩토리.
 *
 * 예약 ID와 금액 등의 정보를 기반으로 적절한 제목과 본문을 생성하여 알림을 만든다.
 */
@Component
public class NotificationFactory {

    /**
     * 알림 유형과 상세 정보를 기반으로 알림을 생성한다.
     *
     * @param userId 사용자 ID
     * @param type 알림 유형
     * @param reservationId 예약 ID
     * @param amount 금액 (결제 완료 시에만 사용, null 가능)
     * @return 생성된 알림
     */
    public Notification create(Long userId, NotificationType type, Long reservationId, Integer amount) {
        String title;
        String body;

        switch (type) {
            case PAYMENT_COMPLETED -> {
                title = "결제 완료";
                body = "예약 #" + reservationId + " 결제가 완료되었습니다."
                        + (amount != null ? " 금액: " + amount + "원" : "");
            }
            case PAYMENT_FAILED -> {
                title = "결제 실패";
                body = "예약 #" + reservationId + " 결제가 실패했습니다.";
            }
            case RESERVATION_EXPIRED -> {
                title = "예약 만료";
                body = "예약 #" + reservationId + " 가 만료되었습니다.";
            }
            default -> throw new IllegalArgumentException("Unsupported notification type: " + type);
        }

        return Notification.create(userId, type, title, body);
    }
}
