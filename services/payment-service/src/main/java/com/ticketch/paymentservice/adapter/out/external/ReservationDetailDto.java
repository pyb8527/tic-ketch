package com.ticketch.paymentservice.adapter.out.external;

import java.time.LocalDateTime;

/**
 * 예약 서비스의 예약 상세 정보 DTO.
 * API 응답 데이터로 역직렬화되는 레코드.
 */
public record ReservationDetailDto(
    Long id,
    Long seatId,
    Long eventId,
    String status,
    LocalDateTime expiresAt,
    long remainingSeconds
) {}
