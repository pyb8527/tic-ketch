package com.ticketch.eventservice.domain.service;

import com.ticketch.common.exception.BusinessException;
import com.ticketch.common.exception.ErrorCode;
import com.ticketch.eventservice.domain.model.Seat;
import org.springframework.stereotype.Service;

/**
 * [Domain Service] 좌석 상태 전이 규칙.
 *
 * <p>허용된 상태 전이:
 * <pre>
 * AVAILABLE → HELD     (임시 선점)
 * AVAILABLE → SOLD     (즉시 구매)
 * HELD      → SOLD     (결제 완료)
 * HELD      → AVAILABLE (TTL 만료 / 취소)
 * SOLD      → AVAILABLE (환불)
 * </pre>
 *
 * <p>도메인 서비스이므로 Spring 컨텍스트와 무관하게 단독으로 테스트 가능하다.
 */
@Service
public class SeatStatusService {

    /**
     * 좌석이 임시 선점 가능한 상태인지 검증한다.
     *
     * @throws BusinessException SEAT_ALREADY_HELD — 이미 선점된 좌석
     * @throws BusinessException SEAT_ALREADY_SOLD — 이미 판매된 좌석
     */
    public void validateHoldable(Seat seat) {
        if (seat.getStatus() == Seat.SeatStatus.HELD) {
            throw new BusinessException(ErrorCode.SEAT_ALREADY_HELD);
        }
        if (seat.getStatus() == Seat.SeatStatus.SOLD) {
            throw new BusinessException(ErrorCode.SEAT_ALREADY_SOLD);
        }
    }

    /**
     * 요청된 상태 전이가 허용된 전이인지 검증한다.
     *
     * @param current 현재 상태
     * @param next    변경하려는 상태
     * @throws BusinessException SEAT_NOT_AVAILABLE — 허용되지 않은 전이
     */
    public void validateTransition(Seat.SeatStatus current, Seat.SeatStatus next) {
        boolean allowed = switch (current) {
            case AVAILABLE -> next == Seat.SeatStatus.HELD || next == Seat.SeatStatus.SOLD;
            case HELD      -> next == Seat.SeatStatus.SOLD || next == Seat.SeatStatus.AVAILABLE;
            case SOLD      -> next == Seat.SeatStatus.AVAILABLE; // 환불
        };

        if (!allowed) {
            throw new BusinessException(ErrorCode.SEAT_NOT_AVAILABLE,
                    String.format("좌석 상태 전이 불가: %s → %s", current, next));
        }
    }
}
