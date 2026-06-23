package com.ticketch.gateway.config;

import com.ticketch.gateway.filter.JwtAuthenticationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API Gateway 라우트 구성.
 *
 * <p>마이크로서비스 라우팅 규칙을 정의하는 중앙 구성 클래스.
 * 각 서비스 경로에 대한 라우트를 설정하고, 보호된 엔드포인트에 JWT 인증 필터를 적용한다.
 *
 * <p>라우트 설정:
 * <ul>
 *   <li><b>user-service</b>: {@code /api/auth/**}, {@code /api/users/**}
 *       → lb://user-service (필터 미적용, 공개)</li>
 *   <li><b>event-service</b>: {@code /api/events/**}
 *       → lb://event-service (필터 미적용, 공개)</li>
 *   <li><b>reservation-service</b>: {@code /api/reservations/**}, {@code /api/queue/**}
 *       → JwtAuthenticationFilter 적용 → lb://reservation-service (보호됨)</li>
 *   <li><b>payment-service</b>: {@code /api/payments/**}
 *       → JwtAuthenticationFilter 적용 → lb://payment-service (보호됨)</li>
 * </ul>
 *
 * <p>보호된 라우트(reservation/payment)는 {@code Authorization: Bearer <token>}
 * 헤더를 필수로 요구하며, 검증 실패 시 HTTP 401을 반환한다.
 */
@Configuration
public class RouteConfig {

    /**
     * RouteLocator를 생성하여 모든 마이크로서비스 라우트를 등록한다.
     *
     * <p>공개 라우트(user-service, event-service)는 필터를 적용하지 않으며,
     * 보호된 라우트(reservation-service, payment-service)는 JWT 인증 필터를 거친다.
     *
     * @param builder Spring Cloud Gateway의 RouteLocatorBuilder 인스턴스
     * @param jwtFilter JWT 인증 필터 (JwtAuthenticationFilter 컴포넌트)
     * @return 등록된 라우트를 담은 RouteLocator
     */
    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder, JwtAuthenticationFilter jwtFilter) {
        return builder.routes()
                .route("user-service", r -> r.path("/api/auth/**", "/api/users/**")
                        .uri("lb://user-service"))
                .route("event-service", r -> r.path("/api/events/**")
                        .uri("lb://event-service"))
                .route("reservation-service", r -> r.path("/api/reservations/**", "/api/queue/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://reservation-service"))
                .route("payment-service", r -> r.path("/api/payments/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://payment-service"))
                .build();
    }
}
