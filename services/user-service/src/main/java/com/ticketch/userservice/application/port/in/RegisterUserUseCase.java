package com.ticketch.userservice.application.port.in;

/**
 * [Input Port] 회원가입 유스케이스 인터페이스.
 *
 * <p>구현체: {@link com.ticketch.userservice.application.service.RegisterUserService}
 * <p>호출자: {@link com.ticketch.userservice.adapter.in.web.AuthController}
 */
public interface RegisterUserUseCase {

    /**
     * 신규 회원을 등록하고 생성된 사용자 ID를 반환한다.
     *
     * @param command 이메일, 원문 비밀번호, 이름
     * @return 생성된 사용자 ID
     * @throws com.ticketch.common.exception.BusinessException DUPLICATE_EMAIL — 이메일 중복 시
     */
    Long register(RegisterCommand command);

    /** 회원가입 요청 커맨드 */
    record RegisterCommand(String email, String rawPassword, String name) {}
}
