package com.ticketch.userservice.application.port.in;

/**
 * [Input Port] 로그인 유스케이스 인터페이스.
 *
 * <p>구현체: {@link com.ticketch.userservice.application.service.AuthService}
 * <p>호출자: {@link com.ticketch.userservice.adapter.in.web.AuthController}
 */
public interface LoginUseCase {

    /**
     * 이메일/비밀번호 검증 후 AccessToken과 RefreshToken을 발급한다.
     *
     * @param command 이메일, 원문 비밀번호
     * @return AccessToken + RefreshToken 쌍
     * @throws com.ticketch.common.exception.BusinessException USER_NOT_FOUND — 미가입 이메일
     * @throws com.ticketch.common.exception.BusinessException INVALID_PASSWORD — 비밀번호 불일치
     */
    TokenPair login(LoginCommand command);

    /** 로그인 요청 커맨드 */
    record LoginCommand(String email, String rawPassword) {}
}
