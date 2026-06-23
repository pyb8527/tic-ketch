<!-- chained-from: Qgenerate-spec -->
# TASK_REQUEST — Phase 4: Reservation Service

- **UUID**: ee2a40d0
- **Type**: code
- **Phase**: Phase 4 / 단계별 구현 계획 (doc/SPEC.md §15)
- **대상 모듈**: `services/reservation-service`
- **참조 컨벤션**: `services/event-service` 헥사고날 패키지 구조

---

## What (비즈니스 목표)
좌석 **임시 선점(분산락)** → **5분 TTL 자동 회수** → **대기열 트래픽 제어** → **결제 이벤트 수신 후 예약 확정/취소**까지, 동시성이 핵심인 예약 도메인을 구현한다.

**완료 조건(핵심)**
1. 동일 좌석 동시 선점 요청 시 정확히 1건만 성공
2. 5분 경과 시 Redis TTL 만료 + Scheduler 보조 회수로 좌석 반환
3. `payment.completed`/`payment.failed` 수신 시 예약 상태 자동 전이

## How (기술 구현 로직)
- **분산락**: Redisson `RLock` — `seat:lock:{seatId}`, waitTime 3s / leaseTime 자동해제
- **TTL**: Redis Hash `reservation:temp:{reservationId}`, TTL 300s
- **대기열**: Redis Sorted Set `queue:{eventId}` (score = timestamp)
- **동기 통신**: Feign → Event Service 좌석 검증, Resilience4j Circuit Breaker + Fallback
- **비동기 통신**: RabbitMQ — `payment.*` 수신(Consumer), `seat.released` 발행(Publisher)
- 기존 `build.gradle`에 Redisson·AMQP·Feign·Resilience4j·Testcontainers 의존성 이미 포함됨

`com.ticketch.reservationservice` 패키지 하위. 기본 패키지 구조는 event-service와 동일.

---

## 체크리스트 (Atomic Items)

### 부트스트랩 / 인프라 설정
- [ ] **부트스트랩**: `ReservationServiceApplication`(@SpringBootApplication, @EnableDiscoveryClient, @EnableFeignClients, @EnableScheduling) + `application.yml`(포트 8083, Eureka/Config/Redis/RabbitMQ/Datasource) 작성 → output: `services/reservation-service/src/main/java/com/ticketch/reservationservice/ReservationServiceApplication.java`, `services/reservation-service/src/main/resources/application.yml`
- [ ] **Flyway 마이그레이션**: `reservations` 테이블 DDL (doc/SPEC.md §8 스키마 그대로) → output: `services/reservation-service/src/main/resources/db/migration/V1__create_reservations.sql`
- [ ] **RedissonConfig**: `RedissonClient` Bean (single server, Redis 주소 주입) → output: `.../config/RedissonConfig.java`
- [ ] **RabbitConfig**: `seat.exchange`(topic) + `payment.completed.queue`/`payment.failed.queue` 선언 및 바인딩, Jackson2JsonMessageConverter → output: `.../config/RabbitConfig.java`

### 도메인 (순수 Java)
- [ ] **Domain model**: `Reservation`{id,userId,seatId,eventId,status,expiresAt} + `ReservationStatus` enum(PENDING/CONFIRMED/CANCELLED/EXPIRED), `QueueEntry`{userId,eventId,score} → output: `.../domain/model/Reservation.java`, `.../domain/model/QueueEntry.java`
- [ ] **Domain service**: `ReservationDomainService` — 만료 여부 판단(`isExpired`), 상태 전이 규칙(confirm/cancel 가능 여부) → output: `.../domain/service/ReservationDomainService.java`

### 포트 (인터페이스)
- [ ] **Input Ports**: `HoldSeatUseCase`, `ConfirmReservationUseCase`, `CancelReservationUseCase`, `GetReservationUseCase`, `QueueUseCase` → output: `.../application/port/in/` <!-- depends_on: [domain model] -->
- [ ] **Output Ports**: `LoadReservationPort`, `SaveReservationPort`, `AcquireSeatLockPort`, `HoldSeatCachePort`, `ReleaseSeatCachePort`, `ManageQueuePort`, `ValidateSeatPort`, `PublishSeatReleasedPort` → output: `.../application/port/out/` <!-- depends_on: [domain model] -->

### 아웃바운드 어댑터 (Output Port 구현)
- [ ] **Persistence 어댑터**: `ReservationJpaEntity` + `ReservationJpaRepository` + `ReservationPersistenceAdapter`(implements Load/SaveReservationPort, Entity↔Domain 매핑) → output: `.../adapter/out/persistence/` <!-- depends_on: [Output Ports] -->
- [ ] **RedissonLockAdapter**: implements `AcquireSeatLockPort` — `seat:lock:{seatId}` tryLock(3s) → output: `.../adapter/out/redis/RedissonLockAdapter.java` <!-- depends_on: [Output Ports, RedissonConfig] -->
- [ ] **ReservationCacheAdapter**: implements `HoldSeatCachePort`/`ReleaseSeatCachePort` — Redis Hash TTL 300s → output: `.../adapter/out/redis/ReservationCacheAdapter.java` <!-- depends_on: [Output Ports] -->
- [ ] **QueueRedisAdapter**: implements `ManageQueuePort` — Sorted Set 진입/순번조회/팝 → output: `.../adapter/out/redis/QueueRedisAdapter.java` <!-- depends_on: [Output Ports] -->
- [ ] **EventServiceClient**: Feign(implements `ValidateSeatPort`) + Resilience4j CircuitBreaker Fallback → output: `.../adapter/out/external/EventServiceClient.java`, `.../adapter/out/external/EventClientFallback.java` <!-- depends_on: [Output Ports] -->
- [ ] **SeatReleasedPublisher**: implements `PublishSeatReleasedPort` — `seat.exchange` 로 `SeatReleasedEvent`(ticketch-events) 발행 → output: `.../adapter/out/messaging/SeatReleasedPublisher.java` <!-- depends_on: [Output Ports, RabbitConfig] -->

### 애플리케이션 서비스 (UseCase 구현)
- [ ] **HoldSeatService**: 좌석검증(Feign) → 분산락 획득 → DB 중복확인 → Reservation(PENDING) 저장 → TTL 캐시 저장 → 락 해제 (doc/SPEC.md HoldSeat 플로우) → output: `.../application/service/HoldSeatService.java` <!-- depends_on: [Output Ports, RedissonLockAdapter, ReservationCacheAdapter, EventServiceClient] -->
- [ ] **ReservationConfirmService**: `ConfirmReservationUseCase`+`CancelReservationUseCase` 구현 — 결제 완료 시 CONFIRMED, 실패/만료 시 CANCELLED + 좌석 회수 이벤트 발행 → output: `.../application/service/ReservationConfirmService.java` <!-- depends_on: [Output Ports, SeatReleasedPublisher] -->
- [ ] **QueueService**: `QueueUseCase` 구현 — 대기열 진입/순번 조회 → output: `.../application/service/QueueService.java` <!-- depends_on: [QueueRedisAdapter] -->
- [ ] **GetReservationService**: `GetReservationUseCase` 구현 — 예약 상세 + 남은 시간 계산 → output: `.../application/service/GetReservationService.java` <!-- depends_on: [Output Ports] -->

### 인바운드 어댑터
- [ ] **ReservationController + DTO**: `POST /api/reservations`, `GET /api/reservations/{id}`, `GET /api/reservations/me`, `DELETE /api/reservations/{id}` (X-User-Id 헤더 기반) → output: `.../adapter/in/web/ReservationController.java`, `.../adapter/in/web/dto/` <!-- depends_on: [HoldSeatService, ReservationConfirmService, GetReservationService] -->
- [ ] **QueueController**: `POST /api/queue/{eventId}/enter`, `GET /api/queue/{eventId}` → output: `.../adapter/in/web/QueueController.java` <!-- depends_on: [QueueService] -->
- [ ] **PaymentEventConsumer**: `@RabbitListener` payment.completed/failed → Confirm/Cancel UseCase 호출 → output: `.../adapter/in/messaging/PaymentEventConsumer.java` <!-- depends_on: [ReservationConfirmService, RabbitConfig] -->
- [ ] **ExpiredReservationScheduler**: `@Scheduled` 만료 PENDING 예약 주기 회수(보조) → output: `.../adapter/in/scheduler/ExpiredReservationScheduler.java` <!-- depends_on: [ReservationConfirmService] -->

### 테스트
- [ ] **통합 테스트**: Testcontainers(MySQL+Redis) — 동시 선점 멀티스레드 시나리오에서 1건만 성공 검증 → output: `services/reservation-service/src/test/java/com/ticketch/reservationservice/HoldSeatServiceIntegrationTest.java` <!-- depends_on: [HoldSeatService] -->

---

## Notes
- port/out 인터페이스 ↔ adapter/out 구현체 1:1 매핑 유지, 도메인은 프레임워크 무의존
- `X-User-Id` 헤더는 API Gateway(Phase 7)가 주입하나, 현 단계는 헤더 직접 수신으로 구현(Gateway 미구현)
- `SeatReleasedEvent`/`PaymentCompletedEvent`/`PaymentFailedEvent`는 `common:ticketch-events`의 기존 DTO 사용 (신규 생성 금지)
- Role ownership: 본 태스크는 `services/reservation-service/**`만 수정. common 모듈/타 서비스 변경 금지
