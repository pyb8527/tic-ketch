package com.ticketch.userservice.application.port.out;

/**
 * [Output Port] 비밀번호 인코딩/검증 인터페이스.
 *
 * <p>헥사고날 원칙에 따라 도메인/애플리케이션 레이어가 BCrypt 같은 구현 기술을 직접 알지 못하도록 분리.
 * <p>구현체: {@link com.ticketch.userservice.adapter.out.security.BCryptPasswordAdapter}
 */
public interface EncodePasswordPort {

    /**
     * 원문 비밀번호를 단방향 해시로 인코딩.
     *
     * @param rawPassword 사용자가 입력한 원문 비밀번호
     * @return BCrypt 인코딩된 비밀번호
     */
    String encode(String rawPassword);

    /**
     * 원문 비밀번호와 인코딩된 비밀번호 일치 여부 확인.
     *
     * @param rawPassword     원문 비밀번호
     * @param encodedPassword 저장된 인코딩 비밀번호
     */
    boolean matches(String rawPassword, String encodedPassword);
}
