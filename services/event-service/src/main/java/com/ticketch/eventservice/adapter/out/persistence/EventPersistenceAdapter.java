package com.ticketch.eventservice.adapter.out.persistence;

import com.ticketch.eventservice.application.port.out.LoadEventPort;
import com.ticketch.eventservice.application.port.out.LoadSeatPort;
import com.ticketch.eventservice.application.port.out.SaveEventPort;
import com.ticketch.eventservice.application.port.out.UpdateSeatStatusPort;
import com.ticketch.eventservice.domain.model.Event;
import com.ticketch.eventservice.domain.model.Seat;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * [Persistence Adapter] JPA 기반 공연·좌석 저장소 구현체.
 *
 * <p>구현 포트: {@link LoadEventPort}, {@link LoadSeatPort}, {@link SaveEventPort}, {@link UpdateSeatStatusPort}
 */
@Component
@RequiredArgsConstructor
public class EventPersistenceAdapter implements LoadEventPort, LoadSeatPort, SaveEventPort, UpdateSeatStatusPort {

    private final EventJpaRepository eventJpaRepository;
    private final SeatJpaRepository seatJpaRepository;

    @Override public Page<Event> findAll(Pageable pageable) {
        return eventJpaRepository.findAll(pageable).map(EventJpaEntity::toDomain);
    }

    @Override public Optional<Event> findEventById(Long eventId) {
        return eventJpaRepository.findById(eventId).map(EventJpaEntity::toDomain);
    }

    @Override public List<Seat> findByEventId(Long eventId) {
        return seatJpaRepository.findByEventId(eventId).stream()
                .map(SeatJpaEntity::toDomain).toList();
    }

    @Override public Optional<Seat> findSeatById(Long seatId) {
        return seatJpaRepository.findById(seatId).map(SeatJpaEntity::toDomain);
    }

    @Override public Long saveEvent(Event event) {
        return eventJpaRepository.save(EventJpaEntity.fromDomain(event)).getId();
    }

    @Override public List<Long> saveSeats(List<Seat> seats) {
        return seatJpaRepository.saveAll(seats.stream().map(SeatJpaEntity::fromDomain).toList())
                .stream().map(SeatJpaEntity::getId).toList();
    }

    @Override public void updateStatus(Long seatId, Seat.SeatStatus newStatus) {
        seatJpaRepository.updateStatus(seatId, newStatus);
    }
}
