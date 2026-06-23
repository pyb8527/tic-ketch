package com.ticketch.notificationservice.adapter.out.persistence;

import com.ticketch.notificationservice.application.port.out.SaveNotificationPort;
import com.ticketch.notificationservice.domain.model.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 알림 저장 아웃바운드 어댑터 (MongoDB).
 *
 * SaveNotificationPort를 구현하여 도메인 모델 Notification을 MongoDB에 저장한다.
 * NotificationMongoRepository를 통해 영속성 작업을 수행한다.
 */
@Component
@RequiredArgsConstructor
public class NotificationMongoAdapter implements SaveNotificationPort {

    private final NotificationMongoRepository notificationMongoRepository;

    /**
     * 알림을 MongoDB에 저장한다.
     *
     * @param notification 저장할 알림 도메인 객체
     * @return 저장된 알림 (ID 포함)
     */
    @Override
    public Notification save(Notification notification) {
        return notificationMongoRepository.save(NotificationDocument.fromDomain(notification)).toDomain();
    }
}
