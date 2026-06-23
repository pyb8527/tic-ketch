package com.ticketch.paymentservice.application.port.in;

/**
 * 결제 취소 UseCase
 * 완료된 결제를 환불처리합니다.
 */
public interface CancelPaymentUseCase {

    /**
     * 결제를 취소하고 환불합니다.
     *
     * @param paymentId 결제 ID
     * @param userId 사용자 ID (소유권 확인용)
     */
    void cancel(Long paymentId, Long userId);
}
