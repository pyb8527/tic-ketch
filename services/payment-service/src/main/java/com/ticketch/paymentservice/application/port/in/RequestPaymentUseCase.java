package com.ticketch.paymentservice.application.port.in;

/**
 * 결제 요청 UseCase
 * 예약에 대한 결제를 처리하고 결과를 반환합니다.
 */
public interface RequestPaymentUseCase {

    /**
     * 결제를 요청합니다.
     *
     * @param command 결제 요청 커맨드
     * @return 결제 결과
     */
    PaymentResult requestPayment(RequestPaymentCommand command);

    /**
     * 결제 요청 커맨드
     *
     * @param userId 사용자 ID
     * @param reservationId 예약 ID
     * @param amount 결제 금액
     */
    record RequestPaymentCommand(Long userId, Long reservationId, Integer amount) {}

    /**
     * 결제 결과
     *
     * @param paymentId 결제 ID
     * @param status 결제 상태
     */
    record PaymentResult(Long paymentId, String status) {}
}
