# VERIFY_CHECKLIST — Phase 7: API Gateway

- **UUID**: dc189371
- **Type**: code (auth)
- **대상**: `infrastructure/api-gateway`

---

## 빌드 / 구조
- [ ] `./gradlew :infrastructure:api-gateway:build` 성공
- [ ] 패키지 루트 `com.ticketch.gateway` 하위에 filter/config 구성
- [ ] 리액티브 API만 사용 (서블릿 `javax/jakarta.servlet.Filter`, `HttpServletRequest` 미사용)

## 기능 — JWT 인증 필터
- [ ] Authorization 헤더가 없으면 401 반환
- [ ] `Bearer ` 접두사가 없으면 401 반환
- [ ] 유효하지 않은/만료된 토큰(JwtTokenProvider.validate가 BusinessException) → 401
- [ ] 유효한 토큰이면 `X-User-Id` 헤더가 주입되어 downstream으로 전달됨
- [ ] X-User-Id 값이 토큰의 userId(subject)와 일치

## 기능 — 라우팅 / Rate Limit / CORS
- [ ] user(`/api/auth`,`/api/users`)·event(`/api/events`) 경로는 JWT 필터 없이 라우팅
- [ ] reservation(`/api/reservations`,`/api/queue`)·payment(`/api/payments`) 경로에 JWT 필터 적용
- [ ] 모든 라우트가 `lb://{service}` 형태로 Eureka 디스커버리 사용
- [ ] RateLimitFilter가 `ratelimit:{ip}` 키로 카운트하고 한도 초과 시 429 반환 (TTL 1s)
- [ ] CORS 프리플라이트/요청이 허용됨 (CorsWebFilter)

## 테스트 / 보안
- [ ] JwtAuthenticationFilter 단위 테스트 통과 (유효 토큰 주입 / 무토큰 401 / 무효토큰 401)
- [ ] 기존 테스트(user/event/reservation/payment/notification) 전부 통과 (회귀 없음)
- [ ] 변경 코드에 OWASP Top 10 보안 취약점 없음
- [ ] **인증 구현 보안 검토**: 토큰 검증 우회 경로 없음, X-User-Id는 Gateway만 주입(클라이언트가 보낸 X-User-Id를 덮어쓰는지 확인 — 신뢰 경계), 서명 검증 후에만 헤더 주입 (Esecurity-officer 또는 수동 검토)

## 마무리
- [ ] `infrastructure/api-gateway/**` 외 파일 변경 없음 (경계 준수)
- [ ] 완료 후 `.qe/TASK_LOG.md`의 Phase 7 항목을 ✅로 갱신
