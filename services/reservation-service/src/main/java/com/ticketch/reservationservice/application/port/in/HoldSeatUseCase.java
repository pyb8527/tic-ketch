package com.ticketch.reservationservice.application.port.in;

import java.time.LocalDateTime;

/**
 * 좌석 예약 홀드(보류)하는 유스케이스 포트.
 */
public interface HoldSeatUseCase {

  HoldSeatResult holdSeat(HoldSeatCommand command);

  record HoldSeatCommand(Long userId, Long seatId, Long eventId) {}

  record HoldSeatResult(Long reservationId, LocalDateTime expiresAt) {}
}
