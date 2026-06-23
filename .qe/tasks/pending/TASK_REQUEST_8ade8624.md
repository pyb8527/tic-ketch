<!-- chained-from: Qgenerate-spec -->
# TASK_REQUEST — Phase 5: Payment Service

- **UUID**: 8ade8624
- **Type**: code (payment — 보안 검토 대상)
- **Phase**: Phase 5 / 단계별 구현 계획 (doc/SPEC.md §15)
- **대상 모듈**: `services/payment-service`
- **참조 컨벤션**: `services/reservation-service` 헥사고날 패키지 + Feign/RabbitMQ 패턴

---

## What (비즈니스 목표)
**목업 결제**를 처리하고 결과를 **RabbitMQ 이벤트로 발행**해, Reservation Service가 예약을 확정/회수하고 Notification Service가 알림을 보내도록 한다. 실제 PG 연동 없이 결제 플로우를 시뮬레이션한다.

**완료 조건(핵심)**
1. `POST /api/payments` 결제 요청 → 목업 처리(성공/실패) → `payment.completed`/`payment.failed` 이벤트 발행
2. 발행된 이벤트를 (기존) Reservation Service `PaymentEventConsumer`가 수신해 예약 상태 전이
3. 결제 실패 재시도는 DLQ(`payment.dlx` → `payment.dlq`)로 처리

## How (기술 구현 로직)
- **목업 PG**: `MockPgAdapter`(ProcessPaymentPort) — 90% 성공 / 10% 실패 (`java.util.Random`)
- **이벤트 발행**: `payment.exchange`(topic)로 `PaymentCompletedEvent`/`PaymentFailedEvent`(common:ticketch-events 기존 DTO) 발행
- **DLQ**: `payment.dlx`(DLX) + `payment.dlq` 선언 — consumer 재시도 실패분 수집
- **예약 검증**: Feign → Reservation Service `GET /api/reservations/{id}`(X-User-Id 헤더) → `seatId`/`eventId`/`status` 획득. Resilience4j Fallback로 회로 차단
  - **중요**: `PaymentCompletedEvent`/`PaymentFailedEvent`는 `seatId`/`eventId` 필드를 요구하므로, Payment 도메인엔 없는 이 값을 **반드시 예약 조회로 가져와** 이벤트에 채운다
- 패키지 루트: `com.ticketch.paymentservice`. 기본 구조는 reservation-service와 동일. **Redis 미사용**
- 기존 `build.gradle`에 web·jpa·amqp·feign·resilience4j·flyway 의존성 포함됨 (Redis/Testcontainers 없음)

## 데이터 모델 (doc/SPEC.md §8, payments)
`id, reservation_id(UNIQUE), user_id, amount(INT), status ENUM(PENDING/COMPLETED/FAILED/REFUNDED), failure_reason VARCHAR(500), paid_at DATETIME, created_at`

---

## 체크리스트 (Atomic Items)

### 부트스트랩 / 설정
- [ ] **부트스트랩**: `PaymentServiceApplication`(@SpringBootApplication scanBasePackages={paymentservice, common}, @EnableDiscoveryClient, @EnableFeignClients) + `application.yml`(name=payment-service, config import) + 테스트용 `application-test.yml`(H2 MODE=MySQL, flyway off, rabbitmq listener 미사용) → output: `services/payment-service/src/main/java/com/ticketch/paymentservice/PaymentServiceApplication.java`, `services/payment-service/src/main/resources/application.yml`, `services/payment-service/src/test/resources/application-test.yml`
- [ ] **Flyway 마이그레이션**: `payments` 테이블 (doc/SPEC.md §8 그대로, reservation의 V1 스타일) → output: `services/payment-service/src/main/resources/db/migration/V1__create_payments.sql`
- [ ] **RabbitConfig**: `payment.exchange`(TopicExchange) + `payment.dlx`(DLX) + `payment.dlq`(Queue) + 바인딩 + Jackson2JsonMessageConverter + RabbitTemplate(converter 적용). 상수: `PAYMENT_EXCHANGE`, `PAYMENT_COMPLETED_KEY="payment.completed"`, `PAYMENT_FAILED_KEY="payment.failed"`, `PAYMENT_DLX`, `PAYMENT_DLQ` → output: `.../config/RabbitConfig.java`

### 도메인 (순수 Java)
- [ ] **Domain model**: `Payment`{id,reservationId,userId,amount,status,failureReason,paidAt,createdAt} + `PaymentStatus` enum(PENDING/COMPLETED/FAILED/REFUNDED). 메서드: `static create(reservationId,userId,amount)`(PENDING), `complete()`(PENDING→COMPLETED, paidAt 설정, 아니면 PAYMENT_ALREADY_COMPLETED), `fail(reason)`(→FAILED), `refund()`(COMPLETED→REFUNDED, 아니면 예외). 순수 Java(common 예외만 허용) → output: `.../domain/model/Payment.java`, `.../domain/model/PaymentStatus.java`

### 포트 (인터페이스)
- [ ] **Input Ports**: `RequestPaymentUseCase`(+RequestPaymentCommand/PaymentResult record), `CancelPaymentUseCase`, `GetPaymentUseCase`(+PaymentDetail record) → output: `.../application/port/in/` <!-- depends_on: [Domain model] -->
- [ ] **Output Ports**: `LoadPaymentPort`, `SavePaymentPort`, `ProcessPaymentPort`(목업 PG), `ValidateReservationPort`(Feign), `PublishPaymentEventPort` → output: `.../application/port/out/` <!-- depends_on: [Domain model] -->

### 아웃바운드 어댑터
- [ ] **Persistence 어댑터**: `PaymentJpaEntity`(updated 없음 주의: created_at만, paid_at nullable) + `PaymentJpaRepository`(findByReservationId) + `PaymentPersistenceAdapter`(implements Load/SavePaymentPort) → output: `.../adapter/out/persistence/` <!-- depends_on: [Output Ports] -->
- [ ] **MockPgAdapter**: implements `ProcessPaymentPort` — 90% 성공/10% 실패 시뮬레이션, 결과 객체 반환 → output: `.../adapter/out/mock/MockPgAdapter.java` <!-- depends_on: [Output Ports] -->
- [ ] **ReservationClient(Feign)**: `ReservationFeignClient`(@FeignClient name="reservation-service", `GET /api/reservations/{id}` with `@RequestHeader("X-User-Id")`) + 로컬 응답 record(common ApiResponse 역직렬화 불가 → 로컬 래퍼) + `ReservationClient`(implements ValidateReservationPort, seatId/eventId/status 반환) + `ReservationClientFallback` → output: `.../adapter/out/external/` <!-- depends_on: [Output Ports] -->
- [ ] **PaymentEventPublisher**: implements `PublishPaymentEventPort` — `payment.exchange`로 completed/failed 발행 → output: `.../adapter/out/messaging/PaymentEventPublisher.java` <!-- depends_on: [Output Ports, RabbitConfig] -->

### 애플리케이션 서비스
- [ ] **PaymentService**: implements RequestPayment/Cancel/GetPayment UseCase. requestPayment 흐름: 예약검증(Feign→seatId/eventId/status, status=PENDING 확인) → Payment(PENDING) 저장 → MockPg 처리 → 성공 시 complete()+save+PaymentCompletedEvent 발행, 실패 시 fail()+save+PaymentFailedEvent 발행 → output: `.../application/service/PaymentService.java` <!-- depends_on: [Output Ports, MockPgAdapter, ReservationClient, PaymentEventPublisher] -->

### 인바운드 어댑터
- [ ] **PaymentController + DTO**: `POST /api/payments`(X-User-Id, body{reservationId,amount}), `GET /api/payments/{id}`, `POST /api/payments/{id}/cancel` → output: `.../adapter/in/web/PaymentController.java`, `.../adapter/in/web/dto/` <!-- depends_on: [PaymentService] -->

### 테스트 (단위 — Mockito)
- [ ] **PaymentService 단위 테스트**: ProcessPaymentPort/ValidateReservationPort/PublishPaymentEventPort/Save/LoadPort Mockito 목킹. (1) 목업 성공 → PaymentCompletedEvent 발행 검증, (2) 목업 실패 → PaymentFailedEvent 발행 검증, (3) 예약 status≠PENDING → 예외 → output: `services/payment-service/src/test/java/com/ticketch/paymentservice/application/service/PaymentServiceTest.java` <!-- depends_on: [PaymentService] -->

---

## Notes
- `PaymentCompletedEvent{paymentId,reservationId,userId,seatId,eventId,amount,paidAt}` / `PaymentFailedEvent{...,reason,failedAt}` — `common:ticketch-events` 기존 DTO 그대로 사용(신규 정의 금지)
- `seatId`/`eventId`는 Reservation Feign 응답(`ReservationDetail{id,seatId,eventId,status,expiresAt,remainingSeconds}`)에서 획득
- `amount`는 결제 요청 바디로 수신(목업 범위 — 실제 결제라면 서버측 가격 검증 필요, VERIFY에 명시)
- Reservation의 `PaymentEventConsumer`는 이미 `payment.completed.reservation.queue`/`payment.failed.reservation.queue`를 구독 중 → Payment는 `payment.exchange`에 발행만 하면 연동됨
- Role ownership: `services/payment-service/**`만 수정. common/타 서비스 변경 금지
