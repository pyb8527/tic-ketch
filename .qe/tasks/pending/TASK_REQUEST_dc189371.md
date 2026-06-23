<!-- chained-from: Qgenerate-spec -->
# TASK_REQUEST — Phase 7: API Gateway

- **UUID**: dc189371
- **Type**: code (auth — 보안 검토 대상)
- **Phase**: Phase 7 / 단계별 구현 계획 (doc/SPEC.md §15)
- **대상 모듈**: `infrastructure/api-gateway`
- **참조**: `services/user-service`(security 빈 등록 방식), `common/ticketch-security`(JwtTokenProvider)

---

## What (비즈니스 목표)
모든 클라이언트 요청의 **단일 관문**. 서비스별 **라우팅**, **JWT 검증 후 `X-User-Id` 헤더 주입**, **IP Rate Limit**, **CORS**를 처리한다. 이로써 reservation/payment 서비스가 헤더로 받던 `X-User-Id`를 Gateway가 토큰에서 추출해 신뢰성 있게 주입한다.

**완료 조건(핵심)**
1. Gateway(:8080) 통해 각 서비스로 경로 기반 라우팅
2. reservation/payment 경로는 유효한 JWT 없으면 401, 있으면 `X-User-Id` 주입 후 전달
3. 과도한 요청 IP는 Redis 기반 Rate Limit으로 429 반환

## How (기술 구현 로직)
- **리액티브 스택**: Spring Cloud Gateway(WebFlux/Netty) — 서블릿 아님. 필터는 `ServerWebExchange` 기반
- **JWT**: `com.ticketch.security.JwtTokenProvider`(`validate()`/`parseToken()`) 사용. 메인 클래스에 `scanBasePackages="com.ticketch"` + `@EnableConfigurationProperties(JwtProperties.class)` (user-service와 동일). jwt.secret은 Config Server 주입
- **라우팅**: `RouteLocator`(Java DSL), `lb://{service}` (Eureka + LoadBalancer)
- **Rate Limit**: `ReactiveStringRedisTemplate`로 `ratelimit:{ip}` 카운트(TTL 1s) — doc/SPEC.md §9 Redis 설계와 일치
- 패키지 루트 `com.ticketch.gateway`. 기존 build.gradle에 gateway·eureka·redis-reactive·config·ticketch-security 포함됨
- 테스트는 **단위 테스트**(JwtAuthenticationFilter, MockServerWebExchange + Mockito) — 실인프라 불필요

## 라우팅 규칙 (doc/SPEC.md §7)
| 서비스 | 경로 | JWT 필터 |
|--------|------|----------|
| user-service | `/api/auth/**`, `/api/users/**` | ✗ |
| event-service | `/api/events/**` | ✗ |
| reservation-service | `/api/reservations/**`, `/api/queue/**` | ✓ |
| payment-service | `/api/payments/**` | ✓ |

---

## 체크리스트 (Atomic Items)

### 부트스트랩 / CORS (Wave 1, 독립)
- [ ] **부트스트랩**: `ApiGatewayApplication`(@SpringBootApplication scanBasePackages="com.ticketch", @EnableConfigurationProperties(JwtProperties.class), @EnableDiscoveryClient) + `application.yml`(server.port 8080, name api-gateway, config import, eureka defaultZone http://localhost:8761/eureka/, gateway discovery locator enabled:false) → output: `infrastructure/api-gateway/src/main/java/com/ticketch/gateway/ApiGatewayApplication.java`, `.../resources/application.yml`
- [ ] **CorsConfig**: `@Configuration` + `@Bean CorsWebFilter`(리액티브 `org.springframework.web.cors.reactive.CorsWebFilter`, allowedOriginPattern "*", method/header "*", allowCredentials true, "/**") → output: `.../config/CorsConfig.java`

### 필터 (Wave 1, 독립 — 기존 빈만 의존)
- [ ] **JwtAuthenticationFilter**: `@Component extends AbstractGatewayFilterFactory<Config>`. apply(): Authorization Bearer 추출 → 없으면 401, `jwtTokenProvider.validate(token)`(BusinessException 시 401) → `parseToken`으로 userId/role 추출 → 요청에 `X-User-Id`/`X-User-Role` 헤더 주입 후 chain. `static class Config {}`. 401은 응답에 직접 setStatus+setComplete → output: `.../filter/JwtAuthenticationFilter.java` <!-- depends_on: [common: JwtTokenProvider(기존)] -->
- [ ] **RateLimitFilter**: `@Component implements GlobalFilter, Ordered`. `ReactiveStringRedisTemplate`로 `ratelimit:{ip}` increment, 첫 요청 시 TTL 1s 설정, 한도(예: 20/s) 초과 시 429 setComplete, 아니면 chain. getOrder() = -1 → output: `.../filter/RateLimitFilter.java` <!-- depends_on: [부트스트랩(redis-reactive 자동설정)] -->

### 라우팅 (Wave 2)
- [ ] **RouteConfig**: `@Configuration` + `@Bean RouteLocator routeLocator(RouteLocatorBuilder, JwtAuthenticationFilter)`. user/event 경로는 필터 없이, reservation/payment 경로는 `jwtFilter.apply(new JwtAuthenticationFilter.Config())` 적용. 전부 `lb://{service}` → output: `.../config/RouteConfig.java` <!-- depends_on: [JwtAuthenticationFilter] -->

### 테스트 (Wave 3 — 단위)
- [ ] **JwtAuthenticationFilter 단위 테스트**: `@Mock JwtTokenProvider`, `MockServerWebExchange`/`MockServerHttpRequest`. (1) 유효 토큰 → chain에 전달된 요청에 X-User-Id 주입 검증, (2) Authorization 없음 → 401, (3) validate가 BusinessException → 401 → output: `infrastructure/api-gateway/src/test/java/com/ticketch/gateway/filter/JwtAuthenticationFilterTest.java` <!-- depends_on: [JwtAuthenticationFilter] -->

---

## Notes
- `JwtTokenProvider.validate(token)`는 실패 시 `BusinessException`(EXPIRED_TOKEN/INVALID_TOKEN)을 **던짐**(boolean true만 정상) → 필터는 try/catch로 401 처리
- `parseToken`은 `UserPrincipal{userId, email, role}` 반환
- `lb://`는 Eureka+LoadBalancer 필요(eureka-client 의존성에 포함). 런타임엔 Eureka/대상 서비스 기동 필요
- jwt.secret은 user-service와 **동일 값**(Config Server)이어야 토큰 검증 가능 — Gateway 단독 기동 시 Config Server 필요. 단위 테스트는 JwtTokenProvider를 목킹하므로 무관
- GatewayExceptionHandler는 별도 파일 대신 각 필터가 401/429 응답을 직접 작성(리액티브 setComplete)하는 방식으로 대체 — 단순·견고
- Role ownership: `infrastructure/api-gateway/**`만 수정. common/타 서비스 변경 금지
