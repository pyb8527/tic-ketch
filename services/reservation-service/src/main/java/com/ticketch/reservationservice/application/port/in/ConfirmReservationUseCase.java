package com.ticketch.reservationservice.application.port.in;

/**
 * 예약을 확정하는 유스케이스 포트.
 */
public interface ConfirmReservationUseCase {

  void confirm(Long reservationId);
}
