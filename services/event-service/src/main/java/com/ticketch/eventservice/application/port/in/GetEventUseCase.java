package com.ticketch.eventservice.application.port.in;

import com.ticketch.eventservice.domain.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * [Input Port] 공연 조회 유스케이스.
 *
 * <p>구현체: {@link com.ticketch.eventservice.application.service.EventQueryService}
 */
public interface GetEventUseCase {

    /** 공연 목록 페이징 조회 */
    Page<Event> getEvents(Pageable pageable);

    /**
     * 공연 단건 조회.
     *
     * @throws com.ticketch.common.exception.BusinessException EVENT_NOT_FOUND
     */
    Event getEvent(Long eventId);
}
