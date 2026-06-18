package com.ticketch.eventservice.adapter.in.messaging;

import com.ticketch.eventservice.application.port.in.UpdateSeatStatusUseCase;
import com.ticketch.eventservice.domain.model.Seat;
import com.ticketch.events.reservation.SeatReleasedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * [Messaging Adapter] 좌석 해제 이벤트 RabbitMQ Consumer.
 *
 * <p>Reservation Service가 발행한 {@link SeatReleasedEvent}를 소비하여
 * 좌석 상태를 AVAILABLE로 복원하고 SSE로 클라이언트에 실시간 푸시한다.
 *
 * <p>큐: {@code seat.released.queue}
 * <p>Exchange: {@code seat.exchange} (routing key: seat.released)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatReleaseConsumer {

    private final UpdateSeatStatusUseCase updateSeatStatusUseCase;

    /**
     * 좌석 해제 이벤트 처리.
     * TTL 만료, 사용자 취소, 결제 실패 — 모두 AVAILABLE로 복원.
     */
    @RabbitListener(queues = "seat.released.queue")
    public void consume(SeatReleasedEvent event) {
        log.info("좌석 해제 이벤트 수신 seatId={}, reason={}", event.getSeatId(), event.getReason());

        // 해제 사유와 무관하게 AVAILABLE 복원 (SeatManagementService가 캐시·SSE까지 처리)
        updateSeatStatusUseCase.updateStatus(event.getSeatId(), Seat.SeatStatus.AVAILABLE);
    }
}
