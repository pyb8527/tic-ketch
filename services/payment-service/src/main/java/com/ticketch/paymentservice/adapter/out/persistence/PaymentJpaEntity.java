package com.ticketch.paymentservice.adapter.out.persistence;

import com.ticketch.paymentservice.domain.model.Payment;
import com.ticketch.paymentservice.domain.model.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * [JPA Entity] payments 테이블 매핑.
 *
 * <p>결제 도메인 모델을 데이터베이스에 저장하기 위한 영속성 엔티티입니다.
 */
@Entity
@Table(name = "payments")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentJpaEntity {

	/** 결제 ID (자동 생성) */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 예약 ID */
	@Column(nullable = false, unique = true)
	private Long reservationId;

	/** 사용자 ID */
	@Column(nullable = false)
	private Long userId;

	/** 결제 금액 */
	@Column(nullable = false)
	private Integer amount;

	/** 결제 상태 (PENDING, COMPLETED, FAILED, REFUNDED) */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PaymentStatus status;

	/** 결제 실패 사유 */
	@Column(length = 500)
	private String failureReason;

	/** 결제 완료 시간 */
	private LocalDateTime paidAt;

	/** 결제 생성 시간 */
	@Column(nullable = false)
	private LocalDateTime createdAt;

	/**
	 * JPA 엔티티를 도메인 모델로 변환.
	 *
	 * @return 도메인 모델 {@link Payment}
	 */
	public Payment toDomain() {
		return Payment.builder()
				.id(id)
				.reservationId(reservationId)
				.userId(userId)
				.amount(amount)
				.status(status)
				.failureReason(failureReason)
				.paidAt(paidAt)
				.createdAt(createdAt)
				.build();
	}

	/**
	 * 도메인 모델로부터 JPA 엔티티 생성.
	 *
	 * <p>createdAt은 도메인 값(NOT NULL)을 그대로 설정한다.
	 * paidAt과 failureReason은 nullable이다.
	 *
	 * @param payment 도메인 모델
	 * @return JPA 엔티티
	 */
	public static PaymentJpaEntity fromDomain(Payment payment) {
		return PaymentJpaEntity.builder()
				.id(payment.getId())
				.reservationId(payment.getReservationId())
				.userId(payment.getUserId())
				.amount(payment.getAmount())
				.status(payment.getStatus())
				.failureReason(payment.getFailureReason())
				.paidAt(payment.getPaidAt())
				.createdAt(payment.getCreatedAt())
				.build();
	}
}
