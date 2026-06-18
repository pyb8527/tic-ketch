package com.ticketch.eventservice.domain.model;

import lombok.Builder;
import lombok.Getter;

/**
 * [Domain] 좌석 도메인 모델.
 *
 * <p>좌석 상태(AVAILABLE / HELD / SOLD)는 Reservation Service의 분산락 및
 * Redis TTL에 의해 관리되며, Event Service는 SSE를 통해 클라이언트에 상태 변경을 푸시한다.
 *
 * <p>{@code version} 필드는 JPA Optimistic Lock 용도이며,
 * Reservation Service의 Redisson 분산락과 2중으로 동시성을 보호한다.
 */
@Getter
@Builder
public class Seat {

    private Long id;
    private Long eventId;
    private Long seatGradeId;
    private String rowName;
    private Integer seatNumber;
    private SeatStatus status;
    /** JPA Optimistic Lock 버전 — 동시 상태 변경 충돌 감지 */
    private Long version;

    /** 좌석 예약 상태 */
    public enum SeatStatus {
        AVAILABLE,  // 예약 가능
        HELD,       // 임시 선점 중 (Redis TTL 5분)
        SOLD        // 판매 완료
    }
}
