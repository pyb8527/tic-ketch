package com.ticketch.paymentservice.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * [Spring Data JPA Repository] 결제 엔티티 조회 및 저장.
 *
 * <p>JPA 파생쿼리로 다양한 조회 조건을 지원합니다.
 */
public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, Long> {

	/**
	 * 예약 ID로 결제를 조회한다.
	 *
	 * @param reservationId 예약 ID
	 * @return 결제 엔티티, 없으면 empty Optional
	 */
	Optional<PaymentJpaEntity> findByReservationId(Long reservationId);
}
