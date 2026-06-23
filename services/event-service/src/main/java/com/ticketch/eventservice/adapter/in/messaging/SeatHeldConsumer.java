package com.ticketch.eventservice.adapter.in.messaging;

import com.ticketch.eventservice.application.port.in.UpdateSeatStatusUseCase;
import com.ticketch.eventservice.domain.model.Seat;
import com.ticketch.events.reservation.SeatHeldEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * [Messaging Adapter] 좌석 임시 선점 이벤트 RabbitMQ Consumer.
 *
 * <p>Reservation Service가 좌석을 선점할 때 발행한 {@link SeatHeldEvent}를 소비하여
 * 좌석 상태를 HELD로 변경하고 SSE로 클라이언트에 실시간 푸시한다.
 *
 * <p>큐: {@code seat.held.queue} (Exchange: seat.exchange, routing key: seat.held)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatHeldConsumer {

    private final UpdateSeatStatusUseCase updateSeatStatusUseCase;

    @RabbitListener(queues = "seat.held.queue")
    public void consume(SeatHeldEvent event) {
        log.info("좌석 선점 이벤트 수신 seatId={}, reservationId={}", event.getSeatId(), event.getReservationId());
        updateSeatStatusUseCase.updateStatus(event.getSeatId(), Seat.SeatStatus.HELD);
    }
}
