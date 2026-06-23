package com.ticketch.reservationservice.adapter.out.external;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Event Service의 Feign fallback 구현.
 * event-service 장애 시 좌석을 unavailable로 취급하는 fail-closed 전략.
 */
@Component
public class EventClientFallback implements EventFeignClient {

    /**
     * event-service 장애 시 빈 좌석 목록 반환.
     * 이로써 모든 좌석을 unavailable로 취급하여 예약을 거부(fail-closed).
     *
     * @param eventId 이벤트 ID
     * @return 빈 좌석 목록을 포함한 fallback 응답
     */
    @Override
    public EventSeatsResponse getSeats(Long eventId) {
        return new EventSeatsResponse("C004", "event-service unavailable (fallback)", List.of());
    }
}
