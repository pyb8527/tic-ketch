package com.ticketch.reservationservice.domain.service;

import com.ticketch.reservationservice.domain.model.Reservation;
import org.springframework.stereotype.Service;

/**
 * [Domain Service] 예약 도메인 서비스.
 *
 * <p>도메인 모델들 간의 복잡한 상태 전이나 비즈니스 규칙을 조정하며,
 * 순수 비즈니스 로직을 담당한다.
 *
 * <p>만료 예약 판단은 {@code LoadReservationPort.findExpiredPending()}에 위임하고,
 * 이 서비스는 상태 전이 헬퍼 메서드만 제공한다.
 */
@Service
public class ReservationDomainService {

    /**
     * 예약을 만료 상태로 전이한다.
     *
     * <p>도메인 모델의 {@link Reservation#expire()} 메서드를 호출하여
     * 멱등한 상태 전이를 수행한다.
     *
     * @param reservation 만료시킬 예약
     * @return 상태 전이된 예약
     */
    public Reservation toExpired(Reservation reservation) {
        reservation.expire();
        return reservation;
    }
}
