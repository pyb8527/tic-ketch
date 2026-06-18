package com.ticketch.userservice.application.port.out;

/**
 * [Output Port] RefreshToken 저장/삭제 인터페이스.
 *
 * <p>구현체: {@link com.ticketch.userservice.adapter.out.redis.TokenRedisAdapter}
 * <p>Redis 키: {@code refresh:token:{userId}}, TTL = jwt.refresh-expiry
 */
public interface SaveRefreshTokenPort {

    /**
     * RefreshToken을 Redis에 저장 (기존 토큰은 덮어씀 — Refresh Token Rotation).
     *
     * @param userId       사용자 ID (Redis 키)
     * @param refreshToken 저장할 RefreshToken
     */
    void save(Long userId, String refreshToken);

    /**
     * 로그아웃 시 Redis에서 RefreshToken 삭제.
     *
     * @param userId 사용자 ID
     */
    void revoke(Long userId);
}
