package com.ticketch.reservationservice.application.port.in;

/**
 * 대기열 관리하는 유스케이스 포트.
 */
public interface QueueUseCase {

  QueueStatus enter(Long eventId, Long userId);

  QueueStatus getStatus(Long eventId, Long userId);

  record QueueStatus(long position, long totalWaiting) {}
}
