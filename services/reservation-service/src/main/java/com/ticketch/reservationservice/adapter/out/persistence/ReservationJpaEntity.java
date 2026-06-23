package com.ticketch.reservationservice.adapter.out.persistence;

import com.ticketch.reservationservice.domain.model.Reservation;
import com.ticketch.reservationservice.domain.model.ReservationStatus;
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
 * [JPA Entity] reservations 테이블 매핑.
 *
 * <p>예약 도메인 모델을 데이터베이스에 저장하기 위한 영속성 엔티티입니다.
 */
@Entity
@Table(name = "reservations")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationJpaEntity {

    /** 예약 ID (자동 생성) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 사용자 ID */
    @Column(nullable = false)
    private Long userId;

    /** 좌석 ID */
    @Column(nullable = false)
    private Long seatId;

    /** 공연 ID */
    @Column(nullable = false)
    private Long eventId;

    /** 예약 상태 (PENDING, CONFIRMED, CANCELLED, EXPIRED) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    /** 예약 만료 시간 */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /** 예약 생성 시간 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 예약 업데이트 시간.
     *
     * <p>DB가 {@code DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP}로 관리하므로
     * INSERT/UPDATE 문에서 제외한다(insertable/updatable=false). 명시적 NULL 삽입으로 인한
     * NOT NULL 제약 위반을 방지한다.
     */
    @Column(insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    /**
     * JPA 엔티티를 도메인 모델로 변환.
     *
     * @return 도메인 모델 {@link Reservation}
     */
    public Reservation toDomain() {
        return Reservation.builder()
                .id(id)
                .userId(userId)
                .seatId(seatId)
                .eventId(eventId)
                .status(status)
                .expiresAt(expiresAt)
                .createdAt(createdAt)
                .build();
    }

    /**
     * 도메인 모델로부터 JPA 엔티티 생성.
     *
     * <p>createdAt/updatedAt이 null이면 데이터베이스의 기본값(DEFAULT CURRENT_TIMESTAMP)을 사용하도록
     * 필드를 설정하지 않습니다.
     *
     * @param reservation 도메인 모델
     * @return JPA 엔티티
     */
    public static ReservationJpaEntity fromDomain(Reservation reservation) {
        return ReservationJpaEntity.builder()
                .id(reservation.getId())
                .userId(reservation.getUserId())
                .seatId(reservation.getSeatId())
                .eventId(reservation.getEventId())
                .status(reservation.getStatus())
                .expiresAt(reservation.getExpiresAt())
                .createdAt(reservation.getCreatedAt())
                .build();
    }
}
