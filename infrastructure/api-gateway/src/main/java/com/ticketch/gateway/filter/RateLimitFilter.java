package com.ticketch.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * IP 기반 요청 속도 제한 글로벌 필터.
 *
 * <p>Redis Reactive를 사용하여 초당 IP별 요청 수를 집계하고,
 * {@link #LIMIT}을 초과하는 요청에 대해 429 Too Many Requests를 반환합니다.</p>
 *
 * <ul>
 *   <li>윈도우 크기: {@link #WINDOW} (1초)</li>
 *   <li>허용 임계치: {@link #LIMIT} (초당 20회)</li>
 *   <li>Redis 키 형식: {@code ratelimit:<IP주소>}</li>
 * </ul>
 *
 * <p>첫 번째 요청(count == 1)이 발생할 때 TTL을 설정하여 자동 만료되도록 합니다.</p>
 *
 * @see GlobalFilter
 * @see Ordered
 */
@Slf4j
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    /** Redis Reactive 템플릿 — 생성자 주입. */
    private final ReactiveStringRedisTemplate redisTemplate;

    /** 초당 IP별 허용 최대 요청 수. */
    private static final int LIMIT = 20;

    /** 속도 제한 슬라이딩 윈도우 크기 (1초). */
    private static final Duration WINDOW = Duration.ofSeconds(1);

    /**
     * RateLimitFilter 생성자.
     *
     * @param redisTemplate Redis Reactive 템플릿 (spring-boot-starter-data-redis-reactive 자동 구성)
     */
    public RateLimitFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 요청 IP를 기준으로 속도 제한을 적용합니다.
     *
     * <ol>
     *   <li>Redis INCR로 현재 윈도우 내 요청 수를 증가시킵니다.</li>
     *   <li>첫 번째 요청(count == 1)인 경우 TTL을 {@link #WINDOW}로 설정합니다.</li>
     *   <li>요청 수가 {@link #LIMIT}을 초과하면 429를 반환하고 처리를 종료합니다.</li>
     *   <li>제한 이내라면 다음 필터로 요청을 전달합니다.</li>
     * </ol>
     *
     * @param exchange 현재 서버 웹 교환 객체
     * @param chain    게이트웨이 필터 체인
     * @return 처리 완료를 나타내는 {@link Mono}
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String ip = resolveIp(exchange);
        String key = "ratelimit:" + ip;

        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    Mono<Boolean> ttl = (count != null && count == 1L)
                            ? redisTemplate.expire(key, WINDOW)
                            : Mono.just(true);
                    return ttl.thenReturn(count);
                })
                .flatMap(count -> {
                    if (count != null && count > LIMIT) {
                        log.warn("속도 제한 초과 — IP: {}, count: {}", ip, count);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete();
                    }
                    return chain.filter(exchange);
                });
    }

    /**
     * 요청에서 클라이언트 IP 주소를 추출합니다.
     *
     * <p>원격 주소가 {@code null}인 경우 {@code "unknown"}을 반환합니다.</p>
     *
     * @param exchange 현재 서버 웹 교환 객체
     * @return 클라이언트 IP 주소 문자열 (알 수 없는 경우 {@code "unknown"})
     */
    private String resolveIp(ServerWebExchange exchange) {
        return exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    /**
     * 필터 실행 순서를 반환합니다.
     *
     * <p>음수(-1)를 반환하여 다른 글로벌 필터보다 먼저 실행되도록 합니다.</p>
     *
     * @return 필터 순서 값 {@code -1}
     */
    @Override
    public int getOrder() {
        return -1;
    }
}
