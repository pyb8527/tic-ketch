package com.ticketch.reservationservice.adapter.in.web;

import com.ticketch.common.response.ApiResponse;
import com.ticketch.events.reservation.SeatReleasedEvent;
import com.ticketch.reservationservice.adapter.in.web.dto.HoldSeatRequest;
import com.ticketch.reservationservice.application.port.in.CancelReservationUseCase;
import com.ticketch.reservationservice.application.port.in.GetReservationUseCase;
import com.ticketch.reservationservice.application.port.in.GetReservationUseCase.ReservationDetail;
import com.ticketch.reservationservice.application.port.in.HoldSeatUseCase;
import com.ticketch.reservationservice.application.port.in.HoldSeatUseCase.HoldSeatResult;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [Web Adapter] 예약 생성·조회·취소 컨트롤러.
 *
 * <p>모든 엔드포인트는 X-User-Id 헤더를 통해 사용자 인증 정보를 수신합니다.
 * <p>API Gateway에서 헤더 검증을 수행하므로 컨트롤러에서는 직접 검증하지 않습니다.
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

  private final HoldSeatUseCase holdSeatUseCase;
  private final GetReservationUseCase getReservationUseCase;
  private final CancelReservationUseCase cancelReservationUseCase;

  /**
   * 좌석을 예약 홀드합니다.
   *
   * @param userId 사용자 ID (X-User-Id 헤더)
   * @param request 좌석 ID와 공연 ID를 포함하는 요청 본문
   * @return HTTP 201 Created 응답, 예약 ID와 만료 시간을 포함한 HoldSeatResult
   */
  @PostMapping
  public ResponseEntity<ApiResponse<HoldSeatResult>> createReservation(
      @RequestHeader("X-User-Id") Long userId,
      @RequestBody @Valid HoldSeatRequest request) {
    HoldSeatResult result = holdSeatUseCase.holdSeat(
        new HoldSeatUseCase.HoldSeatCommand(userId, request.seatId(), request.eventId())
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result));
  }

  /**
   * 특정 예약을 단건 조회합니다.
   *
   * @param id 예약 ID
   * @param userId 사용자 ID (X-User-Id 헤더)
   * @return HTTP 200 OK 응답, 예약 상세 정보
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ReservationDetail>> getReservation(
      @PathVariable Long id,
      @RequestHeader("X-User-Id") Long userId) {
    ReservationDetail detail = getReservationUseCase.getReservation(id, userId);
    return ResponseEntity.ok(ApiResponse.ok(detail));
  }

  /**
   * 현재 사용자의 모든 예약을 조회합니다.
   *
   * @param userId 사용자 ID (X-User-Id 헤더)
   * @return HTTP 200 OK 응답, 예약 목록
   */
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<List<ReservationDetail>>> getMyReservations(
      @RequestHeader("X-User-Id") Long userId) {
    List<ReservationDetail> details = getReservationUseCase.getMyReservations(userId);
    return ResponseEntity.ok(ApiResponse.ok(details));
  }

  /**
   * 예약을 취소합니다.
   *
   * <p>사용자는 자신의 PENDING 또는 CONFIRMED 예약만 취소할 수 있습니다.
   *
   * @param id 예약 ID
   * @param userId 사용자 ID (X-User-Id 헤더)
   * @return HTTP 200 OK 응답, 빈 데이터
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> cancel(
      @PathVariable Long id,
      @RequestHeader("X-User-Id") Long userId) {
    cancelReservationUseCase.cancel(
        new CancelReservationUseCase.CancelCommand(id, userId, SeatReleasedEvent.ReleaseReason.CANCELLED)
    );
    return ResponseEntity.ok(ApiResponse.ok());
  }
}
