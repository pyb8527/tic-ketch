package com.ticketch.reservationservice.adapter.out.redis;

import com.ticketch.common.exception.BusinessException;
import com.ticketch.common.exception.ErrorCode;
import com.ticketch.reservationservice.application.port.out.AcquireSeatLockPort;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * [Adapter] Redisson 기반 좌석 잠금 어댑터.
 *
 * <p>분산 환경에서 동일한 좌석에 대한 동시 접근을 제어합니다.
 * Redis의 RLock을 이용하여 잠금을 획득한 후 작업을 수행합니다.
 */
@Component
@RequiredArgsConstructor
public class RedissonLockAdapter implements AcquireSeatLockPort {

    private final RedissonClient redissonClient;

    /**
     * 좌석 잠금을 획득하여 작업을 실행합니다.
     *
     * <p>잠금 획득 대기 시간: 3초, 잠금 유지 시간: 10초.
     * 잠금 획득에 실패하면 {@link ErrorCode#LOCK_ACQUISITION_FAILED}를 던집니다.
     *
     * @param seatId 좌석 ID
     * @param action 잠금 획득 후 실행할 작업
     * @param <T> 반환 타입
     * @return 작업 수행 결과
     * @throws BusinessException 잠금 획득 실패 시 또는 인터럽트 발생 시
     */
    @Override
    public <T> T executeWithLock(Long seatId, Supplier<T> action) {
        RLock lock = redissonClient.getLock("seat:lock:" + seatId);
        boolean acquired = false;

        try {
            // 3초간 대기, 획득 후 10초간 유지
            acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BusinessException(ErrorCode.LOCK_ACQUISITION_FAILED);
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.LOCK_ACQUISITION_FAILED);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
