package com.ticketch.paymentservice.application.port.out;

import com.ticketch.paymentservice.domain.model.Payment;
import java.util.Optional;

/**
 * 결제 정보 조회 Output Port.
 * ID 또는 예약ID로 결제를 검색한다.
 */
public interface LoadPaymentPort {
	/**
	 * 결제 ID로 결제를 조회한다.
	 *
	 * @param id 결제 ID
	 * @return 결제 도메인 모델, 없으면 empty Optional
	 */
	Optional<Payment> findById(Long id);

	/**
	 * 예약 ID로 결제를 조회한다.
	 *
	 * @param reservationId 예약 ID
	 * @return 결제 도메인 모델, 없으면 empty Optional
	 */
	Optional<Payment> findByReservationId(Long reservationId);
}
