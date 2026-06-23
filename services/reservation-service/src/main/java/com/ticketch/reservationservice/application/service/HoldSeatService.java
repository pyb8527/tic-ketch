package com.ticketch.reservationservice.application.service;

import com.ticketch.common.exception.BusinessException;
import com.ticketch.common.exception.ErrorCode;
import com.ticketch.events.reservation.SeatHeldEvent;
import com.ticketch.reservationservice.application.port.in.HoldSeatUseCase;
import com.ticketch.reservationservice.application.port.in.HoldSeatUseCase.HoldSeatCommand;
import com.ticketch.reservationservice.application.port.in.HoldSeatUseCase.HoldSeatResult;
import com.ticketch.reservationservice.application.port.out.AcquireSeatLockPort;
import com.ticketch.reservationservice.application.port.out.HoldSeatCachePort;
import com.ticketch.reservationservice.application.port.out.LoadReservationPort;
import com.ticketch.reservationservice.application.port.out.PublishSeatReleasedPort;
import com.ticketch.reservationservice.application.port.out.SaveReservationPort;
import com.ticketch.reservationservice.application.port.out.ValidateSeatPort;
import com.ticketch.reservationservice.domain.model.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * [UseCase] 좌석 홀드 서비스.
 *
 * <p>좌석 예약 프로세스:
 * <ol>
 *   <li><strong>외부 검증:</strong> Event Service로부터 좌석 가용성 확인</li>
 *   <li><strong>분산 락 획득:</strong> 동일 좌석에 대한 동시 예약 방지</li>
 *   <li><strong>중복 확인:</strong> 락 내에서 해당 좌석의 활성 예약(PENDING/CONFIRMED) 확인</li>
 *   <li><strong>예약 저장:</strong> 새로운 PENDING 예약을 DB에 저장</li>
 *   <li><strong>TTL 캐싱:</strong> Redis에 5분 TTL로 예약 임시 저장</li>
 *   <li><strong>이벤트 발행:</strong> SeatHeldEvent를 RabbitMQ로 발행하여 Event Service 동기화</li>
 * </ol>
 *
 * <p>분산 환경에서 동시성 문제를 방지하며, 실패 시 적절한 비즈니스 예외를 던진다.
 */
@Service
@RequiredArgsConstructor
public class HoldSeatService implements HoldSeatUseCase {

    private final ValidateSeatPort validateSeatPort;
    private final AcquireSeatLockPort acquireSeatLockPort;
    private final LoadReservationPort loadReservationPort;
    private final SaveReservationPort saveReservationPort;
    private final HoldSeatCachePort holdSeatCachePort;
    private final PublishSeatReleasedPort publishSeatReleasedPort;

    private static final long HOLD_MINUTES = 5;

    /**
     * 좌석을 예약 보류 상태로 변경한다.
     *
     * <p>동작 흐름:
     * <ol>
     *   <li>Event Service에서 좌석 가용 여부 확인</li>
     *   <li>분산 락으로 보호된 임계 영역에서 doHold 실행</li>
     * </ol>
     *
     * @param command 좌석 홀드 명령 (userId, seatId, eventId)
     * @return 예약 ID 및 만료 시간
     * @throws BusinessException SEAT_NOT_AVAILABLE 또는 LOCK_ACQUISITION_FAILED
     */
    @Override
    public HoldSeatResult holdSeat(HoldSeatCommand command) {
        if (!validateSeatPort.isAvailable(command.eventId(), command.seatId())) {
            throw new BusinessException(ErrorCode.SEAT_NOT_AVAILABLE);
        }
        return acquireSeatLockPort.executeWithLock(command.seatId(), () -> doHold(command));
    }

    /**
     * 분산 락 내에서 좌석 홀드를 수행한다.
     *
     * <p>동작 흐름:
     * <ol>
     *   <li>해당 좌석의 활성 예약(PENDING/CONFIRMED) 존재 확인</li>
     *   <li>현재시간 + 5분을 만료시간으로 설정하여 PENDING 예약 생성</li>
     *   <li>예약을 DB에 저장</li>
     *   <li>Redis에 TTL을 설정하여 임시 저장</li>
     *   <li>SeatHeldEvent를 발행하여 Event Service 동기화</li>
     * </ol>
     *
     * @param command 좌석 홀드 명령
     * @return 예약 ID 및 만료 시간
     * @throws BusinessException SEAT_ALREADY_HELD
     */
    private HoldSeatResult doHold(HoldSeatCommand command) {
        if (loadReservationPort.existsActiveBySeatId(command.seatId())) {
            throw new BusinessException(ErrorCode.SEAT_ALREADY_HELD);
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(HOLD_MINUTES);
        Reservation reservation = Reservation.create(
                command.userId(),
                command.seatId(),
                command.eventId(),
                expiresAt
        );

        reservation = saveReservationPort.save(reservation);

        holdSeatCachePort.hold(reservation, Duration.ofMinutes(HOLD_MINUTES));

        publishSeatReleasedPort.publishSeatHeld(
                SeatHeldEvent.builder()
                        .reservationId(reservation.getId())
                        .seatId(reservation.getSeatId())
                        .eventId(reservation.getEventId())
                        .userId(reservation.getUserId())
                        .expiresAt(reservation.getExpiresAt())
                        .build()
        );

        return new HoldSeatResult(reservation.getId(), reservation.getExpiresAt());
    }
}
