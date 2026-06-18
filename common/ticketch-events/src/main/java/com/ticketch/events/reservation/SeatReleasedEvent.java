package com.ticketch.events.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatReleasedEvent {

    private Long reservationId;
    private Long seatId;
    private Long eventId;
    private ReleaseReason reason;

    public enum ReleaseReason {
        EXPIRED,        // TTL 만료
        CANCELLED,      // 사용자 직접 취소
        PAYMENT_FAILED  // 결제 실패
    }
}
