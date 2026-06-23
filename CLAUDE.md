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
- 🔜 Phase 4: Reservation Service ← **현재 작업** (분산락·TTL·대기열, 핵심 복잡도)
- ⬜ Phase 5~10: Payment / Notification / Gateway / Jenkins / Frontend / 통합

## 단일 진실 공급원
- 전체 스펙: `doc/SPEC.md` (서비스별 상세 설계·데이터 모델·Redis/MQ 설계)
- QE 규칙(파일 네이밍·태스크 상태·완료 기준): `QE_CONVENTIONS.md` 참조 (존재 시)

## Task Log
- 태스크 이력은 `.qe/TASK_LOG.md` 참조
