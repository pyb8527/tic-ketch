package com.ticketch.reservationservice.adapter.out.messaging;

import com.ticketch.events.reservation.SeatHeldEvent;
import com.ticketch.events.reservation.SeatReleasedEvent;
import com.ticketch.reservationservice.application.port.out.PublishSeatReleasedPort;
import com.ticketch.reservationservice.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 좌석 이벤트 발행 어댑터.
 * RabbitMQ를 통해 좌석 보유/해제 이벤트를 발행합니다.
 */
@Component
@RequiredArgsConstructor
public class SeatEventPublisher implements PublishSeatReleasedPort {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 좌석 보유 이벤트 발행.
     * 예약이 생성되어 좌석이 보유될 때 seat.exchange로 발행됩니다.
     *
     * @param event 좌석 보유 이벤트
     */
    @Override
    public void publishSeatHeld(SeatHeldEvent event) {
        rabbitTemplate.convertAndSend(RabbitConfig.SEAT_EXCHANGE, RabbitConfig.SEAT_HELD_KEY, event);
    }

    /**
     * 좌석 해제 이벤트 발행.
     * 예약이 취소되거나 만료될 때 seat.exchange로 발행됩니다.
     *
     * @param event 좌석 해제 이벤트
     */
    @Override
    public void publishSeatReleased(SeatReleasedEvent event) {
        rabbitTemplate.convertAndSend(RabbitConfig.SEAT_EXCHANGE, RabbitConfig.SEAT_RELEASED_KEY, event);
    }
}
