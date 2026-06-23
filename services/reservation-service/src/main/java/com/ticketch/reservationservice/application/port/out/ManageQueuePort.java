package com.ticketch.reservationservice.application.port.out;

/**
 * 대기열 관리 포트.
 * Redis Sorted Set을 이용한 이벤트 대기열 기능을 제공합니다.
 */
public interface ManageQueuePort {

    /**
     * 사용자를 대기열에 추가.
     * score는 우선순위를 나타냅니다 (낮을수록 높은 우선순위).
     *
     * @param eventId 이벤트 ID
     * @param userId 사용자 ID
     * @param score 우선순위 점수
     */
    void add(Long eventId, Long userId, long score);

    /**
     * 대기열에서 사용자의 순위 조회.
     * 1-based 순위를 반환합니다. 대기열에 없으면 -1을 반환합니다.
     *
     * @param eventId 이벤트 ID
     * @param userId 사용자 ID
     * @return 순위 (1-based) 또는 -1 (미존재)
     */
    long rank(Long eventId, Long userId);

    /**
     * 대기열 크기 조회.
     *
     * @param eventId 이벤트 ID
     * @return 대기 중인 사용자 수
     */
    long size(Long eventId);
}
