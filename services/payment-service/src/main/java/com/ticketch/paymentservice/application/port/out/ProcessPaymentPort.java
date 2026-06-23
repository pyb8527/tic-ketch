package com.ticketch.paymentservice.application.port.out;

/**
 * 결제 처리 Output Port (목업 PG).
 * 외부 PG(결제 게이트웨이)에 결제를 요청한다.
 */
public interface ProcessPaymentPort {
	/**
	 * 결제를 처리한다.
	 *
	 * @param reservationId 예약 ID
	 * @param amount 결제액
	 * @return 결제 처리 결과
	 */
	ProcessResult process(Long reservationId, Integer amount);

	/**
	 * 결제 처리 결과.
	 *
	 * @param success 성공 여부
	 * @param failureReason 실패 사유 (성공 시 null)
	 */
	record ProcessResult(boolean success, String failureReason) {}
}
