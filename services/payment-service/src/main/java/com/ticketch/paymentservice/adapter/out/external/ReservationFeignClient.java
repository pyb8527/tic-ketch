package com.ticketch.paymentservice.adapter.out.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 예약 서비스와의 통신을 담당하는 Feign 클라이언트.
 * 예약 상세 정보를 조회하고 fallback을 통해 fail-closed 동작 구현.
 */
@FeignClient(name = "reservation-service", fallback = ReservationClientFallback.class)
public interface ReservationFeignClient {

    /**
     * 예약 서비스에서 특정 예약의 상세 정보를 조회.
     *
     * @param id 예약 ID
     * @param userId 사용자 ID (헤더)
     * @return 예약 상세 정보를 포함한 API 응답
     */
    @GetMapping("/api/reservations/{id}")
    ReservationApiResponse getReservation(
        @PathVariable("id") Long id,
        @RequestHeader("X-User-Id") Long userId
    );
}
