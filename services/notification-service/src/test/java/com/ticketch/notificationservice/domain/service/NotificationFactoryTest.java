package com.ticketch.notificationservice.domain.service;

import com.ticketch.notificationservice.domain.model.Notification;
import com.ticketch.notificationservice.domain.model.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NotificationFactory 단위 테스트.
 * Mockito 불필요 — 순수 도메인 로직만 검증한다.
 */
class NotificationFactoryTest {

    private final NotificationFactory factory = new NotificationFactory();

    @Test
    @DisplayName("결제 완료 알림이 올바른 제목·본문·타입·사용자 ID로 생성된다")
    void 결제완료_알림_생성() {
        // when
        Notification n = factory.create(1L, NotificationType.PAYMENT_COMPLETED, 5L, 50000);

        // then
        assertThat(n.getTitle()).contains("결제 완료");
        assertThat(n.getBody()).contains("5");
        assertThat(n.getBody()).contains("50000");
        assertThat(n.getType()).isEqualTo(NotificationType.PAYMENT_COMPLETED);
        assertThat(n.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("결제 실패 알림이 올바른 제목으로 생성된다")
    void 결제실패_알림_생성() {
        // when
        Notification n = factory.create(1L, NotificationType.PAYMENT_FAILED, 5L, null);

        // then
        assertThat(n.getTitle()).contains("결제 실패");
    }
}
