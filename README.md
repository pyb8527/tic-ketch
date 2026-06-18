# 🎫 TicKetch

> 실시간 티켓 예매 플랫폼 — MSA · 멀티모듈 · 헥사고날 아키텍처

## Tech Stack

**Backend** `Java 21` `Spring Boot 3.3` `Spring Cloud` `JPA` `QueryDSL` `Flyway`  
**Infra** `Redis` `RabbitMQ` `MySQL` `MongoDB` `Docker Compose`  
**CI/CD** `Jenkins` `SonarQube` `Zipkin`  
**Frontend** `React 18` `TypeScript` `Tailwind CSS`

## Key Features

- 분산 좌석 선점 — Redis Redisson 분산락
- 예약 타임아웃 자동 회수 — Redis TTL 5분
- 실시간 좌석 현황 — SSE (Server-Sent Events)
- 비동기 결제 — RabbitMQ Dead Letter Queue
- 오픈런 대기열 — Redis Sorted Set
- MSA 서비스 간 Circuit Breaker — Resilience4j
- 분산 추적 — Micrometer + Zipkin

## Architecture

> 상세 스펙 및 구현 계획 → `Doc/SPEC.md` · `Doc/plan.html`
