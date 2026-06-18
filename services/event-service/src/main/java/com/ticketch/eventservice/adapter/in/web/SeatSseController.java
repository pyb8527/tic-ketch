package com.ticketch.eventservice.adapter.in.web;

import com.ticketch.eventservice.adapter.out.sse.SseEmitterAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * [Web Adapter] 좌석 상태 실시간 SSE 스트림 컨트롤러.
 *
 * <p>클라이언트는 이 엔드포인트에 연결하여 좌석 상태 변경 이벤트를 실시간으로 수신한다.
 *
 * <p>동작 원리:
 * <ol>
 *   <li>클라이언트가 GET /api/events/{eventId}/seats/stream 에 연결</li>
 *   <li>SseEmitter 생성 및 SseEmitterAdapter의 emitter 풀에 등록</li>
 *   <li>좌석 상태 변경 시 {@link SseEmitterAdapter#emit}이 해당 공연의 모든 emitter에 이벤트 전송</li>
 *   <li>타임아웃(30분) 또는 연결 종료 시 emitter 풀에서 자동 제거</li>
 * </ol>
 *
 * <p>프론트엔드 사용 예:
 * <pre>
 * const source = new EventSource('/api/events/1/seats/stream');
 * source.addEventListener('seat-status', (e) => { ... });
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class SeatSseController {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30분

    private final SseEmitterAdapter sseEmitterAdapter;

    /**
     * SSE 스트림 구독 엔드포인트.
     * 연결 즉시 heartbeat 이벤트를 전송하여 초기 연결을 확인한다.
     */
    @GetMapping(value = "/{eventId}/seats/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSeatStatus(@PathVariable Long eventId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // emitter 풀에 등록
        sseEmitterAdapter.register(eventId, emitter);

        // 연결 확인용 초기 heartbeat 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("connected to event " + eventId));
        } catch (Exception e) {
            log.warn("SSE 초기 연결 실패 eventId={}", eventId);
            emitter.completeWithError(e);
        }

        log.info("SSE 클라이언트 연결 eventId={}", eventId);
        return emitter;
    }
}
