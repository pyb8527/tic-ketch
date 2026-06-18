package com.ticketch.eventservice.application.port.in;

import com.ticketch.eventservice.domain.model.Seat;

/**
 * [Input Port] 좌석 상태 변경 유스케이스.
 *
 * <p>구현체: {@link com.ticketch.eventservice.application.service.SeatManagementService}
 * <p>호출자:
 * <ul>
 *   <li>{@link com.ticketch.eventservice.adapter.in.messaging.SeatReleaseConsumer} — RabbitMQ seat.released 이벤트</li>
 * </ul>
 */
public interface UpdateSeatStatusUseCase {

    /**
     * 좌석 상태를 변경하고 Redis 캐시를 갱신한 뒤 SSE로 클라이언트에 푸시한다.
     *
     * @param seatId    상태를 변경할 좌석 ID
     * @param newStatus 변경할 상태 (AVAILABLE / HELD / SOLD)
     */
    void updateStatus(Long seatId, Seat.SeatStatus newStatus);
}
