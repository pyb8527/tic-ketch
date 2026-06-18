package com.ticketch.userservice.application.port.out;

import java.util.Optional;

/**
 * [Output Port] RefreshToken 조회 인터페이스.
 *
 * <p>구현체: {@link com.ticketch.userservice.adapter.out.redis.TokenRedisAdapter}
 * <p>토큰 재발급 시 Redis에 저장된 토큰 존재 여부를 검증하는 데 사용한다.
 */
public interface LoadRefreshTokenPort {

    /**
     * userId로 Redis에 저장된 RefreshToken 조회.
     *
     * @param userId 사용자 ID
     * @return 저장된 토큰 (없으면 Optional.empty — 로그아웃 상태)
     */
    Optional<String> findByUserId(Long userId);
}
