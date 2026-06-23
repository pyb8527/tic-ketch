package com.ticketch.notificationservice.application.service;

import com.ticketch.notificationservice.application.port.in.SendNotificationUseCase;
import com.ticketch.notificationservice.application.port.out.SaveNotificationPort;
import com.ticketch.notificationservice.application.port.out.SendEmailPort;
import com.ticketch.notificationservice.domain.model.Notification;
import com.ticketch.notificationservice.domain.service.NotificationFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 알림 발송을 담당하는 애플리케이션 서비스.
 * 알림을 생성하고, 이메일로 발송한 후, 결과를 저장한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService implements SendNotificationUseCase {

    private final NotificationFactory notificationFactory;
    private final SendEmailPort sendEmailPort;
    private final SaveNotificationPort saveNotificationPort;

    /**
     * 알림을 발송한다.
     *
     * @param cmd 알림 발송 명령(userId, type, reservationId, amount)
     */
    @Override
    public void send(SendNotificationCommand cmd) {
        // 1. 도메인 팩토리로 알림 생성
        Notification n = notificationFactory.create(cmd.userId(), cmd.type(), cmd.reservationId(), cmd.amount());

        // 2. 이메일 발송 시도
        boolean ok = sendEmailPort.send(n);

        // 3. 발송 결과에 따라 상태 업데이트
        if (ok) {
            n.markSent();
        } else {
            n.markFailed();
        }

        // 4. 알림 저장
        saveNotificationPort.save(n);

        // 선택사항: 로그 기록
        log.info("Notification sent: userId={}, type={}, status={}", n.getUserId(), n.getType(), n.getStatus());
    }
}
