# TicKetch — 프로젝트 컨텍스트

> 실시간 티켓 예매 플랫폼 · MSA · 멀티모듈 · 헥사고날 아키텍처 백엔드 포트폴리오

## 목표
- 분산환경 동시성 제어(분산락·TTL·대기열)를 실제로 구현해 보이는 포트폴리오
- MSA 표준 구성: Eureka 디스커버리, Config Server, API Gateway, RabbitMQ 비동기 통신
- 헥사고날 아키텍처로 도메인-인프라 분리 일관성 유지

## 기술 스택
- Java 21 · Spring Boot 3.3 · Gradle 멀티모듈
- Spring Data JPA + QueryDSL · Flyway · MySQL 8
- Redis 7 + Redisson(분산락/TTL/대기열) · RabbitMQ 3.13(DLQ)
- Spring Cloud: Eureka · Config · Gateway · OpenFeign · Resilience4j
- JWT(JJWT 0.12) · Testcontainers · SpringDoc OpenAPI 3

## 핵심 아키텍처 규칙 (반드시 준수)
- `domain` 패키지는 Spring/JPA/Redis 의존성 없음 (순수 Java)
- `application/port/out`은 인터페이스만 — 구현체는 `adapter/out`에 위치
- `adapter/in/web` Controller는 UseCase(Input Port) 인터페이스만 호출
- JPA Entity는 Domain Model과 분리해 매핑
- 패키지 컨벤션은 기존 `user-service`/`event-service` 구조를 따른다

## 현재 진행 상태
- ✅ Phase 0: 인프라 (eureka-server, config-server) / Phase 1: common 모듈
- ✅ Phase 2: User Service (JWT 인증) / Phase 3: Event Service (좌석 SSE)
- ✅ Phase 4: Reservation Service (분산락·TTL·대기열, 동시 선점 통합테스트 통과)
- ✅ Phase 5: Payment Service (목업 결제, RabbitMQ 이벤트 발행, 단위 테스트)
- ✅ Phase 6: Notification Service (MQ 이벤트 수신 → 알림 발송 → MongoDB 이력)
- ✅ Phase 7: API Gateway (리액티브, JWT 검증→X-User-Id 주입, Rate Limit, CORS)
- ⏸️ Phase 8: Jenkins CI/CD — **보류** (빌드 서버/배포 환경 미구축, 추후 진행)
- 🔜 Phase 9: Frontend ← **현재 작업** (React/Vite/TS, 예매 플로우 UI, SSE 실시간 좌석)
- ⬜ Phase 10: 통합 테스트 & 정리

## 백엔드 API 요약 (Frontend 연동 기준, 모두 Gateway :8080 경유)
- 응답 래퍼: `{ code, message, data }` (성공 code="SUCCESS")
- Auth: `POST /api/auth/register{email,password,name}` → userId / `POST /api/auth/login{email,password}` → `{accessToken,refreshToken}` / `GET /api/users/me`
- Event(공개): `GET /api/events`(Page) / `GET /api/events/{id}` → `{id,title,venue,eventDate,status}` / `GET /api/events/{id}/seats` → `{id,seatGradeId,rowName,seatNumber,status}[]` / `GET /api/events/{id}/seats/stream`(SSE: `connected`,`seat-status`)
- Reservation(JWT): `POST /api/reservations{seatId,eventId}` → `{reservationId,expiresAt}` / `GET /api/reservations/{id}` → `{id,seatId,eventId,status,expiresAt,remainingSeconds}` / `GET /api/reservations/me` / `DELETE /api/reservations/{id}` / `POST /api/queue/{eventId}/enter` → `{position,totalWaiting}`
- Payment(JWT): `POST /api/payments{reservationId,amount}` → `{paymentId,status}` / `GET /api/payments/{id}` / `POST /api/payments/{id}/cancel`

## 단일 진실 공급원
- 전체 스펙: `doc/SPEC.md` (서비스별 상세 설계·데이터 모델·Redis/MQ 설계)
- QE 규칙(파일 네이밍·태스크 상태·완료 기준): `QE_CONVENTIONS.md` 참조 (존재 시)

## Task Log
- 태스크 이력은 `.qe/TASK_LOG.md` 참조
