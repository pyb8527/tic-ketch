package com.ticketch.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── User ──────────────────────────────────────────────────
    USER_NOT_FOUND(404, "U001", "사용자를 찾을 수 없습니다"),
    DUPLICATE_EMAIL(409, "U002", "이미 사용 중인 이메일입니다"),
    INVALID_PASSWORD(401, "U003", "비밀번호가 올바르지 않습니다"),
    INVALID_TOKEN(401, "U004", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(401, "U005", "만료된 토큰입니다"),
    UNAUTHORIZED(401, "U006", "인증이 필요합니다"),

    // ── Event ─────────────────────────────────────────────────
    EVENT_NOT_FOUND(404, "E001", "공연을 찾을 수 없습니다"),
    EVENT_NOT_ON_SALE(400, "E002", "판매 중인 공연이 아닙니다"),

    // ── Seat ──────────────────────────────────────────────────
    SEAT_NOT_FOUND(404, "S001", "좌석을 찾을 수 없습니다"),
    SEAT_ALREADY_HELD(409, "S002", "이미 선점된 좌석입니다"),
    SEAT_ALREADY_SOLD(409, "S003", "이미 판매된 좌석입니다"),
    SEAT_NOT_AVAILABLE(409, "S004", "선택할 수 없는 좌석입니다"),

    // ── Reservation ───────────────────────────────────────────
    RESERVATION_NOT_FOUND(404, "R001", "예약을 찾을 수 없습니다"),
    RESERVATION_EXPIRED(410, "R002", "예약 시간이 만료되었습니다"),
    RESERVATION_ALREADY_CANCELLED(409, "R003", "이미 취소된 예약입니다"),
    RESERVATION_ALREADY_CONFIRMED(409, "R004", "이미 확정된 예약입니다"),
    RESERVATION_NOT_OWNED(403, "R005", "본인의 예약이 아닙니다"),

    // ── Payment ───────────────────────────────────────────────
    PAYMENT_NOT_FOUND(404, "P001", "결제 정보를 찾을 수 없습니다"),
    PAYMENT_ALREADY_COMPLETED(409, "P002", "이미 완료된 결제입니다"),
    PAYMENT_FAILED(400, "P003", "결제에 실패했습니다"),
    PAYMENT_ALREADY_REFUNDED(409, "P004", "이미 환불된 결제입니다"),

    // ── Common ────────────────────────────────────────────────
    INVALID_INPUT(400, "C001", "잘못된 입력값입니다"),
    ACCESS_DENIED(403, "C002", "접근 권한이 없습니다"),
    LOCK_ACQUISITION_FAILED(409, "C003", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요"),
    INTERNAL_SERVER_ERROR(500, "C004", "서버 오류가 발생했습니다");

    private final int status;
    private final String code;
    private final String message;
}
