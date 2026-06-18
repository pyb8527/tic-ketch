package com.ticketch.userservice.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * [Domain] RefreshToken 도메인 모델.
 *
 * <p>실제 저장은 Redis({@link com.ticketch.userservice.adapter.out.redis.TokenRedisAdapter})에
 * 위임하므로, 이 클래스는 도메인 개념 표현 용도로만 사용된다.
 * Redis 키: {@code refresh:token:{userId}}
 */
@Getter
@Builder
public class RefreshToken {

    private Long id;
    private Long userId;
    /** SHA-256 해시된 토큰값 */
    private String tokenHash;
    private LocalDateTime expiresAt;
    private boolean revoked;
}
