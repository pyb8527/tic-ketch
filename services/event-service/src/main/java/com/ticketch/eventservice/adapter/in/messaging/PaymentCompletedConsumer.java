package com.ticketch.eventservice.adapter.in.messaging;

import com.ticketch.eventservice.application.port.in.UpdateSeatStatusUseCase;
import com.ticketch.eventservice.domain.model.Seat;
import com.ticketch.events.payment.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * [Messaging Adapter] 결제 완료 이벤트 RabbitMQ Consumer.
 *
 * <p>Payment Service가 발행한 {@link PaymentCompletedEvent}를 소비하여
 * 해당 좌석 상태를 SOLD로 변경하고 SSE로 클라이언트에 실시간 푸시한다.
 *
 * <p>큐: {@code payment.completed.event.queue}
 * (Exchange: payment.exchange, routing key: payment.completed) — Reservation/Notification의 큐와 별개
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedConsumer {

    private final UpdateSeatStatusUseCase updateSeatStatusUseCase;

    @RabbitListener(queues = "payment.completed.event.queue")
    public void consume(PaymentCompletedEvent event) {
        log.info("결제 완료 이벤트 수신 → 좌석 판매 처리 seatId={}, paymentId={}", event.getSeatId(), event.getPaymentId());
        updateSeatStatusUseCase.updateStatus(event.getSeatId(), Seat.SeatStatus.SOLD);
    }
}
