package com.ticketch.notificationservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 알림(Notification) 도메인 모델.
 *
 * 알림의 기본 정보와 상태를 관리하며, 생성 후 발송 또는 실패 상태를 기록할 수 있다.
 */
@Getter
@Builder
@AllArgsConstructor
public class Notification {

    /** 알림 고유 ID */
    private String id;

    /** 사용자 ID */
    private Long userId;

    /** 알림 유형 */
    private NotificationType type;

    /** 알림 제목 */
    private String title;

    /** 알림 본문 */
    private String body;

    /** 알림 상태 (발송 완료 또는 발송 실패) */
    private NotificationStatus status;

    /** 알림 발송 시간 */
    private LocalDateTime sentAt;

    /**
     * 새로운 알림을 생성한다.
     *
     * @param userId 사용자 ID
     * @param type 알림 유형
     * @param title 알림 제목
     * @param body 알림 본문
     * @return 생성된 알림 (id, status, sentAt은 null)
     */
    public static Notification create(Long userId, NotificationType type, String title, String body) {
        return Notification.builder()
                .id(null)
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .status(null)
                .sentAt(null)
                .build();
    }

    /**
     * 알림을 발송 완료로 표시한다.
     */
    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    /**
     * 알림을 발송 실패로 표시한다.
     */
    public void markFailed() {
        this.status = NotificationStatus.FAILED;
        this.sentAt = LocalDateTime.now();
    }
}
