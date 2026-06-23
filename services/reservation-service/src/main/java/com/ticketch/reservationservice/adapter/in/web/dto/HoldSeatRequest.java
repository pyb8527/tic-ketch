package com.ticketch.reservationservice.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 좌석 홀드 요청 DTO.
 *
 * <p>고객이 특정 좌석을 예약 홀드(보류)할 때 전송하는 요청 본문.
 */
public record HoldSeatRequest(
    @NotNull(message = "seatId는 필수입니다")
    Long seatId,

    @NotNull(message = "eventId는 필수입니다")
    Long eventId
) {}
