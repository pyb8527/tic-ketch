package com.ticketch.reservationservice.application.port.in;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 조회하는 유스케이스 포트.
 */
public interface GetReservationUseCase {

  ReservationDetail getReservation(Long reservationId, Long requesterId);

  List<ReservationDetail> getMyReservations(Long userId);

  record ReservationDetail(Long id, Long seatId, Long eventId, String status, LocalDateTime expiresAt,
      long remainingSeconds) {}
}
