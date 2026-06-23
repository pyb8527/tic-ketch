package com.ticketch.reservationservice.application.port.out;

import java.util.function.Supplier;

/**
 * 좌석 잠금 획득 포트.
 * Redisson을 이용한 분산 잠금 기능을 제공합니다.
 */
public interface AcquireSeatLockPort {

    /**
     * 좌석에 대한 잠금을 획득하여 작업 실행.
     * 잠금 획득 실패 시 BusinessException(ErrorCode.LOCK_ACQUISITION_FAILED) 발생.
     *
     * @param seatId 좌석 ID
     * @param action 잠금 획득 후 실행할 작업
     * @param <T> 반환 타입
     * @return 작업 수행 결과
     */
    <T> T executeWithLock(Long seatId, Supplier<T> action);
}
