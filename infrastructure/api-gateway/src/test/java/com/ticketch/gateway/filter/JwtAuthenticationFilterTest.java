package com.ticketch.gateway.filter;

import com.ticketch.common.exception.BusinessException;
import com.ticketch.common.exception.ErrorCode;
import com.ticketch.security.JwtTokenProvider;
import com.ticketch.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * {@link JwtAuthenticationFilter} 단위 테스트.
 *
 * <p>Spring Cloud Gateway WebFlux 환경에서 JWT 인증 필터의 동작을 검증한다.
 * JwtTokenProvider는 Mockito로 모킹하며, Redis/Eureka 등 외부 의존성은 불필요하다.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilterTest.class);

    @Mock
    JwtTokenProvider jwtTokenProvider;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtTokenProvider);
    }

    @Test
    @DisplayName("유효한 토큰이면 X-User-Id 헤더가 주입된다")
    void 유효한_토큰이면_X_User_Id_헤더가_주입된다() {
        log.info("[시나리오] 유효한 Bearer 토큰으로 요청 → X-User-Id 헤더 주입 검증");

        // given
        when(jwtTokenProvider.validate("good")).thenReturn(true);
        when(jwtTokenProvider.parseToken("good")).thenReturn(
                UserPrincipal.builder()
                        .userId(7L)
                        .email("a@b.c")
                        .role("USER")
                        .build()
        );

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/reservations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            captured.set(ex);
            return Mono.empty();
        };

        // when
        GatewayFilter gatewayFilter = filter.apply(new JwtAuthenticationFilter.Config());
        gatewayFilter.filter(exchange, chain).block();

        // then
        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("7");

        log.info("[결과] X-User-Id 헤더 값: {}", captured.get().getRequest().getHeaders().getFirst("X-User-Id"));
    }

    @Test
    @DisplayName("토큰이 없으면 401을 반환한다")
    void 토큰이_없으면_401을_반환한다() {
        log.info("[시나리오] Authorization 헤더 없는 요청 → 401 Unauthorized 검증");

        // given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/reservations")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            captured.set(ex);
            return Mono.empty();
        };

        // when
        GatewayFilter gatewayFilter = filter.apply(new JwtAuthenticationFilter.Config());
        gatewayFilter.filter(exchange, chain).block();

        // then
        assertThat(captured.get()).isNull(); // 체인 미호출 확인
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        log.info("[결과] 응답 상태 코드: {}", exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("무효한 토큰이면 401을 반환한다")
    void 무효한_토큰이면_401을_반환한다() {
        log.info("[시나리오] 무효한 Bearer 토큰으로 요청 → 401 Unauthorized 검증");

        // given
        when(jwtTokenProvider.validate("bad"))
                .thenThrow(new BusinessException(ErrorCode.INVALID_TOKEN));

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/reservations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer bad")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            captured.set(ex);
            return Mono.empty();
        };

        // when
        GatewayFilter gatewayFilter = filter.apply(new JwtAuthenticationFilter.Config());
        gatewayFilter.filter(exchange, chain).block();

        // then
        assertThat(captured.get()).isNull(); // 체인 미호출 확인
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        log.info("[결과] 응답 상태 코드: {}", exchange.getResponse().getStatusCode());
    }
}
