package com.ticketch.eventservice.adapter.in.web.dto;

import com.ticketch.eventservice.domain.model.Event;

import java.time.LocalDateTime;

/** 공연 응답 DTO */
public record EventResponse(
        Long id,
        String title,
        String venue,
        String category,
        String posterUrl,
        LocalDateTime eventDate,
        String status
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(), event.getTitle(), event.getVenue(),
                event.getCategory(), event.getPosterUrl(),
                event.getEventDate(), event.getStatus().name()
        );
    }
}
