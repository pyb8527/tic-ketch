package com.ticketch.eventservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Event Service 메인 클래스.
 *
 * <p>담당: 공연·좌석 CRUD, 실시간 좌석 상태 SSE, Redis 캐시
 * <p>포트: 8082 (config-repo/event-service.yml)
 */
@SpringBootApplication(scanBasePackages = {"com.ticketch.eventservice", "com.ticketch.common"})
public class EventServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventServiceApplication.class, args);
    }
}
