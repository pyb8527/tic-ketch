package com.ticketch.reservationservice.application.port.in;

import com.ticketch.events.reservation.SeatReleasedEvent;

/**
 * 예약을 취소하는 유스케이스 포트.
 */
public interface CancelReservationUseCase {

  void cancel(CancelCommand command);

  record CancelCommand(Long reservationId, Long requesterId, SeatReleasedEvent.ReleaseReason reason) {}
}
