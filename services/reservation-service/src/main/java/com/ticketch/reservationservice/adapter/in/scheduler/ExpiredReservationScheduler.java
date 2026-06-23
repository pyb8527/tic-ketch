package com.ticketch.reservationservice.adapter.in.scheduler;

import com.ticketch.events.reservation.SeatReleasedEvent;
import com.ticketch.reservationservice.application.port.in.CancelReservationUseCase;
import com.ticketch.reservationservice.application.port.out.LoadReservationPort;
import com.ticketch.reservationservice.domain.model.Reservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [In Adapter] 만료된 예약 정리 스케줄러.
 *
 * <p>매 30초마다 실행되어 PENDING 상태이면서 만료된 예약을 찾아서
 * 자동으로 취소하고 좌석을 해제한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiredReservationScheduler {

    private final LoadReservationPort loadReservationPort;
    private final CancelReservationUseCase cancelReservationUseCase;

    /**
     * 만료된 미확정 예약을 정기적으로 회수한다.
     *
     * <p>30초 고정 지연으로 실행되며, 각 만료된 예약에 대해 취소 처리를 시도한다.
     * 취소 실패 시에는 로그를 남기고 다음 예약으로 진행한다(멱등성 보장).
     */
    @Scheduled(fixedDelay = 30000)
    public void sweepExpired() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expired = loadReservationPort.findExpiredPending(now);

        for (Reservation r : expired) {
            try {
                cancelReservationUseCase.cancel(
                    new CancelReservationUseCase.CancelCommand(
                        r.getId(),
                        null,
                        SeatReleasedEvent.ReleaseReason.EXPIRED
                    )
                );
            } catch (Exception e) {
                log.warn("만료 회수 실패 reservationId={}", r.getId(), e);
            }
        }

        if (!expired.isEmpty()) {
            log.info("만료 예약 {}건 회수", expired.size());
        }
    }
}
