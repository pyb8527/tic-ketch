package com.ticketch.reservationservice.adapter.out.persistence;

import com.ticketch.reservationservice.domain.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * [Spring Data JPA Repository] 예약 엔티티 조회 및 저장.
 *
 * <p>JPA 파생쿼리로 다양한 조회 조건을 지원합니다.
 */
public interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, Long> {

    /**
     * 사용자의 모든 예약 조회.
     *
     * @param userId 사용자 ID
     * @return 예약 목록
     */
    List<ReservationJpaEntity> findByUserId(Long userId);

    /**
     * 특정 상태이면서 만료 시간 이전의 예약 조회.
     *
     * <p>주로 PENDING 상태이면서 현재 시간을 지난 예약을 찾는데 사용됩니다.
     *
     * @param status 예약 상태
     * @param now 기준 시간
     * @return 조건을 만족하는 예약 목록
     */
    List<ReservationJpaEntity> findByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime now);

    /**
     * 특정 좌석에 대해 주어진 상태 중 하나를 가진 예약이 존재하는지 확인.
     *
     * <p>주로 PENDING 또는 CONFIRMED 상태의 활성 예약 존재 여부를 확인합니다.
     *
     * @param seatId 좌석 ID
     * @param statuses 상태 컬렉션
     * @return 존재 여부
     */
    boolean existsBySeatIdAndStatusIn(Long seatId, Collection<ReservationStatus> statuses);
}
