package com.ticketch.reservationservice.domain.model;

/**
 * [Domain] 예약 상태 열거형.
 *
 * <p>예약의 생명주기는 다음과 같습니다:
 * <ul>
 *   <li>PENDING: 초기 상태 (좌석 임시 선점 중)</li>
 *   <li>CONFIRMED: 결제 완료 상태 (좌석 확정 판매)</li>
 *   <li>CANCELLED: 취소 상태 (사용자/시스템 취소)</li>
 *   <li>EXPIRED: 만료 상태 (5분 타임아웃 초과)</li>
 * </ul>
 */
public enum ReservationStatus {
    /** 초기 상태 — 좌석 임시 선점 중 (Redis TTL 5분) */
    PENDING,

    /** 결제 완료 상태 — 좌석 확정 판매 */
    CONFIRMED,

    /** 취소 상태 — 사용자 또는 시스템 취소 */
    CANCELLED,

    /** 만료 상태 — 5분 타임아웃 초과 */
    EXPIRED
}
