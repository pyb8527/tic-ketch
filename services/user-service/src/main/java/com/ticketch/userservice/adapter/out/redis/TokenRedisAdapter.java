package com.ticketch.userservice.adapter.out.redis;

import com.ticketch.security.JwtProperties;
import com.ticketch.userservice.application.port.out.BlacklistTokenPort;
import com.ticketch.userservice.application.port.out.LoadRefreshTokenPort;
import com.ticketch.userservice.application.port.out.SaveRefreshTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * [Redis Adapter] RefreshToken 및 AccessToken 블랙리스트 관리 구현체.
 *
 * <p>구현 포트: {@link SaveRefreshTokenPort}, {@link LoadRefreshTokenPort}, {@link BlacklistTokenPort}
 *
 * <p>Redis 키 구조:
 * <ul>
 *   <li>{@code refresh:token:{userId}} — RefreshToken 저장, TTL = jwt.refresh-expiry</li>
 *   <li>{@code blacklist:jwt:{jti}}    — 로그아웃 AccessToken JTI, TTL = jwt.access-expiry</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class TokenRedisAdapter implements SaveRefreshTokenPort, LoadRefreshTokenPort, BlacklistTokenPort {

    private static final String REFRESH_KEY   = "refresh:token:";
    private static final String BLACKLIST_KEY = "blacklist:jwt:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    /** RefreshToken 저장 — 같은 userId로 재로그인 시 기존 토큰을 덮어씀 (Refresh Token Rotation) */
    @Override
    public void save(Long userId, String refreshToken) {
        long ttlSeconds = jwtProperties.getRefreshExpiry() / 1000;
        redisTemplate.opsForValue()
                .set(REFRESH_KEY + userId, refreshToken, ttlSeconds, TimeUnit.SECONDS);
    }

    /** 로그아웃 시 RefreshToken 삭제 */
    @Override
    public void revoke(Long userId) {
        redisTemplate.delete(REFRESH_KEY + userId);
    }

    @Override
    public Optional<String> findByUserId(Long userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(REFRESH_KEY + userId));
    }

    /**
     * AccessToken JTI 블랙리스트 등록.
     * TTL을 AccessToken 만료시간으로 설정하여 만료 후 자동 제거된다.
     */
    @Override
    public void blacklist(String jti, long ttlMs) {
        redisTemplate.opsForValue()
                .set(BLACKLIST_KEY + jti, "1", ttlMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY + jti));
    }
}
