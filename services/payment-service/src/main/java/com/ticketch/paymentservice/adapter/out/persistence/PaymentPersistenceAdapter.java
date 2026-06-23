package com.ticketch.paymentservice.adapter.out.persistence;

import com.ticketch.paymentservice.application.port.out.LoadPaymentPort;
import com.ticketch.paymentservice.application.port.out.SavePaymentPort;
import com.ticketch.paymentservice.domain.model.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * [Persistence Adapter] JPA 기반 결제 저장소 구현체.
 *
 * <p>구현 포트: {@link LoadPaymentPort}, {@link SavePaymentPort}
 */
@Component
@RequiredArgsConstructor
public class PaymentPersistenceAdapter implements LoadPaymentPort, SavePaymentPort {

	private final PaymentJpaRepository paymentJpaRepository;

	@Override
	public Optional<Payment> findById(Long id) {
		return paymentJpaRepository.findById(id).map(PaymentJpaEntity::toDomain);
	}

	@Override
	public Optional<Payment> findByReservationId(Long reservationId) {
		return paymentJpaRepository.findByReservationId(reservationId).map(PaymentJpaEntity::toDomain);
	}

	@Override
	public Payment save(Payment payment) {
		PaymentJpaEntity saved = paymentJpaRepository.save(PaymentJpaEntity.fromDomain(payment));
		return saved.toDomain();
	}
}
