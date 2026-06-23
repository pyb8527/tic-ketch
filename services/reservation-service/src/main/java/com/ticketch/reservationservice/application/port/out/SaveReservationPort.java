package com.ticketch.reservationservice.application.port.out;

import com.ticketch.reservationservice.domain.model.Reservation;

/**
 * 예약 저장 포트.
 * 예약 데이터를 영속성 계층에 저장합니다.
 */
public interface SaveReservationPort {

    /**
     * 예약 저장 또는 업데이트.
     * ID를 채워서 반환합니다.
     *
     * @param reservation 저장할 예약 객체
     * @return ID가 채워진 예약 객체
     */
    Reservation save(Reservation reservation);
}
