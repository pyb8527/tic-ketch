# Phase 5 CONTRACTS — Payment Service (단일 진실 공급원)

> 모든 Wave 팀원은 이 파일을 그대로 따른다. 시그니처/패키지/이름 임의 변경 금지.
> 루트 패키지: `com.ticketch.paymentservice` (이하 `…`)
> 소스 경로: `services/payment-service/src/main/java/com/ticketch/paymentservice/`
> 참조: `services/reservation-service` (Feign/RabbitMQ/Persistence 패턴 동일). **Redis 미사용**.

## 0. 공통 규칙
- 도메인 모델: 순수 Java (JDK + Lombok + `com.ticketch.common.*` + `com.ticketch.events.*`만). Spring/JPA import 금지
- Output Port = 인터페이스(`application/port/out`), 구현체 `@Component`(`adapter/out`)
- UseCase = 인터페이스(`application/port/in`), 구현체 `@Service`(`application/service`)
- Command/Result/Detail은 UseCase 인터페이스 안에 `record` 중첩
- 예외: `throw new BusinessException(ErrorCode.XXX)`. 사용 가능 코드:
  `PAYMENT_NOT_FOUND, PAYMENT_ALREADY_COMPLETED, PAYMENT_FAILED, PAYMENT_ALREADY_REFUNDED, RESERVATION_NOT_FOUND, ACCESS_DENIED, INVALID_INPUT`
- 컨트롤러 응답: `ResponseEntity<ApiResponse<T>>`, `ApiResponse.ok(data)`
- 공유 이벤트 DTO (com.ticketch.events.payment, **그대로 사용, 신규 정의 금지**):
  - `PaymentCompletedEvent{paymentId, reservationId, userId, seatId, eventId, amount(Integer), paidAt(LocalDateTime)}` (Builder)
  - `PaymentFailedEvent{paymentId, reservationId, userId, seatId, eventId, reason(String), failedAt(LocalDateTime)}` (Builder)

## 1. 도메인 (Wave 1)

### `domain/model/PaymentStatus.java`
```java
public enum PaymentStatus { PENDING, COMPLETED, FAILED, REFUNDED }
```

### `domain/model/Payment.java`
순수 Java. `@Getter @Builder @AllArgsConstructor`. 필드:
`Long id; Long reservationId; Long userId; Integer amount; PaymentStatus status; String failureReason; LocalDateTime paidAt; LocalDateTime createdAt;`
메서드:
- `static Payment create(Long reservationId, Long userId, Integer amount)` → status=PENDING, createdAt=now, paidAt=null, failureReason=null
- `void complete()` → status가 COMPLETED면 `PAYMENT_ALREADY_COMPLETED`; PENDING이 아니면 `PAYMENT_FAILED`; 통과 시 status=COMPLETED, paidAt=now()
- `void fail(String reason)` → status=FAILED, failureReason=reason (PENDING에서만 의미, 멱등 허용)
- `void refund()` → status가 COMPLETED가 아니면: REFUNDED면 `PAYMENT_ALREADY_REFUNDED`, 그 외 `PAYMENT_FAILED`; 통과 시 status=REFUNDED
- `void assertOwnedBy(Long userId)` → `!this.userId.equals(userId)`이면 `ACCESS_DENIED`

## 2. Input Ports (Wave 2) — `application/port/in/`

### `RequestPaymentUseCase`
```java
PaymentResult requestPayment(RequestPaymentCommand command);
record RequestPaymentCommand(Long userId, Long reservationId, Integer amount) {}
record PaymentResult(Long paymentId, String status) {}
```
### `CancelPaymentUseCase`
```java
void cancel(Long paymentId, Long userId);
```
### `GetPaymentUseCase`
```java
PaymentDetail getPayment(Long paymentId, Long userId);
record PaymentDetail(Long id, Long reservationId, Integer amount, String status, java.time.LocalDateTime paidAt) {}
```

## 3. Output Ports (Wave 2) — `application/port/out/`
```java
// LoadPaymentPort
Optional<Payment> findById(Long id);
Optional<Payment> findByReservationId(Long reservationId);

// SavePaymentPort
Payment save(Payment payment);   // id 채워서 반환

// ProcessPaymentPort  (목업 PG)
ProcessResult process(Long reservationId, Integer amount);
record ProcessResult(boolean success, String failureReason) {}   // 인터페이스 안에 중첩

// ValidateReservationPort  (Feign → reservation-service)
ReservationInfo getReservation(Long reservationId, Long userId);
record ReservationInfo(Long seatId, Long eventId, String status) {}   // 인터페이스 안에 중첩

// PublishPaymentEventPort  (RabbitMQ payment.exchange)
void publishCompleted(com.ticketch.events.payment.PaymentCompletedEvent event);
void publishFailed(com.ticketch.events.payment.PaymentFailedEvent event);
```
import: `…domain.model.Payment`, `java.util.Optional`.

## 4. 설정/부트스트랩 (Wave 1)

### `PaymentServiceApplication.java` + resources
```java
@SpringBootApplication(scanBasePackages = {"com.ticketch.paymentservice", "com.ticketch.common"})
@EnableDiscoveryClient @EnableFeignClients
```
- `application.yml`: `spring.application.name: payment-service` + `spring.config.import: optional:configserver:http://localhost:8888`
- `src/test/resources/application-test.yml`: H2(MODE=MySQL, ddl-auto create-drop, H2Dialect), flyway.enabled:false, `spring.rabbitmq.listener.simple.auto-startup:false`, `eureka.client.enabled:false` (Redis 설정 없음)

### `db/migration/V1__create_payments.sql`
doc/SPEC.md §8 payments 스키마 그대로:
`id BIGINT AUTO_INCREMENT PK, reservation_id BIGINT NOT NULL UNIQUE, user_id BIGINT NOT NULL, amount INT NOT NULL, status ENUM('PENDING','COMPLETED','FAILED','REFUNDED') DEFAULT 'PENDING', failure_reason VARCHAR(500), paid_at DATETIME, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP`. INDEX(user_id).

### `config/RabbitConfig.java`
`@Configuration`. 상수:
`PAYMENT_EXCHANGE="payment.exchange"`, `PAYMENT_COMPLETED_KEY="payment.completed"`, `PAYMENT_FAILED_KEY="payment.failed"`, `PAYMENT_DLX="payment.dlx"`, `PAYMENT_DLQ="payment.dlq"`.
Bean: `TopicExchange paymentExchange()`, `FanoutExchange paymentDlx()`, `Queue paymentDlq()`(durable), `Binding dlqBinding()`(dlq→dlx), `Jackson2JsonMessageConverter messageConverter()`, `RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter mc)`(converter 적용).
> Payment는 publisher. payment.exchange 선언은 reservation과 멱등 중복 OK. consumer 큐는 reservation/notification이 소유.

## 5. Out 어댑터 (Wave 3)

### `out/persistence/` (3파일)
- `PaymentJpaEntity` `@Entity @Table(name="payments")` `@Getter @Builder @NoArgsConstructor @AllArgsConstructor`. 필드 id(@Id @GeneratedValue IDENTITY), reservationId, userId, amount(Integer), status(@Enumerated STRING len 20), failureReason(@Column(length=500)), paidAt, createdAt. `toDomain()`, `static fromDomain(Payment)` — **createdAt은 도메인 값(NOT NULL) 그대로 set**(NULL 삽입 금지). paidAt/failureReason은 nullable.
- `PaymentJpaRepository extends JpaRepository<PaymentJpaEntity,Long>`: `Optional<PaymentJpaEntity> findByReservationId(Long reservationId)`
- `PaymentPersistenceAdapter implements LoadPaymentPort, SavePaymentPort` `@Component @RequiredArgsConstructor`

### `out/mock/MockPgAdapter.java` implements `ProcessPaymentPort`
`@Component`. private final `java.util.Random random = new Random()`. process: `random.nextInt(100) < 90` → `new ProcessResult(true, null)` ; else `new ProcessResult(false, "목업 PG 결제 거절 (10% 실패 시뮬레이션)")`. (런타임 Random 사용 정상)

### `out/external/` (Feign, reservation-service의 EventFeignClient 패턴 그대로)
- `ReservationDetailDto` record(Long id, Long seatId, Long eventId, String status, java.time.LocalDateTime expiresAt, long remainingSeconds)
- `ReservationApiResponse` record(String code, String message, ReservationDetailDto data)  ← common ApiResponse 역직렬화 불가하므로 로컬 정의
- `ReservationFeignClient` `@FeignClient(name="reservation-service", fallback=ReservationClientFallback.class)`: `@GetMapping("/api/reservations/{id}") ReservationApiResponse getReservation(@PathVariable("id") Long id, @RequestHeader("X-User-Id") Long userId);`
- `ReservationClientFallback implements ReservationFeignClient` `@Component`: `new ReservationApiResponse("C004","reservation-service unavailable", null)`
- `ReservationClient implements ValidateReservationPort` `@Component @RequiredArgsConstructor`: getReservation(id,userId) → resp=feign.getReservation(id,userId); `if(resp==null||resp.data()==null) throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);` → `return new ReservationInfo(d.seatId(), d.eventId(), d.status());`

### `out/messaging/PaymentEventPublisher.java` implements `PublishPaymentEventPort`
`@Component @RequiredArgsConstructor`; RabbitTemplate. publishCompleted → `convertAndSend(RabbitConfig.PAYMENT_EXCHANGE, RabbitConfig.PAYMENT_COMPLETED_KEY, event)`; publishFailed → `…PAYMENT_FAILED_KEY…`.

## 6. Application 서비스 (Wave 3)

### `application/service/PaymentService.java` implements RequestPaymentUseCase, CancelPaymentUseCase, GetPaymentUseCase
`@Service @RequiredArgsConstructor`. 의존: ValidateReservationPort, LoadPaymentPort, SavePaymentPort, ProcessPaymentPort, PublishPaymentEventPort.
- `@Transactional public PaymentResult requestPayment(RequestPaymentCommand cmd)`:
  1. `ReservationInfo info = validateReservationPort.getReservation(cmd.reservationId(), cmd.userId());`
  2. `if(!"PENDING".equals(info.status())) throw new BusinessException(ErrorCode.INVALID_INPUT, "결제 가능한 예약 상태가 아닙니다");`
  3. `Payment payment = savePaymentPort.save(Payment.create(cmd.reservationId(), cmd.userId(), cmd.amount()));`
  4. `ProcessResult result = processPaymentPort.process(cmd.reservationId(), cmd.amount());`
  5. 성공: `payment.complete(); savePaymentPort.save(payment);`
     `publishPaymentEventPort.publishCompleted(PaymentCompletedEvent.builder().paymentId(payment.getId()).reservationId(payment.getReservationId()).userId(payment.getUserId()).seatId(info.seatId()).eventId(info.eventId()).amount(payment.getAmount()).paidAt(payment.getPaidAt()).build());`
  6. 실패: `payment.fail(result.failureReason()); savePaymentPort.save(payment);`
     `publishPaymentEventPort.publishFailed(PaymentFailedEvent.builder().paymentId(payment.getId()).reservationId(payment.getReservationId()).userId(payment.getUserId()).seatId(info.seatId()).eventId(info.eventId()).reason(result.failureReason()).failedAt(java.time.LocalDateTime.now()).build());`
  7. `return new PaymentResult(payment.getId(), payment.getStatus().name());`
- `@Transactional public void cancel(Long paymentId, Long userId)`: findById or PAYMENT_NOT_FOUND; assertOwnedBy(userId); refund(); save.
- `public PaymentDetail getPayment(Long paymentId, Long userId)`: findById or PAYMENT_NOT_FOUND; assertOwnedBy(userId); `return new PaymentDetail(p.getId(), p.getReservationId(), p.getAmount(), p.getStatus().name(), p.getPaidAt());`
- Command/Result/Detail/ReservationInfo/ProcessResult는 각 인터페이스의 중첩 record로 참조.

## 7. In 어댑터 (Wave 3)

### `adapter/in/web/dto/RequestPaymentRequest.java`
`public record RequestPaymentRequest(@NotNull Long reservationId, @NotNull @Positive Integer amount) {}` (jakarta.validation)

### `adapter/in/web/PaymentController.java`
`@RestController @RequestMapping("/api/payments") @RequiredArgsConstructor`. 의존: RequestPaymentUseCase, GetPaymentUseCase, CancelPaymentUseCase.
- `@PostMapping` create(`@RequestHeader("X-User-Id") Long userId`, `@RequestBody @Valid RequestPaymentRequest req`): `requestPaymentUseCase.requestPayment(new RequestPaymentUseCase.RequestPaymentCommand(userId, req.reservationId(), req.amount()))` → `ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result))`
- `@GetMapping("/{id}")` get(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) → ok(getPaymentUseCase.getPayment(id, userId))
- `@PostMapping("/{id}/cancel")` cancel(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId): `cancelPaymentUseCase.cancel(id, userId); return ResponseEntity.ok(ApiResponse.ok());`

## 8. 단위 테스트 (Wave 4) — `src/test/java/com/ticketch/paymentservice/application/service/PaymentServiceTest.java`
`@ExtendWith(MockitoExtension.class)`. `@Mock` ValidateReservationPort, LoadPaymentPort, SavePaymentPort, ProcessPaymentPort, PublishPaymentEventPort; `@InjectMocks PaymentService`.
공통 stub: `when(savePaymentPort.save(any())).thenAnswer(inv -> { Payment p = inv.getArgument(0); return p.getId()!=null ? p : Payment.builder().id(1L).reservationId(p.getReservationId()).userId(p.getUserId()).amount(p.getAmount()).status(p.getStatus()).failureReason(p.getFailureReason()).paidAt(p.getPaidAt()).createdAt(p.getCreatedAt()).build(); });`
(또는 간단히 첫 save에 id 부여하도록 stub) — 핵심은 publish 검증.
- test1 결제성공: `when(validateReservationPort.getReservation(anyLong(),anyLong())).thenReturn(new ValidateReservationPort.ReservationInfo(10L,1L,"PENDING")); when(processPaymentPort.process(anyLong(),any())).thenReturn(new ProcessPaymentPort.ProcessResult(true,null));` → requestPayment 호출 → `verify(publishPaymentEventPort).publishCompleted(any())` + `verify(publishPaymentEventPort, never()).publishFailed(any())`
- test2 결제실패: process가 `new ProcessResult(false,"거절")` → `verify(publishPaymentEventPort).publishFailed(any())`
- test3 예약상태 비PENDING: getReservation이 status="CONFIRMED" → `assertThatThrownBy(...).isInstanceOf(BusinessException.class)`
- AssertJ + Mockito(verify, ArgumentCaptor). RabbitMQ/DB/Feign 실인프라 불필요.
