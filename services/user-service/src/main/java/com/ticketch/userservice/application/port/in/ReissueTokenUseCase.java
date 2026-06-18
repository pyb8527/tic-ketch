package com.ticketch.userservice.application.port.in;

/**
 * [Input Port] AccessToken 재발급 유스케이스 인터페이스.
 *
 * <p>구현체: {@link com.ticketch.userservice.application.service.AuthService}
 * <p>RefreshToken을 검증하고, 새로운 AccessToken + RefreshToken을 발급한다 (Refresh Token Rotation).
 */
public interface ReissueTokenUseCase {

    /**
     * RefreshToken 유효성 검증 후 새 토큰 쌍 발급.
     *
     * @param refreshToken 클라이언트가 보낸 RefreshToken
     * @return 새 AccessToken + RefreshToken 쌍
     * @throws com.ticketch.common.exception.BusinessException INVALID_TOKEN — Redis에 없는 토큰
     * @throws com.ticketch.common.exception.BusinessException EXPIRED_TOKEN — 만료된 토큰
     */
    TokenPair reissue(String refreshToken);
}
