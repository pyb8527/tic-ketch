package com.ticketch.reservationservice.application.port.out;

import com.ticketch.reservationservice.domain.model.Reservation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 예약 조회 포트.
 * 영속성 계층의 예약 데이터 로드 및 검증 기능을 정의합니다.
 */
public interface LoadReservationPort {

    /**
     * ID로 예약 단건 조회.
     *
     * @param id 예약 ID
     * @return 예약 객체 (미존재 시 empty)
     */
    Optional<Reservation> findById(Long id);

    /**
     * 사용자의 모든 예약 조회.
     *
     * @param userId 사용자 ID
     * @return 예약 목록
     */
    List<Reservation> findByUserId(Long userId);

    /**
     * 만료된 미확정 예약 조회.
     * status=PENDING AND expiresAt<now인 예약을 반환합니다.
     *
     * @param now 현재 시간
     * @return 만료된 미확정 예약 목록
     */
    List<Reservation> findExpiredPending(LocalDateTime now);

    /**
     * 좌석의 활성 예약 존재 여부.
     * status이 PENDING 또는 CONFIRMED인 예약이 존재하는지 확인합니다.
     *
     * @param seatId 좌석 ID
     * @return 활성 예약 존재 여부
     */
    boolean existsActiveBySeatId(Long seatId);
}
