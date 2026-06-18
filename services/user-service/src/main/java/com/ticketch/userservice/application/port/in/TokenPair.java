package com.ticketch.userservice.application.port.in;

/**
 * [Value Object] AccessToken + RefreshToken 발급 결과.
 *
 * <p>로그인({@link LoginUseCase}), 토큰 재발급({@link ReissueTokenUseCase})에서 공통 사용.
 */
public record TokenPair(
        /** JWT AccessToken (유효기간 15분) */
        String accessToken,
        /** JWT RefreshToken (유효기간 7일, Redis에 저장) */
        String refreshToken
) {}
