package com.ticketch.reservationservice.adapter.out.persistence;

import com.ticketch.reservationservice.application.port.out.LoadReservationPort;
import com.ticketch.reservationservice.application.port.out.SaveReservationPort;
import com.ticketch.reservationservice.domain.model.Reservation;
import com.ticketch.reservationservice.domain.model.ReservationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * [Persistence Adapter] JPA 기반 예약 저장소 구현체.
 *
 * <p>구현 포트: {@link LoadReservationPort}, {@link SaveReservationPort}
 */
@Component
@RequiredArgsConstructor
public class ReservationPersistenceAdapter implements LoadReservationPort, SaveReservationPort {

    private final ReservationJpaRepository reservationJpaRepository;

    @Override
    public Optional<Reservation> findById(Long id) {
        return reservationJpaRepository.findById(id).map(ReservationJpaEntity::toDomain);
    }

    @Override
    public List<Reservation> findByUserId(Long userId) {
        return reservationJpaRepository.findByUserId(userId).stream()
                .map(ReservationJpaEntity::toDomain).toList();
    }

    @Override
    public List<Reservation> findExpiredPending(LocalDateTime now) {
        return reservationJpaRepository.findByStatusAndExpiresAtBefore(ReservationStatus.PENDING, now).stream()
                .map(ReservationJpaEntity::toDomain).toList();
    }

    @Override
    public boolean existsActiveBySeatId(Long seatId) {
        return reservationJpaRepository.existsBySeatIdAndStatusIn(seatId,
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED));
    }

    @Override
    public Reservation save(Reservation reservation) {
        ReservationJpaEntity saved = reservationJpaRepository.save(ReservationJpaEntity.fromDomain(reservation));
        return saved.toDomain();
    }
}
