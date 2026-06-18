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

@Component
@RequiredArgsConstructor
public class TokenRedisAdapter implements SaveRefreshTokenPort, LoadRefreshTokenPort, BlacklistTokenPort {

    private static final String REFRESH_KEY  = "refresh:token:";
    private static final String BLACKLIST_KEY = "blacklist:jwt:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    @Override
    public void save(Long userId, String refreshToken) {
        long ttlSeconds = jwtProperties.getRefreshExpiry() / 1000;
        redisTemplate.opsForValue()
                .set(REFRESH_KEY + userId, refreshToken, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void revoke(Long userId) {
        redisTemplate.delete(REFRESH_KEY + userId);
    }

    @Override
    public Optional<String> findByUserId(Long userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(REFRESH_KEY + userId));
    }

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
