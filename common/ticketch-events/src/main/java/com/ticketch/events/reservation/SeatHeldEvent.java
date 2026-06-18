package com.ticketch.events.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatHeldEvent {

    private Long reservationId;
    private Long seatId;
    private Long eventId;
    private Long userId;
    private LocalDateTime expiresAt;
}
