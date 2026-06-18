package com.ticketch.eventservice.adapter.in.web.dto;

import com.ticketch.eventservice.domain.model.Seat;

/** 좌석 응답 DTO — 클라이언트의 좌석 배치도 렌더링에 사용 */
public record SeatResponse(
        Long id,
        Long seatGradeId,
        String rowName,
        Integer seatNumber,
        String status
) {
    public static SeatResponse from(Seat seat) {
        return new SeatResponse(
                seat.getId(), seat.getSeatGradeId(),
                seat.getRowName(), seat.getSeatNumber(),
                seat.getStatus().name()
        );
    }
}
