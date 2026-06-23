package com.ticketch.reservationservice.application.service;

import com.ticketch.common.exception.BusinessException;
import com.ticketch.common.exception.ErrorCode;
import com.ticketch.reservationservice.application.port.in.GetReservationUseCase;
import com.ticketch.reservationservice.application.port.out.LoadReservationPort;
import com.ticketch.reservationservice.domain.model.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 조회 애플리케이션 서비스.
 *
 * <p>사용자의 예약 정보 조회 및 목록 조회 기능을 구현합니다.
 * 소유권 검증 및 남은 시간 계산을 포함합니다.
 */
@Service
@RequiredArgsConstructor
public class GetReservationService implements GetReservationUseCase {

    private final LoadReservationPort loadReservationPort;

    /**
     * 예약 단건을 조회합니다.
     *
     * <p>요청자가 예약의 소유자인지 검증한 후 상세 정보를 반환합니다.
     *
     * @param reservationId 예약 ID
     * @param requesterId 요청자 ID
     * @return 예약 상세 정보
     * @throws BusinessException 예약 미존재 또는 소유권 검증 실패 시
     */
    @Override
    public ReservationDetail getReservation(Long reservationId, Long requesterId) {
        Reservation r = loadReservationPort.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
        r.assertOwnedBy(requesterId);
        return toDetail(r);
    }

    /**
     * 사용자의 모든 예약을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 예약 상세 정보 목록
     */
    @Override
    public List<ReservationDetail> getMyReservations(Long userId) {
        return loadReservationPort.findByUserId(userId)
                .stream()
                .map(this::toDetail)
                .toList();
    }

    /**
     * 예약 도메인 모델을 상세 정보로 변환합니다.
     *
     * <p>남은 시간을 초 단위로 계산하여 포함합니다.
     * 현재시간이 만료시간을 지났다면 0을 반환합니다.
     *
     * @param r 예약 도메인 모델
     * @return 예약 상세 정보
     */
    private ReservationDetail toDetail(Reservation r) {
        long remaining = Math.max(0,
                Duration.between(LocalDateTime.now(), r.getExpiresAt()).getSeconds());
        return new ReservationDetail(r.getId(), r.getSeatId(), r.getEventId(),
                r.getStatus().name(), r.getExpiresAt(), remaining);
    }
}
