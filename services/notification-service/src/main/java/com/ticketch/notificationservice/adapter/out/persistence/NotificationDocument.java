package com.ticketch.notificationservice.adapter.out.persistence;

import com.ticketch.notificationservice.domain.model.Notification;
import com.ticketch.notificationservice.domain.model.NotificationStatus;
import com.ticketch.notificationservice.domain.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 알림 MongoDB 문서 엔티티.
 *
 * 도메인 모델 Notification을 MongoDB 컬렉션에 저장하기 위한 문서 클래스이다.
 * toDomain()과 fromDomain()을 통해 도메인 모델과의 변환을 수행한다.
 */
@Document(collection = "notifications")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDocument {

    /** MongoDB 고유 ID */
    @Id
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
     * 문서를 도메인 모델로 변환한다.
     *
     * @return 변환된 Notification 도메인 모델
     */
    public Notification toDomain() {
        return Notification.builder()
                .id(this.id)
                .userId(this.userId)
                .type(this.type)
                .title(this.title)
                .body(this.body)
                .status(this.status)
                .sentAt(this.sentAt)
                .build();
    }

    /**
     * 도메인 모델을 문서로 변환한다.
     *
     * @param notification 변환할 Notification 도메인 모델
     * @return 변환된 NotificationDocument
     */
    public static NotificationDocument fromDomain(Notification notification) {
        return NotificationDocument.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .title(notification.getTitle())
                .body(notification.getBody())
                .status(notification.getStatus())
                .sentAt(notification.getSentAt())
                .build();
    }
}
