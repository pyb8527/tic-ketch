package com.ticketch.eventservice.application.port.out;

import com.ticketch.eventservice.domain.model.Seat;

/**
 * [Output Port] SSE 이벤트 발행 인터페이스.
 *
 * <p>구현체: {@link com.ticketch.eventservice.adapter.out.sse.SseEmitterAdapter}
 * <p>좌석 상태가 변경될 때 해당 공연을 구독 중인 모든 클라이언트에 실시간 Push한다.
 */
public interface EmitSeatEventPort {

    /**
     * 특정 공연을 구독 중인 모든 SSE 클라이언트에 좌석 상태 변경 이벤트 전송.
     *
     * @param eventId 공연 ID
     * @param seatId  변경된 좌석 ID
     * @param status  변경된 좌석 상태
     */
    void emit(Long eventId, Long seatId, Seat.SeatStatus status);
}
