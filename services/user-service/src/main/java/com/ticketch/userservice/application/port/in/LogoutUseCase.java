package com.ticketch.userservice.application.port.in;

/**
 * [Input Port] 로그아웃 유스케이스 인터페이스.
 *
 * <p>구현체: {@link com.ticketch.userservice.application.service.AuthService}
 * <p>AccessToken을 블랙리스트에 등록하고, RefreshToken을 Redis에서 삭제한다.
 */
public interface LogoutUseCase {

    /**
     * 토큰 무효화 처리.
     *
     * @param accessToken  Bearer 헤더에서 추출한 AccessToken
     * @param refreshToken Refresh-Token 헤더에서 추출한 RefreshToken
     */
    void logout(String accessToken, String refreshToken);
}
