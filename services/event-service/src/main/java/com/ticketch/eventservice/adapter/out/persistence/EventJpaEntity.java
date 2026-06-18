package com.ticketch.eventservice.adapter.out.persistence;

import com.ticketch.eventservice.domain.model.Event;
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

/** [JPA Entity] events 테이블 매핑 */
@Entity
@Table(name = "events")
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class EventJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String venue;

    @Column(nullable = false)
    private LocalDateTime eventDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Event.EventStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Event toDomain() {
        return Event.builder().id(id).title(title).venue(venue)
                .eventDate(eventDate).status(status).createdAt(createdAt).build();
    }

    public static EventJpaEntity fromDomain(Event event) {
        return EventJpaEntity.builder().id(event.getId()).title(event.getTitle())
                .venue(event.getVenue()).eventDate(event.getEventDate())
                .status(event.getStatus()).createdAt(event.getCreatedAt()).build();
    }
}
