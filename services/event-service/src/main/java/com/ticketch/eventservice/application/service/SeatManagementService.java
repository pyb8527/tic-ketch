package com.ticketch.eventservice.application.service;

import com.ticketch.common.exception.BusinessException;
import com.ticketch.common.exception.ErrorCode;
import com.ticketch.eventservice.application.port.in.CreateEventUseCase;
import com.ticketch.eventservice.application.port.in.UpdateSeatStatusUseCase;
import com.ticketch.eventservice.application.port.out.CacheSeatStatusPort;
import com.ticketch.eventservice.application.port.out.EmitSeatEventPort;
import com.ticketch.eventservice.application.port.out.LoadSeatPort;
import com.ticketch.eventservice.application.port.out.SaveEventPort;
import com.ticketch.eventservice.application.port.out.UpdateSeatStatusPort;
import com.ticketch.eventservice.domain.model.Event;
import com.ticketch.eventservice.domain.model.Seat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [Application Service] 좌석 상태 관리 및 공연 생성 유스케이스 구현체.
 *
 * <p>좌석 상태 변경 흐름:
 * <ol>
 *   <li>DB 상태 업데이트 (UpdateSeatStatusPort)</li>
 *   <li>Redis 캐시 갱신 (CacheSeatStatusPort)</li>
 *   <li>SSE로 클라이언트 Push (EmitSeatEventPort)</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class SeatManagementService implements UpdateSeatStatusUseCase, CreateEventUseCase {

    private final LoadSeatPort loadSeatPort;
    private final UpdateSeatStatusPort updateSeatStatusPort;
    private final SaveEventPort saveEventPort;
    private final CacheSeatStatusPort cacheSeatStatusPort;
    private final EmitSeatEventPort emitSeatEventPort;

    /**
     * 좌석 상태 변경 — DB 업데이트 → 캐시 갱신 → SSE Push.
     * RabbitMQ seat.released 이벤트 수신 시 호출된다.
     */
    @Override
    @Transactional
    public void updateStatus(Long seatId, Seat.SeatStatus newStatus) {
        Seat seat = loadSeatPort.findSeatById(seatId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));

        // 1. DB 상태 업데이트 (Optimistic Lock 적용)
        updateSeatStatusPort.updateStatus(seatId, newStatus);

        // 2. Redis Hash 캐시에서 해당 좌석만 업데이트
        cacheSeatStatusPort.updateSeatStatus(seat.getEventId(), seatId, newStatus);

        // 3. 해당 공연 구독 클라이언트에 SSE 이벤트 전송
        emitSeatEventPort.emit(seat.getEventId(), seatId, newStatus);
    }

    @Override
    @Transactional
    public Long createEvent(CreateEventCommand command) {
        Event event = Event.builder()
                .title(command.title())
                .venue(command.venue())
                .category(command.category() != null ? command.category() : "CONCERT")
                .posterUrl(command.posterUrl())
                .eventDate(command.eventDate())
                .status(Event.EventStatus.UPCOMING)
                .createdAt(LocalDateTime.now())
                .build();

        return saveEventPort.saveEvent(event);
    }

    @Override
    @Transactional
    public int createSeats(Long eventId, List<CreateSeatCommand> commands) {
        List<Seat> seats = commands.stream()
                .map(cmd -> Seat.builder()
                        .eventId(eventId)
                        .seatGradeId(cmd.seatGradeId())
                        .rowName(cmd.rowName())
                        .seatNumber(cmd.seatNumber())
                        .status(Seat.SeatStatus.AVAILABLE)
                        .version(0L)
                        .build())
                .toList();

        // 좌석 생성 시 캐시 무효화
        cacheSeatStatusPort.evict(eventId);
        return saveEventPort.saveSeats(seats).size();
    }
}
