package com.ticketch.gateway.filter;

import com.ticketch.common.exception.BusinessException;
import com.ticketch.security.JwtTokenProvider;
import com.ticketch.security.UserPrincipal;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * JWT 인증 GatewayFilterFactory.
 *
 * <p>Spring Cloud Gateway(WebFlux 기반)에서 동작하는 리액티브 인증 필터.
 * {@code Authorization: Bearer <token>} 헤더를 검증하고, 검증 성공 시
 * {@code X-User-Id}, {@code X-User-Role} 헤더를 다운스트림 서비스에 주입한다.
 *
 * <p>보안 정책:
 * <ul>
 *   <li>{@code Authorization} 헤더가 없거나 "Bearer " 접두사가 없으면 HTTP 401 반환.</li>
 *   <li>{@link JwtTokenProvider#validate(String)} 실패(만료·위조) 시 HTTP 401 반환.</li>
 *   <li>클라이언트가 임의로 보낸 {@code X-User-Id}를 {@code h.set(...)}으로 덮어써 스푸핑을 방지.</li>
 * </ul>
 *
 * <p>적용 대상: {@code reservation-service}, {@code payment-service} 라우트
 * (RouteConfig에서 필터로 등록).
 */
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    /** JWT 토큰 검증 및 파싱 컴포넌트 */
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 생성자 — {@link AbstractGatewayFilterFactory}에 Config 타입을 등록하고
     * {@link JwtTokenProvider}를 주입받는다.
     *
     * @param jwtTokenProvider JWT 검증·파싱 컴포넌트
     */
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        super(Config.class);
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 이 필터의 구성 클래스.
     * 현재 추가 구성 프로퍼티는 없으며, 향후 화이트리스트 경로 등을 추가할 수 있다.
     */
    public static class Config {
    }

    /**
     * GatewayFilter 인스턴스를 반환한다.
     *
     * <p>처리 흐름:
     * <ol>
     *   <li>{@code Authorization} 헤더 존재 여부 및 "Bearer " 접두사 확인 → 없으면 401.</li>
     *   <li>토큰 추출 후 {@link JwtTokenProvider#validate(String)} 호출 →
     *       {@link BusinessException} 발생 시 401.</li>
     *   <li>{@link JwtTokenProvider#parseToken(String)}으로 {@link UserPrincipal} 획득.</li>
     *   <li>요청 헤더에 {@code X-User-Id}, {@code X-User-Role}를 주입(기존 값 덮어쓰기).</li>
     *   <li>변이된 exchange로 체인 계속 진행.</li>
     * </ol>
     *
     * @param config 필터 구성 (현재 미사용)
     * @return 리액티브 GatewayFilter 람다
     */
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // 1. Authorization 헤더 검사
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return unauthorized(exchange);
            }

            // 2. 토큰 추출 및 서명 검증
            String token = authHeader.substring(7);
            try {
                jwtTokenProvider.validate(token);
            } catch (BusinessException e) {
                return unauthorized(exchange);
            }

            // 3. 토큰 파싱 → 사용자 주체 획득
            UserPrincipal principal = jwtTokenProvider.parseToken(token);

            // 4. 다운스트림 헤더 주입 (클라이언트 위조 값 덮어쓰기 — 스푸핑 방지)
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .headers(h -> {
                        h.set("X-User-Id", String.valueOf(principal.getUserId()));
                        h.set("X-User-Role", principal.getRole());
                    })
                    .build();

            // 5. 변이된 요청으로 체인 계속
            return chain.filter(exchange.mutate().request(mutated).build());
        };
    }

    /**
     * HTTP 401 Unauthorized 응답을 반환하는 헬퍼 메서드.
     *
     * <p>응답 상태 코드를 {@link HttpStatus#UNAUTHORIZED}로 설정하고
     * 응답을 즉시 완료한다(바디 없음).
     *
     * @param exchange 현재 서버 교환 컨텍스트
     * @return 완료 신호를 나타내는 빈 {@link Mono}
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
