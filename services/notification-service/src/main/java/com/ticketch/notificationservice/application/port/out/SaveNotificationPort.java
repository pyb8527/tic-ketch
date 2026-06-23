package com.ticketch.notificationservice.application.port.out;

import com.ticketch.notificationservice.domain.model.Notification;

/**
 * 알림 저장 아웃바운드 포트.
 * 도메인 알림 객체를 영속성 계층에 저장하는 책임을 정의한다.
 */
public interface SaveNotificationPort {

    /**
     * 알림을 저장소에 저장한다.
     *
     * @param notification 저장할 알림 도메인 객체
     * @return 저장된 알림 (ID 포함)
     */
    Notification save(Notification notification);
}
