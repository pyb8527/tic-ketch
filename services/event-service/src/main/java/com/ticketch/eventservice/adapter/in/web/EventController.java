package com.ticketch.eventservice.adapter.in.web;

import com.ticketch.common.response.ApiResponse;
import com.ticketch.eventservice.adapter.in.web.dto.CreateEventRequest;
import com.ticketch.eventservice.adapter.in.web.dto.CreateSeatsRequest;
import com.ticketch.eventservice.adapter.in.web.dto.EventResponse;
import com.ticketch.eventservice.application.port.in.CreateEventUseCase;
import com.ticketch.eventservice.application.port.in.GetEventUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [Web Adapter] 공연 조회 · 생성 컨트롤러.
 *
 * <p>GET 엔드포인트: 인증 불필요 (누구나 공연 목록 조회 가능)
 * <p>POST 엔드포인트: ADMIN 권한 필요 (API Gateway에서 검증)
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final GetEventUseCase getEventUseCase;
    private final CreateEventUseCase createEventUseCase;

    /** 공연 목록 조회 (페이징, 기본 20개) */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<EventResponse>>> getEvents(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<EventResponse> result = getEventUseCase.getEvents(pageable)
                .map(EventResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /** 공연 단건 조회 */
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<EventResponse>> getEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(EventResponse.from(getEventUseCase.getEvent(eventId))));
    }

    /** 공연 등록 (관리자 전용) */
    @PostMapping("/admin")
    public ResponseEntity<ApiResponse<Long>> createEvent(@RequestBody @Valid CreateEventRequest request) {
        Long eventId = createEventUseCase.createEvent(
                new CreateEventUseCase.CreateEventCommand(request.title(), request.venue(), request.eventDate())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(eventId));
    }

    /** 좌석 일괄 생성 (관리자 전용) */
    @PostMapping("/admin/{eventId}/seats")
    public ResponseEntity<ApiResponse<Integer>> createSeats(
            @PathVariable Long eventId,
            @RequestBody @Valid CreateSeatsRequest request) {
        List<CreateEventUseCase.CreateSeatCommand> commands = request.seats().stream()
                .map(s -> new CreateEventUseCase.CreateSeatCommand(s.seatGradeId(), s.rowName(), s.seatNumber()))
                .toList();
        int count = createEventUseCase.createSeats(eventId, commands);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(count));
    }
}
