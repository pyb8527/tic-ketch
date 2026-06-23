package com.ticketch.notificationservice.application.service;

import com.ticketch.notificationservice.application.port.in.SendNotificationUseCase;
import com.ticketch.notificationservice.application.port.out.SaveNotificationPort;
import com.ticketch.notificationservice.application.port.out.SendEmailPort;
import com.ticketch.notificationservice.domain.model.Notification;
import com.ticketch.notificationservice.domain.model.NotificationStatus;
import com.ticketch.notificationservice.domain.model.NotificationType;
import com.ticketch.notificationservice.domain.service.NotificationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NotificationService 단위 테스트.
 * Spring 컨텍스트 없이 순수 JUnit5 + Mockito 로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceTest.class);

    @Mock
    SendEmailPort sendEmailPort;

    @Mock
    SaveNotificationPort saveNotificationPort;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        // NotificationFactory는 실제 객체 사용 (도메인 로직 그대로 검증)
        notificationService = new NotificationService(new NotificationFactory(), sendEmailPort, saveNotificationPort);
    }

    @Test
    @DisplayName("이메일 발송 성공 시 SENT 상태로 저장된다")
    void 이메일_발송_성공시_SENT로_저장() {
        // given
        log.info("[시나리오] 이메일 발송 성공 → SENT 상태로 저장 검증");
        when(sendEmailPort.send(any())).thenReturn(true);

        // when
        notificationService.send(
                new SendNotificationUseCase.SendNotificationCommand(1L, NotificationType.PAYMENT_COMPLETED, 5L, 50000)
        );

        // then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(saveNotificationPort).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT);
        verify(sendEmailPort).send(any());
        log.info("[결과] 저장된 알림 상태 = {}", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("이메일 발송 실패 시 FAILED 상태로 저장된다")
    void 이메일_발송_실패시_FAILED로_저장() {
        // given
        log.info("[시나리오] 이메일 발송 실패 → FAILED 상태로 저장 검증");
        when(sendEmailPort.send(any())).thenReturn(false);

        // when
        notificationService.send(
                new SendNotificationUseCase.SendNotificationCommand(1L, NotificationType.PAYMENT_FAILED, 5L, null)
        );

        // then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(saveNotificationPort).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
        verify(sendEmailPort).send(any());
        log.info("[결과] 저장된 알림 상태 = {}", captor.getValue().getStatus());
    }
}
