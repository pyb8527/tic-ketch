package com.ticketch.paymentservice.application.port.in;

import java.time.LocalDateTime;

/**
 * 결제 조회 UseCase
 * 결제 정보를 조회합니다.
 */
public interface GetPaymentUseCase {

    /**
     * 결제 정보를 조회합니다.
     *
     * @param paymentId 결제 ID
     * @param userId 사용자 ID (소유권 확인용)
     * @return 결제 상세정보
     */
    PaymentDetail getPayment(Long paymentId, Long userId);

    /**
     * 결제 상세정보
     *
     * @param id 결제 ID
     * @param reservationId 예약 ID
     * @param amount 결제 금액
     * @param status 결제 상태
     * @param paidAt 결제 완료 시간
     */
    record PaymentDetail(Long id, Long reservationId, Integer amount, String status, LocalDateTime paidAt) {}
}
