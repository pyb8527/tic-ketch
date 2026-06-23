package com.ticketch.paymentservice.domain.model;

/**
 * [Domain] 결제 상태 열거형.
 *
 * <p>결제의 생명주기를 나타낸다:
 * <ul>
 *   <li>PENDING: 결제 대기 중</li>
 *   <li>COMPLETED: 결제 완료</li>
 *   <li>FAILED: 결제 실패</li>
 *   <li>REFUNDED: 환불 완료</li>
 * </ul>
 */
public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED
}
