package com.ticketch.reservationservice.domain.model;

import com.ticketch.common.exception.BusinessException;
import com.ticketch.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * [Domain] 예약 도메인 모델.
 *
 * <p>예약의 생명주기(PENDING → CONFIRMED 또는 PENDING → CANCELLED/EXPIRED)를 관리하며,
 * 모든 상태 전이는 메서드를 통해 제어되고, 규칙 위반 시 {@link BusinessException}을 던진다.
 *
 * <p>id와 status 필드는 가변이며, setter 없이 도메인 메서드(confirm, cancel, expire 등)를 통해서만
 * 변경된다. @Builder로 전체 필드 생성자를 유지하되, 인스턴스화 후는 메서드를 통해 상태를 관리한다.
 */
@Getter
@Builder
@AllArgsConstructor
public class Reservation {

    /** 예약 ID (DB 생성) */
    private Long id;

    /** 사용자 ID */
    private Long userId;

    /** 좌석 ID */
    private Long seatId;

    /** 공연 ID */
    private Long eventId;

    /** 예약 상태 */
    private ReservationStatus status;

    /** 예약 만료 시간 */
    private LocalDateTime expiresAt;

    /** 예약 생성 시간 */
    private LocalDateTime createdAt;

    /**
     * 예약을 생성한다.
     *
     * <p>상태는 PENDING으로, createdAt은 현재시간으로 초기화된다.
     *
     * @param userId 사용자 ID
     * @param seatId 좌석 ID
     * @param eventId 공연 ID
     * @param expiresAt 예약 만료 시간
     * @return 생성된 예약
     */
    public static Reservation create(Long userId, Long seatId, Long eventId, LocalDateTime expiresAt) {
        return Reservation.builder()
                .userId(userId)
                .seatId(seatId)
                .eventId(eventId)
                .status(ReservationStatus.PENDING)
                .expiresAt(expiresAt)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 예약이 만료되었는지 확인한다.
     *
     * @param now 현재 시간
     * @return now가 expiresAt 이후이면 true
     */
    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }

    /**
     * 예약을 확정한다 (결제 완료).
     *
     * <p>규칙:
     * <ul>
     *   <li>이미 CONFIRMED이면 {@link ErrorCode#RESERVATION_ALREADY_CONFIRMED} 던짐</li>
     *   <li>CANCELLED 또는 EXPIRED이면 {@link ErrorCode#RESERVATION_EXPIRED} 던짐</li>
     *   <li>PENDING이면 CONFIRMED로 상태 변경</li>
     * </ul>
     *
     * @throws BusinessException 규칙 위반 시
     */
    public void confirm() {
        if (status == ReservationStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.RESERVATION_ALREADY_CONFIRMED);
        }
        if (status == ReservationStatus.CANCELLED || status == ReservationStatus.EXPIRED) {
            throw new BusinessException(ErrorCode.RESERVATION_EXPIRED);
        }
        // status == PENDING
        this.status = ReservationStatus.CONFIRMED;
    }

    /**
     * 예약을 취소한다 (사용자 또는 시스템).
     *
     * <p>규칙:
     * <ul>
     *   <li>이미 CANCELLED이면 {@link ErrorCode#RESERVATION_ALREADY_CANCELLED} 던짐</li>
     *   <li>이미 CONFIRMED이면 {@link ErrorCode#RESERVATION_ALREADY_CONFIRMED} 던짐</li>
     *   <li>PENDING 또는 EXPIRED이면 CANCELLED로 상태 변경</li>
     * </ul>
     *
     * @throws BusinessException 규칙 위반 시
     */
    public void cancel() {
        if (status == ReservationStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.RESERVATION_ALREADY_CANCELLED);
        }
        if (status == ReservationStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.RESERVATION_ALREADY_CONFIRMED);
        }
        // status == PENDING || status == EXPIRED
        this.status = ReservationStatus.CANCELLED;
    }

    /**
     * 예약을 만료시킨다 (멱등).
     *
     * <p>규칙:
     * <ul>
     *   <li>status가 PENDING이면 EXPIRED로 변경</li>
     *   <li>그 외 상태는 무시 (멱등성 보장)</li>
     * </ul>
     */
    public void expire() {
        if (status == ReservationStatus.PENDING) {
            this.status = ReservationStatus.EXPIRED;
        }
        // 다른 상태는 변경하지 않음 (멱등)
    }

    /**
     * 이 예약이 요청자의 소유인지 확인한다.
     *
     * <p>userId가 requesterId와 일치하지 않으면 {@link ErrorCode#RESERVATION_NOT_OWNED}를 던진다.
     *
     * @param requesterId 요청자 ID
     * @throws BusinessException 소유권 검증 실패 시
     */
    public void assertOwnedBy(Long requesterId) {
        if (!userId.equals(requesterId)) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_OWNED);
        }
    }
}
