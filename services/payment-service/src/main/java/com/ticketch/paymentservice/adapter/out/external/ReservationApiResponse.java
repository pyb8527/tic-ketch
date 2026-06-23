package com.ticketch.paymentservice.adapter.out.external;

/**
 * 예약 서비스의 API 응답 래퍼.
 * com.ticketch.common.response.ApiResponse의 no-arg 생성자 부재 문제를 우회하기 위해 로컬 정의.
 * Feign이 이 타입으로 역직렬화.
 */
public record ReservationApiResponse(
    String code,
    String message,
    ReservationDetailDto data
) {}
