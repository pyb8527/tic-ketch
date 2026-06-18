package com.ticketch.eventservice.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * [Domain] 공연 도메인 모델.
 *
 * <p>인프라 의존성 없는 순수 Java 객체.
 * JPA Entity({@link com.ticketch.eventservice.adapter.out.persistence.EventJpaEntity})와 분리된다.
 */
@Getter
@Builder
public class Event {

    private Long id;
    private String title;
    private String venue;
    private LocalDateTime eventDate;
    private EventStatus status;
    private LocalDateTime createdAt;

    /** 공연 판매 상태 */
    public enum EventStatus {
        UPCOMING,   // 판매 예정
        ON_SALE,    // 판매 중
        SOLD_OUT,   // 매진
        ENDED       // 종료
    }
}
