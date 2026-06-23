package com.ticketch.paymentservice.domain.model;

import com.ticketch.common.exception.BusinessException;
import com.ticketch.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * [Domain] 결제 도메인 모델.
 *
 * <p>결제의 생명주기(PENDING → COMPLETED → REFUNDED 또는 PENDING → FAILED)를 관리하며,
 * 모든 상태 전이는 메서드를 통해 제어되고, 규칙 위반 시 {@link BusinessException}을 던진다.
 *
 * <p>status, paidAt, failureReason 필드는 가변이며, setter 없이 도메인 메서드(complete, fail, refund 등)를 통해서만
 * 변경된다. @Builder로 전체 필드 생성자를 유지하되, 인스턴스화 후는 메서드를 통해 상태를 관리한다.
 */
@Getter
@Builder
@AllArgsConstructor
public class Payment {

    /** 결제 ID (DB 생성) */
    private Long id;

    /** 예약 ID */
    private Long reservationId;

    /** 사용자 ID */
    private Long userId;

    /** 결제 금액 */
    private Integer amount;

    /** 결제 상태 */
    private PaymentStatus status;

    /** 결제 실패 사유 */
    private String failureReason;

    /** 결제 완료 시간 */
    private LocalDateTime paidAt;

    /** 결제 생성 시간 */
    private LocalDateTime createdAt;

    /**
     * 결제를 생성한다.
     *
     * <p>상태는 PENDING으로, createdAt은 현재시간으로 초기화되고, paidAt과 failureReason은 null이다.
     *
     * @param reservationId 예약 ID
     * @param userId 사용자 ID
     * @param amount 결제 금액
     * @return 생성된 결제
     */
    public static Payment create(Long reservationId, Long userId, Integer amount) {
        return Payment.builder()
                .reservationId(reservationId)
                .userId(userId)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .paidAt(null)
                .failureReason(null)
                .build();
    }

    /**
     * 결제를 완료한다.
     *
     * <p>규칙:
     * <ul>
     *   <li>이미 COMPLETED이면 {@link ErrorCode#PAYMENT_ALREADY_COMPLETED} 던짐</li>
     *   <li>PENDING이 아니면 {@link ErrorCode#PAYMENT_FAILED} 던짐</li>
     *   <li>PENDING이면 COMPLETED로 상태 변경, paidAt을 현재시간으로 설정</li>
     * </ul>
     *
     * @throws BusinessException 규칙 위반 시
     */
    public void complete() {
        if (status == PaymentStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
        }
        if (status != PaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
        this.status = PaymentStatus.COMPLETED;
        this.paidAt = LocalDateTime.now();
    }

    /**
     * 결제를 실패 처리한다.
     *
     * <p>규칙:
     * <ul>
     *   <li>status를 FAILED로 변경, failureReason 설정</li>
     *   <li>멱등성 허용 (PENDING이 아닌 상태에서도 호출 가능)</li>
     * </ul>
     *
     * @param reason 실패 사유
     */
    public void fail(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    /**
     * 결제를 환불한다.
     *
     * <p>규칙:
     * <ul>
     *   <li>COMPLETED가 아니면:
     *     <ul>
     *       <li>REFUNDED이면 {@link ErrorCode#PAYMENT_ALREADY_REFUNDED} 던짐</li>
     *       <li>그 외 {@link ErrorCode#PAYMENT_FAILED} 던짐</li>
     *     </ul>
     *   </li>
     *   <li>COMPLETED이면 REFUNDED로 상태 변경</li>
     * </ul>
     *
     * @throws BusinessException 규칙 위반 시
     */
    public void refund() {
        if (status != PaymentStatus.COMPLETED) {
            if (status == PaymentStatus.REFUNDED) {
                throw new BusinessException(ErrorCode.PAYMENT_ALREADY_REFUNDED);
            }
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
        this.status = PaymentStatus.REFUNDED;
    }

    /**
     * 이 결제가 요청자의 소유인지 확인한다.
     *
     * <p>userId가 requesterId와 일치하지 않으면 {@link ErrorCode#ACCESS_DENIED}를 던진다.
     *
     * @param requesterId 요청자 ID
     * @throws BusinessException 소유권 검증 실패 시
     */
    public void assertOwnedBy(Long requesterId) {
        if (!this.userId.equals(requesterId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
}
