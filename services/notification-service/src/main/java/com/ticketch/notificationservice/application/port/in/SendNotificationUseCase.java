package com.ticketch.notificationservice.application.port.in;

import com.ticketch.notificationservice.domain.model.NotificationType;

/**
 * 알림 발송 유스케이스 인터페이스.
 * 외부 입력(RabbitMQ, API 등)에서 호출되는 애플리케이션 경계.
 */
public interface SendNotificationUseCase {

    /**
     * 지정된 명령으로 알림을 발송한다.
     *
     * @param command 발송할 알림의 상세 정보
     */
    void send(SendNotificationCommand command);

    /**
     * 알림 발송 명령 데이터.
     *
     * @param userId 수신자 사용자 ID
     * @param type 알림 유형
     * @param reservationId 관련 예약 ID
     * @param amount 결제 금액 (선택)
     */
    record SendNotificationCommand(
            Long userId,
            NotificationType type,
            Long reservationId,
            Integer amount
    ) {}
}
