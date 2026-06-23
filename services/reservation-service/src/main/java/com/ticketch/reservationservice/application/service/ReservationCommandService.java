package com.ticketch.reservationservice.application.service;

import com.ticketch.common.exception.BusinessException;
import com.ticketch.common.exception.ErrorCode;
import com.ticketch.events.reservation.SeatReleasedEvent;
import com.ticketch.reservationservice.application.port.in.CancelReservationUseCase;
import com.ticketch.reservationservice.application.port.in.ConfirmReservationUseCase;
import com.ticketch.reservationservice.application.port.out.LoadReservationPort;
import com.ticketch.reservationservice.application.port.out.PublishSeatReleasedPort;
import com.ticketch.reservationservice.application.port.out.ReleaseSeatCachePort;
import com.ticketch.reservationservice.application.port.out.SaveReservationPort;
import com.ticketch.reservationservice.domain.model.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [Service] 예약 확정 및 취소 명령 서비스.
 *
 * <p>결제 완료 및 취소 요청에 대한 예약 상태 전이, 캐시 제거, 이벤트 발행을 담당합니다.
 *
 * <p>구현 유스케이스:
 * <ul>
 *   <li>confirm: 결제 완료(PaymentCompletedEvent) 수신 시 예약을 CONFIRMED로 상태 변경</li>
 *   <li>cancel: 사용자 직접 취소 또는 시스템 자동 취소(결제 실패/만료) 시 예약을 CANCELLED로 상태 변경</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ReservationCommandService implements ConfirmReservationUseCase, CancelReservationUseCase {

    private final LoadReservationPort loadReservationPort;
    private final SaveReservationPort saveReservationPort;
    private final ReleaseSeatCachePort releaseSeatCachePort;
    private final PublishSeatReleasedPort publishSeatReleasedPort;

    /**
     * 예약을 확정한다.
     *
     * <p>결제 완료(PaymentCompletedEvent) 수신 시 호출됩니다. 예약 상태를 PENDING → CONFIRMED로 변경합니다.
     *
     * @param reservationId 예약 ID
     * @throws BusinessException RESERVATION_NOT_FOUND (예약 미존재) 또는 도메인 규칙 위반 시
     */
    @Transactional
    @Override
    public void confirm(Long reservationId) {
        Reservation r = loadReservationPort.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
        r.confirm();
        saveReservationPort.save(r);
    }

    /**
     * 예약을 취소한다.
     *
     * <p>사용자 직접 취소(DELETE /{id}) 또는 시스템 자동 취소(결제 실패/만료) 시 호출됩니다.
     * requesterId가 null이 아니면 소유권을 검증하고, 예약 상태를 CANCELLED로 변경한 후
     * 캐시를 제거하고 SeatReleasedEvent를 발행합니다.
     *
     * @param cmd 취소 명령 (reservationId, requesterId, reason)
     * @throws BusinessException RESERVATION_NOT_FOUND (예약 미존재), RESERVATION_NOT_OWNED (소유권 불일치) 또는 도메인 규칙 위반 시
     */
    @Transactional
    @Override
    public void cancel(CancelReservationUseCase.CancelCommand cmd) {
        Reservation r = loadReservationPort.findById(cmd.reservationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
        if (cmd.requesterId() != null) {
            r.assertOwnedBy(cmd.requesterId());
        }
        r.cancel();
        saveReservationPort.save(r);
        releaseSeatCachePort.release(r.getId());
        publishSeatReleasedPort.publishSeatReleased(
                SeatReleasedEvent.builder()
                        .reservationId(r.getId())
                        .seatId(r.getSeatId())
                        .eventId(r.getEventId())
                        .reason(cmd.reason())
                        .build()
        );
    }
}
