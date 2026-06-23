# Phase 7 CONTRACTS — API Gateway (단일 진실 공급원)

> 모든 Wave 팀원은 이 파일을 그대로 따른다. **리액티브(Spring Cloud Gateway / WebFlux)** — 서블릿 API 금지.
> 루트 패키지: `com.ticketch.gateway` (이하 `…`)
> 소스 경로: `infrastructure/api-gateway/src/main/java/com/ticketch/gateway/`
> 의존성(기존 build.gradle): spring-cloud-starter-gateway, eureka-client, spring-boot-starter-data-redis-reactive, config, `:common:ticketch-security`.

## 0. 기존 자산 (그대로 사용)
- `com.ticketch.security.JwtTokenProvider` (@Component): `void validate(String) throws BusinessException`(성공 시 true 반환), `UserPrincipal parseToken(String)`
- `com.ticketch.security.UserPrincipal` (@Getter): `Long getUserId()`, `String getEmail()`, `String getRole()`
- `com.ticketch.security.JwtProperties` (@ConfigurationProperties("jwt")) — 메인에서 `@EnableConfigurationProperties` 필요
- `com.ticketch.common.exception.BusinessException`

## 1. 부트스트랩 (Wave 1)

### `…/ApiGatewayApplication.java`
```java
package com.ticketch.gateway;
import com.ticketch.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "com.ticketch")
@EnableConfigurationProperties(JwtProperties.class)
@EnableDiscoveryClient
public class ApiGatewayApplication {
    public static void main(String[] args) { SpringApplication.run(ApiGatewayApplication.class, args); }
}
```
> `scanBasePackages="com.ticketch"`로 ticketch-security의 JwtTokenProvider @Component를 스캔(user-service와 동일).

### `…/src/main/resources/application.yml`
```yaml
server:
  port: 8080
spring:
  application:
    name: api-gateway
  config:
    import: optional:configserver:http://localhost:8888
  cloud:
    gateway:
      discovery:
        locator:
          enabled: false
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
```

## 2. CorsConfig (Wave 1) — `…/config/CorsConfig.java`
`@Configuration`. `@Bean public CorsWebFilter corsWebFilter()`:
- `org.springframework.web.cors.reactive.CorsWebFilter`, `org.springframework.web.cors.CorsConfiguration`, `org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource`
```java
CorsConfiguration cfg = new CorsConfiguration();
cfg.addAllowedOriginPattern("*");
cfg.addAllowedMethod("*");
cfg.addAllowedHeader("*");
cfg.setAllowCredentials(true);
UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
source.registerCorsConfiguration("/**", cfg);
return new CorsWebFilter(source);
```

## 3. JwtAuthenticationFilter (Wave 1) — `…/filter/JwtAuthenticationFilter.java`
`@Component`, `extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config>` (org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory).
```java
private final JwtTokenProvider jwtTokenProvider;
public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
    super(Config.class);
    this.jwtTokenProvider = jwtTokenProvider;
}
public static class Config {}

@Override
public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
        String authHeader = exchange.getRequest().getHeaders().getFirst(org.springframework.http.HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }
        String token = authHeader.substring(7);
        try {
            jwtTokenProvider.validate(token);
        } catch (BusinessException e) {
            return unauthorized(exchange);
        }
        UserPrincipal principal = jwtTokenProvider.parseToken(token);
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .headers(h -> {
                    h.set("X-User-Id", String.valueOf(principal.getUserId()));
                    h.set("X-User-Role", principal.getRole());
                })
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    };
}

private Mono<Void> unauthorized(ServerWebExchange exchange) {
    exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
    return exchange.getResponse().setComplete();
}
```
imports: org.springframework.cloud.gateway.filter.GatewayFilter, org.springframework.http.server.reactive.ServerHttpRequest, org.springframework.web.server.ServerWebExchange, reactor.core.publisher.Mono, JwtTokenProvider, UserPrincipal, BusinessException.
> **보안**: `h.set(...)`은 클라이언트가 보낸 X-User-Id를 덮어쓴다(스푸핑 방지). 서명 검증 통과 후에만 주입.

## 4. RateLimitFilter (Wave 1) — `…/filter/RateLimitFilter.java`
`@Component @Slf4j implements GlobalFilter, Ordered` (org.springframework.cloud.gateway.filter.GlobalFilter, org.springframework.core.Ordered).
```java
private final ReactiveStringRedisTemplate redisTemplate;  // 생성자 주입(@RequiredArgsConstructor 또는 명시 생성자)
private static final int LIMIT = 20;                        // 초당 IP 허용 요청
private static final Duration WINDOW = Duration.ofSeconds(1);

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
                exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);
                return exchange.getResponse().setComplete();
            }
            return chain.filter(exchange);
        });
}
private String resolveIp(ServerWebExchange exchange) {
    return exchange.getRequest().getRemoteAddress() != null
        ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        : "unknown";
}
@Override public int getOrder() { return -1; }
```
imports: org.springframework.data.redis.core.ReactiveStringRedisTemplate, org.springframework.cloud.gateway.filter.GatewayFilterChain, ServerWebExchange, Mono, java.time.Duration, Ordered.
> ReactiveStringRedisTemplate은 redis-reactive 스타터가 자동 구성. 생성자 주입.

## 5. RouteConfig (Wave 2) — `…/config/RouteConfig.java`
`@Configuration`. JwtAuthenticationFilter를 주입받아 reservation/payment에만 적용.
```java
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
```
imports: org.springframework.cloud.gateway.route.RouteLocator, org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder.

## 6. 단위 테스트 (Wave 2) — `…/src/test/java/com/ticketch/gateway/filter/JwtAuthenticationFilterTest.java`
`@ExtendWith(MockitoExtension.class)`. `@Mock JwtTokenProvider jwtTokenProvider;`. SUT: `new JwtAuthenticationFilter(jwtTokenProvider)`. GatewayFilter = `filter.apply(new JwtAuthenticationFilter.Config())`.
- MockServerHttpRequest/MockServerWebExchange: `org.springframework.mock.http.server.reactive.MockServerHttpRequest`, `org.springframework.mock.web.server.MockServerWebExchange` (spring-test, 포함됨).
- chain 캡처: `AtomicReference<ServerWebExchange> captured = new AtomicReference<>(); GatewayFilterChain chain = ex -> { captured.set(ex); return Mono.empty(); };`
- test1 유효토큰_헤더주입:
  `when(jwtTokenProvider.validate("good")).thenReturn(true); when(jwtTokenProvider.parseToken("good")).thenReturn(UserPrincipal.builder().userId(7L).email("a@b.c").role("USER").build());`
  request = `MockServerHttpRequest.get("/api/reservations").header(HttpHeaders.AUTHORIZATION, "Bearer good").build()`; exchange = `MockServerWebExchange.from(request)`.
  `filter.apply(new Config()).filter(exchange, chain).block();`
  assert `captured.get().getRequest().getHeaders().getFirst("X-User-Id")` equals "7".
- test2 토큰없음_401:
  request = get("/api/reservations") (no auth header); chain = ex -> Mono.empty();
  `gatewayFilter.filter(exchange, chain).block();`
  assert `exchange.getResponse().getStatusCode()` == HttpStatus.UNAUTHORIZED. (chain 미호출)
- test3 무효토큰_401:
  `when(jwtTokenProvider.validate("bad")).thenThrow(new BusinessException(ErrorCode.INVALID_TOKEN));`
  request header "Bearer bad"; block; assert 401.
- AssertJ + Mockito. `@DisplayName`(한국어). 시나리오 로그(SLF4J) 선택.
> reactor `.block()`으로 동기 검증. Redis/Eureka 불필요(JwtTokenProvider 목킹, RateLimitFilter 미사용).
