package com.ticketch.userservice.application.port.in;

import com.ticketch.userservice.domain.model.User;

/**
 * [Input Port] 내 프로필 조회 유스케이스 인터페이스.
 *
 * <p>구현체: {@link com.ticketch.userservice.application.service.AuthService}
 * <p>JWT 필터가 SecurityContext에 설정한 userId를 기반으로 사용자 정보를 조회한다.
 */
public interface GetMyProfileUseCase {

    /**
     * userId로 사용자 도메인 객체 조회.
     *
     * @param userId JWT 토큰에서 파싱된 사용자 ID
     * @throws com.ticketch.common.exception.BusinessException USER_NOT_FOUND
     */
    User getMyProfile(Long userId);
}
