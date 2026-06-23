package com.ticketch.notificationservice.application.port.out;

import com.ticketch.notificationservice.domain.model.Notification;

/**
 * 이메일 발송 아웃바운드 포트.
 * 알림을 실제 이메일로 발송하는 책임을 정의한다.
 */
public interface SendEmailPort {

    /**
     * 알림을 이메일로 발송한다.
     *
     * @param notification 발송할 알림 도메인 객체
     * @return true 발송 성공, false 발송 실패
     */
    boolean send(Notification notification);
}
