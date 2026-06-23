package com.ticketch.reservationservice.adapter.in.web;

import com.ticketch.common.response.ApiResponse;
import com.ticketch.reservationservice.application.port.in.QueueUseCase;
import com.ticketch.reservationservice.application.port.in.QueueUseCase.QueueStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [Web Adapter] 대기열 관리 컨트롤러.
 *
 * <p>고객이 공연의 대기열에 진입하고 현재 위치를 조회할 때 사용됩니다.
 * <p>X-User-Id 헤더를 통해 사용자 인증 정보를 수신합니다.
 */
@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
public class QueueController {

  private final QueueUseCase queueUseCase;

  /**
   * 대기열에 진입합니다.
   *
   * @param eventId 공연 ID
   * @param userId 사용자 ID (X-User-Id 헤더)
   * @return HTTP 200 OK 응답, 현재 대기 위치와 전체 대기 인원
   */
  @PostMapping("/{eventId}/enter")
  public ResponseEntity<ApiResponse<QueueStatus>> enter(
      @PathVariable Long eventId,
      @RequestHeader("X-User-Id") Long userId) {
    QueueStatus status = queueUseCase.enter(eventId, userId);
    return ResponseEntity.ok(ApiResponse.ok(status));
  }

  /**
   * 대기열에서의 현재 위치를 조회합니다.
   *
   * @param eventId 공연 ID
   * @param userId 사용자 ID (X-User-Id 헤더)
   * @return HTTP 200 OK 응답, 현재 대기 위치와 전체 대기 인원
   */
  @GetMapping("/{eventId}")
  public ResponseEntity<ApiResponse<QueueStatus>> status(
      @PathVariable Long eventId,
      @RequestHeader("X-User-Id") Long userId) {
    QueueStatus queueStatus = queueUseCase.getStatus(eventId, userId);
    return ResponseEntity.ok(ApiResponse.ok(queueStatus));
  }
}
