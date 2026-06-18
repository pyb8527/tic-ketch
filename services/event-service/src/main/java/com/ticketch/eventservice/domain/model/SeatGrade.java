package com.ticketch.eventservice.domain.model;

import lombok.Builder;
import lombok.Getter;

/**
 * [Domain] 좌석 등급 도메인 모델.
 *
 * <p>공연별 좌석 등급(VIP, R석, S석 등)과 가격 정보를 담는다.
 * 좌석 배치도 UI에서 등급별 색상 구분에 {@code colorCode}를 사용한다.
 */
@Getter
@Builder
public class SeatGrade {

    private Long id;
    private Long eventId;
    private String gradeName;
    private Integer price;
    /** 좌석 배치도 색상 코드 (예: #FF5733) */
    private String colorCode;
}
