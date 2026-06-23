package com.ticketch.reservationservice.application.service;

import com.ticketch.reservationservice.application.port.in.QueueUseCase;
import com.ticketch.reservationservice.application.port.out.ManageQueuePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 대기열 관리 애플리케이션 서비스.
 *
 * <p>사용자의 대기열 진입 및 상태 조회 기능을 구현합니다.
 * Redis 기반 대기열 포트를 통해 현재시간을 점수로 하여 관리합니다.
 */
@Service
@RequiredArgsConstructor
public class QueueService implements QueueUseCase {

    private final ManageQueuePort manageQueuePort;

    /**
     * 대기열에 진입하고 현재 상태를 반환합니다.
     *
     * @param eventId 이벤트 ID
     * @param userId 사용자 ID
     * @return 대기열 상태 (순위, 전체 대기 수)
     */
    @Override
    public QueueStatus enter(Long eventId, Long userId) {
        long score = System.currentTimeMillis();
        manageQueuePort.add(eventId, userId, score);
        return getStatus(eventId, userId);
    }

    /**
     * 대기열에서 현재 상태를 조회합니다.
     *
     * @param eventId 이벤트 ID
     * @param userId 사용자 ID
     * @return 대기열 상태 (순위, 전체 대기 수)
     */
    @Override
    public QueueStatus getStatus(Long eventId, Long userId) {
        long position = manageQueuePort.rank(eventId, userId);
        long total = manageQueuePort.size(eventId);
        return new QueueStatus(position, total);
    }
}
