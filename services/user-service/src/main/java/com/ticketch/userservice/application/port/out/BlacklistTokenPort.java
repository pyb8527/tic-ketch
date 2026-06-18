package com.ticketch.userservice.application.port.out;

/**
 * [Output Port] AccessToken 블랙리스트 관리 인터페이스.
 *
 * <p>구현체: {@link com.ticketch.userservice.adapter.out.redis.TokenRedisAdapter}
 * <p>로그아웃된 AccessToken의 JTI를 Redis에 저장하여, 만료 전 재사용을 방지한다.
 * <p>Redis 키: {@code blacklist:jwt:{jti}}, TTL = AccessToken 남은 유효기간
 */
public interface BlacklistTokenPort {

    /**
     * JTI를 블랙리스트에 등록.
     *
     * @param jti   JWT ID (AccessToken에서 추출)
     * @param ttlMs 블랙리스트 유지 시간 (ms, 보통 AccessToken 만료시간과 동일)
     */
    void blacklist(String jti, long ttlMs);

    /**
     * 해당 JTI가 블랙리스트에 등록되어 있는지 확인.
     *
     * @param jti JWT ID
     * @return true면 무효화된 토큰
     */
    boolean isBlacklisted(String jti);
}
