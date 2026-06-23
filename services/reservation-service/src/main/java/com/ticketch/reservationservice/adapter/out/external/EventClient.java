package com.ticketch.reservationservice.adapter.out.external;

import com.ticketch.reservationservice.application.port.out.ValidateSeatPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Event Service의 좌석 가용성 검증 포트 구현.
 * Feign 클라이언트를 통해 event-service와 통신하여 좌석 상태 확인.
 */
@Component
@RequiredArgsConstructor
public class EventClient implements ValidateSeatPort {

    private final EventFeignClient eventFeignClient;

    /**
     * 특정 좌석이 AVAILABLE 상태인지 확인.
     * event-service에서 전체 좌석 목록을 조회한 후 조건에 맞는 좌석을 필터링.
     *
     * @param eventId 이벤트 ID
     * @param seatId 좌석 ID
     * @return 좌석이 AVAILABLE 상태이면 true, 아니면 false
     */
    @Override
    public boolean isAvailable(Long eventId, Long seatId) {
        EventSeatsResponse response = eventFeignClient.getSeats(eventId);

        if (response == null || response.data() == null) {
            return false;
        }

        return response.data().stream()
            .anyMatch(s -> seatId.equals(s.id()) && "AVAILABLE".equals(s.status()));
    }
}
