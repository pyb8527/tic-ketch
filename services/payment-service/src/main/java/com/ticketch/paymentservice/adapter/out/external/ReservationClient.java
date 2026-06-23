package com.ticketch.paymentservice.adapter.out.external;

import com.ticketch.common.exception.BusinessException;
import com.ticketch.common.exception.ErrorCode;
import com.ticketch.paymentservice.application.port.out.ValidateReservationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 예약 서비스의 예약 검증 포트 구현.
 * Feign 클라이언트를 통해 reservation-service와 통신하여 예약 정보 확인.
 */
@Component
@RequiredArgsConstructor
public class ReservationClient implements ValidateReservationPort {

    private final ReservationFeignClient reservationFeignClient;

    /**
     * 특정 예약의 상세 정보를 조회하여 결제 검증에 필요한 정보를 반환.
     *
     * @param reservationId 예약 ID
     * @param userId 사용자 ID
     * @return 예약 정보 (좌석ID, 이벤트ID, 상태)
     * @throws BusinessException RESERVATION_NOT_FOUND 예약이 없거나 응답이 비정상인 경우
     */
    @Override
    public ReservationInfo getReservation(Long reservationId, Long userId) {
        ReservationApiResponse resp = reservationFeignClient.getReservation(reservationId, userId);

        if (resp == null || resp.data() == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        ReservationDetailDto d = resp.data();
        return new ReservationInfo(d.seatId(), d.eventId(), d.status());
    }
}
