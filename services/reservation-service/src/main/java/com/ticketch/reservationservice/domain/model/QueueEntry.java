package com.ticketch.reservationservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * [Domain] 대기열 항목 도메인 모델.
 *
 * <p>Redis Sorted Set(queue:{eventId})에서 관리되며,
 * score를 이용한 정렬을 통해 대기 위치를 결정한다.
 */
@Getter
@Builder
@AllArgsConstructor
public class QueueEntry {

    /** 사용자 ID */
    private Long userId;

    /** 공연 ID */
    private Long eventId;

    /** Sorted Set 스코어 (보통 System.currentTimeMillis() 또는 순서 증가값) */
    private long score;

    /**
     * 대기열 항목을 생성한다.
     *
     * @param eventId 공연 ID
     * @param userId 사용자 ID
     * @param score Sorted Set 스코어
     * @return 생성된 대기열 항목
     */
    public static QueueEntry of(Long eventId, Long userId, long score) {
        return QueueEntry.builder()
                .userId(userId)
                .eventId(eventId)
                .score(score)
                .build();
    }
}
