package com.ticketch.paymentservice.adapter.out.external;

import org.springframework.stereotype.Component;

/**
 * 예약 서비스의 Feign fallback 구현.
 * reservation-service 장애 시 실패 응답을 반환하는 fail-closed 전략.
 */
@Component
public class ReservationClientFallback implements ReservationFeignClient {

    /**
     * reservation-service 장애 시 unavailable 응답 반환.
     * 이로써 예약 검증 실패 처리 후 결제를 거부(fail-closed).
     *
     * @param id 예약 ID
     * @param userId 사용자 ID
     * @return fallback 응답 (data null)
     */
    @Override
    public ReservationApiResponse getReservation(Long id, Long userId) {
        return new ReservationApiResponse("C004", "reservation-service unavailable (fallback)", null);
    }
}
