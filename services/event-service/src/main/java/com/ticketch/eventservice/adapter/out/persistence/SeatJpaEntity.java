package com.ticketch.eventservice.adapter.out.persistence;

import com.ticketch.eventservice.domain.model.Seat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** [JPA Entity] seats 테이블 매핑. {@code @Version}으로 Optimistic Lock 적용 */
@Entity
@Table(name = "seats")
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class SeatJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private Long seatGradeId;

    @Column(nullable = false, length = 10)
    private String rowName;

    @Column(nullable = false)
    private Integer seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Seat.SeatStatus status;

    /** Optimistic Lock — 동시 상태 변경 충돌 감지 (Redisson 분산락과 2중 보호) */
    @Version
    private Long version;

    public Seat toDomain() {
        return Seat.builder().id(id).eventId(eventId).seatGradeId(seatGradeId)
                .rowName(rowName).seatNumber(seatNumber).status(status).version(version).build();
    }

    public static SeatJpaEntity fromDomain(Seat seat) {
        return SeatJpaEntity.builder().id(seat.getId()).eventId(seat.getEventId())
                .seatGradeId(seat.getSeatGradeId()).rowName(seat.getRowName())
                .seatNumber(seat.getSeatNumber()).status(seat.getStatus()).version(seat.getVersion()).build();
    }
}
