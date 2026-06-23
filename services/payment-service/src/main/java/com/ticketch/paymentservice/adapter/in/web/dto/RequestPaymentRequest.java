package com.ticketch.paymentservice.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 결제 요청 DTO
 * 클라이언트로부터 수신하는 결제 요청 데이터를 담습니다.
 *
 * @param reservationId 예약 ID (필수, null 불가)
 * @param amount 결제 금액 (필수, 양수)
 */
public record RequestPaymentRequest(
    @NotNull Long reservationId,
    @NotNull @Positive Integer amount
) {}
