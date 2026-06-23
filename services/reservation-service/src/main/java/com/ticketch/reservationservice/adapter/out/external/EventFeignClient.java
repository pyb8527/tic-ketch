package com.ticketch.reservationservice.adapter.out.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Event Service와의 통신을 담당하는 Feign 클라이언트.
 * 좌석 목록을 조회하고 fallback을 통해 fail-closed 동작 구현.
 */
@FeignClient(name = "event-service", fallback = EventClientFallback.class)
public interface EventFeignClient {

    /**
     * Event Service에서 특정 이벤트의 모든 좌석 정보를 조회.
     * event-service는 단일 좌석 endpoint를 미지원하므로 전체 목록 조회 후 필터링.
     *
     * @param eventId 이벤트 ID
     * @return 좌석 목록과 함께 API 응답
     */
    @GetMapping("/api/events/{eventId}/seats")
    EventSeatsResponse getSeats(@PathVariable("eventId") Long eventId);
}
