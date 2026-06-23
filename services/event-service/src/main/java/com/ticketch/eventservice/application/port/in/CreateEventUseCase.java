package com.ticketch.eventservice.application.port.in;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [Input Port] 공연·좌석 생성 유스케이스 (관리자 전용).
 *
 * <p>구현체: {@link com.ticketch.eventservice.application.service.SeatManagementService}
 */
public interface CreateEventUseCase {

    /** 공연 등록 후 생성된 공연 ID 반환 */
    Long createEvent(CreateEventCommand command);

    /** 좌석 일괄 생성 후 생성된 좌석 수 반환 */
    int createSeats(Long eventId, List<CreateSeatCommand> seats);

    record CreateEventCommand(String title, String venue, String category, String posterUrl, LocalDateTime eventDate) {}

    record CreateSeatCommand(Long seatGradeId, String rowName, Integer seatNumber) {}
}
