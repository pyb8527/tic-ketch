package com.ticketch.eventservice.adapter.out.sse;

import com.ticketch.eventservice.application.port.out.EmitSeatEventPort;
import com.ticketch.eventservice.domain.model.Seat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * [SSE Adapter] SSE Emitter 풀 관리 및 이벤트 발행 구현체.
 *
 * <p>구현 포트: {@link EmitSeatEventPort}
 *
 * <p>Emitter 풀 구조:
 * <pre>
 * emitters: ConcurrentHashMap&lt;eventId, CopyOnWriteArrayList&lt;SseEmitter&gt;&gt;
 * </pre>
 *
 * <p>CopyOnWriteArrayList를 사용하는 이유:
 * 이벤트 발행(읽기)과 emitter 추가/제거(쓰기)가 동시에 일어나도 ConcurrentModificationException 없이
 * 안전하게 처리하기 위함이다. 읽기 빈도가 쓰기보다 훨씬 높아 성능상 유리하다.
 */
@Slf4j
@Component
public class SseEmitterAdapter implements EmitSeatEventPort {

    /** eventId → SSE 클라이언트 Emitter 목록 */
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * 새 SSE 클라이언트 등록.
     * 타임아웃·완료·에러 발생 시 풀에서 자동 제거한다.
     */
    public void register(Long eventId, SseEmitter emitter) {
        List<SseEmitter> eventEmitters = emitters.computeIfAbsent(
                eventId, k -> new CopyOnWriteArrayList<>()
        );
        eventEmitters.add(emitter);

        // 연결 종료 시 자동 제거 콜백 등록
        Runnable removeCallback = () -> {
            eventEmitters.remove(emitter);
            log.debug("SSE emitter 제거 eventId={}, 남은 연결={}", eventId, eventEmitters.size());
        };
        emitter.onCompletion(removeCallback);
        emitter.onTimeout(removeCallback);
        emitter.onError(e -> removeCallback.run());

        log.debug("SSE emitter 등록 eventId={}, 현재 연결={}", eventId, eventEmitters.size());
    }

    /**
     * 특정 공연의 모든 SSE 클라이언트에 좌석 상태 변경 이벤트 전송.
     *
     * <p>전송 실패한 emitter는 즉시 풀에서 제거한다.
     * (연결이 끊긴 클라이언트에 전송 시도하면 IOException이 발생)
     */
    @Override
    public void emit(Long eventId, Long seatId, Seat.SeatStatus status) {
        List<SseEmitter> eventEmitters = emitters.getOrDefault(eventId, List.of());
        if (eventEmitters.isEmpty()) {
            return;
        }

        String eventData = String.format("{\"seatId\":%d,\"status\":\"%s\"}", seatId, status.name());

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        eventEmitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("seat-status")
                        .data(eventData));
            } catch (IOException e) {
                // 연결이 끊긴 emitter — 제거 대상으로 표시
                deadEmitters.add(emitter);
                log.debug("SSE 전송 실패, emitter 제거 eventId={}", eventId);
            }
        });

        // 전송 실패한 emitter 일괄 제거
        eventEmitters.removeAll(deadEmitters);
        log.debug("SSE 이벤트 발행 eventId={}, seatId={}, status={}", eventId, seatId, status);
    }
}
