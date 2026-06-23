package com.ticketch.notificationservice.adapter.out.email;

import com.ticketch.notificationservice.application.port.out.SendEmailPort;
import com.ticketch.notificationservice.domain.model.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 실제 이메일 발송 없이 콘솔에 출력하는 개발용 목업 어댑터.
 */
@Component
@Slf4j
public class MockEmailAdapter implements SendEmailPort {

    /**
     * 알림을 목업 이메일로 발송한다.
     *
     * @param notification 발송할 알림 도메인 객체
     * @return 항상 true (발송 성공)
     */
    @Override
    public boolean send(Notification notification) {
        log.info("📧 [MOCK EMAIL] userId={} type={} | {} - {}",
            notification.getUserId(),
            notification.getType(),
            notification.getTitle(),
            notification.getBody());
        return true;
    }
}
