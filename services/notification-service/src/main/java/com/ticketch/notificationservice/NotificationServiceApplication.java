package com.ticketch.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Notification Service 메인 클래스.
 *
 * <p>담당: 결제 이벤트 수신 → 알림 발송 → MongoDB 이력
 * <p>포트: 8085 (config-repo/notification-service.yml)
 */
@SpringBootApplication(scanBasePackages = {"com.ticketch.notificationservice", "com.ticketch.common"})
@EnableDiscoveryClient
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
