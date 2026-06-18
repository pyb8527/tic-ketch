package com.ticketch.eventservice.adapter.in.web;

import com.ticketch.common.response.ApiResponse;
import com.ticketch.eventservice.adapter.in.web.dto.SeatResponse;
import com.ticketch.eventservice.application.port.in.GetSeatsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * [Web Adapter] 좌석 현황 조회 컨트롤러.
 *
 * <p>Redis 캐시 우선 조회 → 캐시 미스 시 DB 조회.
 * 실시간 좌석 상태는 SSE({@link SeatSseController})로 별도 제공된다.
 */
@RestController
@RequestMapping("/api/events/{eventId}/seats")
@RequiredArgsConstructor
public class SeatController {

    private final GetSeatsUseCase getSeatsUseCase;

    /** 공연의 전체 좌석 목록 조회 (Redis 캐시 적용) */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getSeats(@PathVariable Long eventId) {
        List<SeatResponse> seats = getSeatsUseCase.getSeats(eventId).stream()
                .map(SeatResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(seats));
    }
}
