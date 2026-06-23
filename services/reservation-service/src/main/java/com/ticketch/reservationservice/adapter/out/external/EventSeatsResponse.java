package com.ticketch.reservationservice.adapter.out.external;

import java.util.List;

/**
 * Event Service의 좌석 목록 조회 응답 DTO.
 * ApiResponse의 no-arg 생성자 부재 문제를 우회하기 위해 로컬 래퍼 record 정의.
 * Feign이 이 타입으로 역직렬화.
 */
public record EventSeatsResponse(
    String code,
    String message,
    List<SeatStatusDto> data
) {
}
