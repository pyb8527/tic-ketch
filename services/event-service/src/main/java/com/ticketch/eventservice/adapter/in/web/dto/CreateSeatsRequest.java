package com.ticketch.eventservice.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/** 좌석 일괄 생성 요청 DTO (관리자 전용) */
public record CreateSeatsRequest(
        @NotEmpty List<SeatItem> seats
) {
    public record SeatItem(
            @NotNull Long seatGradeId,
            @NotBlank String rowName,
            @Positive Integer seatNumber
    ) {}
}
