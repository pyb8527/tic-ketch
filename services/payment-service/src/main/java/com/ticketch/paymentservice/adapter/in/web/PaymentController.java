package com.ticketch.paymentservice.adapter.in.web;

import com.ticketch.common.response.ApiResponse;
import com.ticketch.paymentservice.adapter.in.web.dto.RequestPaymentRequest;
import com.ticketch.paymentservice.application.port.in.CancelPaymentUseCase;
import com.ticketch.paymentservice.application.port.in.GetPaymentUseCase;
import com.ticketch.paymentservice.application.port.in.GetPaymentUseCase.PaymentDetail;
import com.ticketch.paymentservice.application.port.in.RequestPaymentUseCase;
import com.ticketch.paymentservice.application.port.in.RequestPaymentUseCase.PaymentResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [Web Adapter] 결제 요청·조회·취소 컨트롤러.
 *
 * <p>모든 엔드포인트는 X-User-Id 헤더를 통해 사용자 인증 정보를 수신합니다.
 * <p>API Gateway에서 헤더 검증을 수행하므로 컨트롤러에서는 직접 검증하지 않습니다.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

  private final RequestPaymentUseCase requestPaymentUseCase;
  private final GetPaymentUseCase getPaymentUseCase;
  private final CancelPaymentUseCase cancelPaymentUseCase;

  /**
   * 결제를 요청합니다.
   *
   * @param userId 사용자 ID (X-User-Id 헤더)
   * @param request 예약 ID와 결제 금액을 포함하는 요청 본문
   * @return HTTP 201 Created 응답, 결제 ID와 상태를 포함한 PaymentResult
   */
  @PostMapping
  public ResponseEntity<ApiResponse<PaymentResult>> create(
      @RequestHeader("X-User-Id") Long userId,
      @RequestBody @Valid RequestPaymentRequest request) {
    PaymentResult result = requestPaymentUseCase.requestPayment(
        new RequestPaymentUseCase.RequestPaymentCommand(
            userId, request.reservationId(), request.amount()
        )
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result));
  }

  /**
   * 특정 결제를 단건 조회합니다.
   *
   * @param id 결제 ID
   * @param userId 사용자 ID (X-User-Id 헤더)
   * @return HTTP 200 OK 응답, 결제 상세 정보
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<PaymentDetail>> get(
      @PathVariable Long id,
      @RequestHeader("X-User-Id") Long userId) {
    PaymentDetail detail = getPaymentUseCase.getPayment(id, userId);
    return ResponseEntity.ok(ApiResponse.ok(detail));
  }

  /**
   * 결제를 취소하고 환불합니다.
   *
   * <p>사용자는 자신의 COMPLETED 결제만 취소(환불)할 수 있습니다.
   *
   * @param id 결제 ID
   * @param userId 사용자 ID (X-User-Id 헤더)
   * @return HTTP 200 OK 응답, 빈 데이터
   */
  @PostMapping("/{id}/cancel")
  public ResponseEntity<ApiResponse<Void>> cancel(
      @PathVariable Long id,
      @RequestHeader("X-User-Id") Long userId) {
    cancelPaymentUseCase.cancel(id, userId);
    return ResponseEntity.ok(ApiResponse.ok());
  }
}
