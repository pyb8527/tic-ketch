package com.ticketch.paymentservice.application.port.out;

import com.ticketch.paymentservice.domain.model.Payment;

/**
 * 결제 정보 저장 Output Port.
 * 결제 도메인 모델을 영속성 계층에 저장한다.
 */
public interface SavePaymentPort {
	/**
	 * 결제를 저장한다. ID가 채워져서 반환된다.
	 *
	 * @param payment 저장할 결제 도메인 모델
	 * @return ID가 채워진 결제 도메인 모델
	 */
	Payment save(Payment payment);
}
