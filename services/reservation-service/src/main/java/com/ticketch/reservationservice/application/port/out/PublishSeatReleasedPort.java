package com.ticketch.reservationservice.application.port.out;

import com.ticketch.events.reservation.SeatHeldEvent;
import com.ticketch.events.reservation.SeatReleasedEvent;

/**
 * 좌석 이벤트 발행 포트.
 * RabbitMQ를 통해 좌석 관련 이벤트를 발행합니다.
 */
public interface PublishSeatReleasedPort {

    /**
     * 좌석 보유 이벤트 발행.
     * 예약이 생성되어 좌석이 보유될 때 발행됩니다.
     *
     * @param event 좌석 보유 이벤트
     */
    void publishSeatHeld(SeatHeldEvent event);

    /**
     * 좌석 해제 이벤트 발행.
     * 예약이 취소되거나 만료될 때 발행됩니다.
     *
     * @param event 좌석 해제 이벤트
     */
    void publishSeatReleased(SeatReleasedEvent event);
}
