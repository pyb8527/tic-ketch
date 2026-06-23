# Phase 4 CONTRACTS — Reservation Service (단일 진실 공급원)

> 모든 Wave 팀원은 이 파일을 그대로 따른다. 시그니처/패키지/이름을 임의 변경 금지.
> 루트 패키지: `com.ticketch.reservationservice` (이하 `…`)
> 소스 경로: `services/reservation-service/src/main/java/com/ticketch/reservationservice/`

## 0. 공통 규칙 (event-service 컨벤션 준수)
- 도메인 모델/도메인서비스: 순수 Java. 허용 import = JDK + Lombok + `com.ticketch.common.*` + `com.ticketch.events.*`. (Spring/JPA/Redis import 금지)
- Output Port = 인터페이스(`application/port/out`), 구현체 = `@Component` (`adapter/out`)
- UseCase(Input Port) = 인터페이스(`application/port/in`), 구현체 = `@Service` (`application/service`)
- Command/Result/Detail은 해당 UseCase 인터페이스 안에 `record` 중첩 (event-service 패턴)
- 예외는 `throw new BusinessException(ErrorCode.XXX)` (common). ErrorCode에 이미 있는 코드만 사용:
  `SEAT_NOT_AVAILABLE, SEAT_ALREADY_HELD, RESERVATION_NOT_FOUND, RESERVATION_EXPIRED, RESERVATION_ALREADY_CANCELLED, RESERVATION_ALREADY_CONFIRMED, RESERVATION_NOT_OWNED, LOCK_ACQUISITION_FAILED, INVALID_INPUT`
- 컨트롤러 응답: `ResponseEntity<ApiResponse<T>>`, `ApiResponse.ok(data)` (common)
- 공유 이벤트 DTO는 `com.ticketch.events.*` 기존 클래스 **그대로** 사용(신규 정의 금지):
  - `SeatHeldEvent{reservationId,seatId,eventId,userId,expiresAt}` (Builder)
  - `SeatReleasedEvent{reservationId,seatId,eventId,reason}` + enum `SeatReleasedEvent.ReleaseReason{EXPIRED,CANCELLED,PAYMENT_FAILED}`
  - `PaymentCompletedEvent{paymentId,reservationId,userId,seatId,eventId,amount,paidAt}`
  - `PaymentFailedEvent{paymentId,reservationId,userId,seatId,eventId,reason,failedAt}`

## 1. 도메인 (Wave 1-E)

### `domain/model/ReservationStatus.java`
```java
public enum ReservationStatus { PENDING, CONFIRMED, CANCELLED, EXPIRED }
```

### `domain/model/Reservation.java`
순수 Java. `@Getter @Builder @AllArgsConstructor`. 필드: `Long id; Long userId; Long seatId; Long eventId; ReservationStatus status; LocalDateTime expiresAt; LocalDateTime createdAt;`
메서드:
- `static Reservation create(Long userId, Long seatId, Long eventId, LocalDateTime expiresAt)` → status=PENDING, createdAt=LocalDateTime.now()
- `boolean isExpired(LocalDateTime now)` → `now.isAfter(expiresAt)`
- `void confirm()` → status가 PENDING이 아니면: CONFIRMED면 `RESERVATION_ALREADY_CONFIRMED`, CANCELLED/EXPIRED면 `RESERVATION_EXPIRED` 던짐; 통과 시 status=CONFIRMED
- `void cancel()` → CANCELLED면 `RESERVATION_ALREADY_CANCELLED`, CONFIRMED면 `RESERVATION_ALREADY_CONFIRMED`; 통과 시 status=CANCELLED
- `void expire()` → status가 PENDING일 때만 EXPIRED로, 아니면 무시(멱등)
- `void assertOwnedBy(Long requesterId)` → `!userId.equals(requesterId)`이면 `RESERVATION_NOT_OWNED`
> id/status는 가변(setter 없이 위 메서드로만 변경). @Builder용 전체 필드 생성자 유지.

### `domain/model/QueueEntry.java`
순수 Java. `@Getter @Builder @AllArgsConstructor`. 필드: `Long userId; Long eventId; long score;`
- `static QueueEntry of(Long eventId, Long userId, long score)`

### `domain/service/ReservationDomainService.java`
`@Service`. 메서드:
- `Reservation toExpired(Reservation r)` → `r.expire(); return r;`
- 만료 스윕 판단은 LoadReservationPort.findExpiredPending에 위임하므로 여기서는 상태전이 헬퍼만 둠.

## 2. Input Ports (Wave 2-F) — `application/port/in/`

### `HoldSeatUseCase`
```java
HoldSeatResult holdSeat(HoldSeatCommand command);
record HoldSeatCommand(Long userId, Long seatId, Long eventId) {}
record HoldSeatResult(Long reservationId, java.time.LocalDateTime expiresAt) {}
```

### `ConfirmReservationUseCase`
```java
void confirm(Long reservationId);   // payment.completed 수신 시
```

### `CancelReservationUseCase`
```java
void cancel(CancelCommand command);
// requesterId != null → 소유권 검증(사용자 취소). null → 시스템(결제실패/만료)
record CancelCommand(Long reservationId, Long requesterId,
                     com.ticketch.events.reservation.SeatReleasedEvent.ReleaseReason reason) {}
```

### `GetReservationUseCase`
```java
ReservationDetail getReservation(Long reservationId, Long requesterId);
java.util.List<ReservationDetail> getMyReservations(Long userId);
record ReservationDetail(Long id, Long seatId, Long eventId, String status,
                         java.time.LocalDateTime expiresAt, long remainingSeconds) {}
```

### `QueueUseCase`
```java
QueueStatus enter(Long eventId, Long userId);
QueueStatus getStatus(Long eventId, Long userId);
record QueueStatus(long position, long totalWaiting) {}  // position: 1-based, 미존재 시 -1
```

## 3. Output Ports (Wave 2-G) — `application/port/out/`

```java
// LoadReservationPort
Optional<Reservation> findById(Long id);
List<Reservation> findByUserId(Long userId);
List<Reservation> findExpiredPending(LocalDateTime now);  // status=PENDING AND expiresAt<now

// SaveReservationPort
Reservation save(Reservation reservation);   // id 채워서 반환

// AcquireSeatLockPort  (Redisson; 락 획득 실패 시 LOCK_ACQUISITION_FAILED)
<T> T executeWithLock(Long seatId, java.util.function.Supplier<T> action);

// HoldSeatCachePort  (Redis Hash reservation:temp:{id}, TTL)
void hold(Reservation reservation, java.time.Duration ttl);

// ReleaseSeatCachePort
void release(Long reservationId);

// ManageQueuePort  (Redis Sorted Set queue:{eventId})
void add(Long eventId, Long userId, long score);
long rank(Long eventId, Long userId);   // 1-based, 없으면 -1
long size(Long eventId);

// ValidateSeatPort  (Feign → event-service)
boolean isAvailable(Long eventId, Long seatId);   // 좌석 status==AVAILABLE 여부

// PublishSeatReleasedPort  (RabbitMQ seat.exchange)
void publishSeatHeld(com.ticketch.events.reservation.SeatHeldEvent event);
void publishSeatReleased(com.ticketch.events.reservation.SeatReleasedEvent event);
```
import: `com.ticketch.reservationservice.domain.model.Reservation` 등, `java.util.*`, `java.time.*`

## 4. 설정/부트스트랩 (Wave 1)

### 1-A `ReservationServiceApplication.java` + resources
```java
@SpringBootApplication(scanBasePackages = {"com.ticketch.reservationservice", "com.ticketch.common"})
@EnableDiscoveryClient @EnableFeignClients @EnableScheduling
```
- `src/main/resources/application.yml`:
```yaml
spring:
  application:
    name: reservation-service
  config:
    import: optional:configserver:http://localhost:8888
```
- `src/test/resources/application-test.yml`: H2(MODE=MySQL) + flyway.enabled:false + jpa ddl-auto:create-drop + `spring.data.redis.host:localhost` + rabbitmq(admin/admin) + `eureka.client.enabled:false` (event-service 테스트 yml과 동일 구조; redis/rabbitmq host/port는 @DynamicPropertySource로 덮어씀)
- `src/test/resources/testcontainers.properties`: `docker.client.strategy=org.testcontainers.dockerclient.NpipeSocketClientProviderStrategy`

### 1-B `db/migration/V1__create_reservations.sql`
doc/SPEC.md §8 reservations 스키마 그대로 (id PK, user_id, seat_id, event_id, status ENUM, expires_at, created_at, updated_at).

### 1-C `config/RedissonConfig.java`
`@Configuration`. `@Bean RedissonClient redissonClient(@Value("${spring.data.redis.host:localhost}") String host, @Value("${spring.data.redis.port:6379}") int port)` → `Config.useSingleServer().setAddress("redis://"+host+":"+port)`.

### 1-D `config/RabbitConfig.java`
`@Configuration`. 상수:
`SEAT_EXCHANGE="seat.exchange"`, `SEAT_RELEASED_KEY="seat.released"`, `SEAT_HELD_KEY="seat.held"`,
`PAYMENT_EXCHANGE="payment.exchange"`,
`PAYMENT_COMPLETED_KEY="payment.completed"`, `PAYMENT_FAILED_KEY="payment.failed"`,
`PAYMENT_COMPLETED_Q="payment.completed.reservation.queue"`, `PAYMENT_FAILED_Q="payment.failed.reservation.queue"`.
Bean: `TopicExchange seatExchange`, `TopicExchange paymentExchange`, 두 Queue(durable), 두 Binding(payment.* → 각 큐), `Jackson2JsonMessageConverter messageConverter`, 그리고 RabbitTemplate에 converter 적용(`@Bean RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter mc)`).
> reservation은 payment 큐를 **소비**, seat.exchange로 **발행**. seat 큐 바인딩은 event-service가 소유하므로 여기서 선언 안 함(발행만).

## 5. Out 어댑터 (Wave 3) — `adapter/out/`

### `out/persistence/` (한 팀원이 3파일)
- `ReservationJpaEntity` `@Entity @Table(name="reservations")` `@Getter @Builder @NoArgsConstructor @AllArgsConstructor`; 필드 매핑(status `@Enumerated(STRING)`); `toDomain()`, `static fromDomain(Reservation)` (event-service EventJpaEntity 패턴).
- `ReservationJpaRepository extends JpaRepository<ReservationJpaEntity,Long>`; 메서드 `List<…> findByUserId(Long userId)`; `@Query`로 `findByStatusAndExpiresAtBefore(ReservationStatus, LocalDateTime)` 또는 파생쿼리 `findByStatusAndExpiresAtBefore`.
- `ReservationPersistenceAdapter implements LoadReservationPort, SaveReservationPort` (`@Component @RequiredArgsConstructor`).

### `out/redis/RedissonLockAdapter.java` implements `AcquireSeatLockPort`
`@Component @RequiredArgsConstructor`; `RedissonClient` 주입. key=`"seat:lock:"+seatId`. `RLock lock = redisson.getLock(key); boolean ok = lock.tryLock(3, 10, TimeUnit.SECONDS);` 실패 시 `BusinessException(LOCK_ACQUISITION_FAILED)`; finally에서 `isHeldByCurrentThread()`면 unlock. action.get() 반환.

### `out/redis/ReservationCacheAdapter.java` implements `HoldSeatCachePort, ReleaseSeatCachePort`
`@Component`; `RedisTemplate<String,Object>` 또는 `StringRedisTemplate` 주입. key=`"reservation:temp:"+id`. hold: HashOps put(reservationId/seatId/eventId/userId) 후 `expire(key, ttl)`. release: `delete(key)`.
> StringRedisTemplate 사용 권장(값 String 변환). RedisConfig Bean 필요 시 같은 파일에 최소 정의 또는 Spring Boot 자동설정 사용.

### `out/redis/QueueRedisAdapter.java` implements `ManageQueuePort`
`@Component`; `StringRedisTemplate`. ZSet ops: key=`"queue:"+eventId`. add: `zSetOps.add(key, userId.toString(), score)`. rank: `zSetOps.rank(...)` (0-based) → `+1` 반환, null이면 -1. size: `zSetOps.zCard(key)`.

### `out/external/EventFeignClient.java` + `EventClient.java`(=ValidateSeatPort 구현) + `EventClientFallback.java`
- `@FeignClient(name="event-service", fallback=EventClientFallback.class)` interface `EventFeignClient { @GetMapping("/api/events/{eventId}/seats") ApiResponse<List<SeatStatusDto>> getSeats(@PathVariable Long eventId); }`
  - 로컬 DTO `record SeatStatusDto(Long id, String rowName, Integer seatNumber, String status) {}` (event-service SeatResponse와 필드 호환; Jackson record 역직렬화)
  - `ApiResponse`는 common 것 사용. **주의**: 역직렬화 위해 reservation에 로컬 래퍼 record 사용 권장 →
    대신 `record EventSeatsResponse(String code, String message, List<SeatStatusDto> data) {}` 정의하고 Feign 반환타입으로 사용(common ApiResponse는 no-arg 생성자 없어 역직렬화 불가).
- `EventClient implements ValidateSeatPort` (`@Component @RequiredArgsConstructor`): `isAvailable(eventId, seatId)` → feign.getSeats(eventId).data()에서 id==seatId 찾아 `"AVAILABLE".equals(status)`; 없으면 false.
- `EventClientFallback implements EventFeignClient`: getSeats → 빈 응답(`new EventSeatsResponse("C004","fallback",List.of())`) 반환 → isAvailable=false (fail-closed).
- resilience4j: application.yml(config-repo)에 의존하지 않도록 Feign fallback Bean으로 회로 차단 대체. (CircuitBreaker 어노테이션 불필요)

### `out/messaging/SeatEventPublisher.java` implements `PublishSeatReleasedPort`
`@Component @RequiredArgsConstructor`; `RabbitTemplate`. publishSeatHeld → `convertAndSend(SEAT_EXCHANGE, SEAT_HELD_KEY, event)`. publishSeatReleased → `convertAndSend(SEAT_EXCHANGE, SEAT_RELEASED_KEY, event)`. (상수는 RabbitConfig 참조)

## 6. Application 서비스 (Wave 3) — `application/service/`

### `HoldSeatService implements HoldSeatUseCase` `@Service @RequiredArgsConstructor`
의존: ValidateSeatPort, AcquireSeatLockPort, LoadReservationPort, SaveReservationPort, HoldSeatCachePort, PublishSeatReleasedPort.
흐름(doc/SPEC HoldSeat):
1. `if(!validateSeatPort.isAvailable(cmd.eventId(), cmd.seatId())) throw SEAT_NOT_AVAILABLE`
2. `return acquireSeatLockPort.executeWithLock(seatId, () -> { ... })`:
   a. (락 안) DB 재확인: 이 좌석에 활성(PENDING/CONFIRMED) 예약 있으면 `SEAT_ALREADY_HELD`
      → `loadReservationPort.findByUserId`로는 부족 → Repository에 `existsBySeatIdAndStatusIn` 필요. **Save/Load 포트에 추가 금지**: 대신 LoadReservationPort에 `boolean existsActiveBySeatId(Long seatId)` 추가하고 어댑터/Repository 구현. (CONTRACTS 갱신: §3 LoadReservationPort에 이 메서드 포함)
   b. `Reservation r = Reservation.create(userId, seatId, eventId, now.plusMinutes(5))`
   c. `r = saveReservationPort.save(r)`
   d. `holdSeatCachePort.hold(r, Duration.ofMinutes(5))`
   e. `publishSeatReleasedPort.publishSeatHeld(SeatHeldEvent.builder()....build())`
   f. `return new HoldSeatResult(r.getId(), r.getExpiresAt())`
> ⚠️ §3 LoadReservationPort에 추가: `boolean existsActiveBySeatId(Long seatId);` (status in PENDING,CONFIRMED)

### `ReservationCommandService implements ConfirmReservationUseCase, CancelReservationUseCase` `@Service`
의존: LoadReservationPort, SaveReservationPort, ReleaseSeatCachePort, PublishSeatReleasedPort.
- confirm(id): findById or `RESERVATION_NOT_FOUND`; `r.confirm()`; save.
- cancel(cmd): findById or `RESERVATION_NOT_FOUND`; `if(cmd.requesterId()!=null) r.assertOwnedBy(cmd.requesterId())`; `r.cancel()`; save; `releaseSeatCachePort.release(id)`; `publishSeatReleased(SeatReleasedEvent.builder().reservationId..seatId..eventId..reason(cmd.reason()).build())`.

### `QueueService implements QueueUseCase` `@Service`
의존: ManageQueuePort. enter: score=System.currentTimeMillis() **금지**(결정성). → 스코프상 enter 시 현재시간 필요하나, 서비스 코드에서 `System.currentTimeMillis()` 허용(런타임). add 후 getStatus 반환. getStatus: rank/size 조합.
> 주: 워크플로 결정성 제약은 빌드시점 코드 생성에만 해당. 런타임 `System.currentTimeMillis()`는 정상 사용.

### `GetReservationService implements GetReservationUseCase` `@Service`
의존: LoadReservationPort. getReservation: findById or NOT_FOUND; requesterId 소유권 검증; remainingSeconds = max(0, Duration.between(now, expiresAt).getSeconds()); detail 매핑. getMyReservations: findByUserId 매핑.

## 7. In 어댑터 (Wave 3) — `adapter/in/`

### `in/web/ReservationController.java` + `in/web/dto/`
`@RestController @RequestMapping("/api/reservations") @RequiredArgsConstructor`. userId는 `@RequestHeader("X-User-Id") Long userId` (Gateway 미구현이라 헤더 직접 수신).
- `POST /` body `HoldSeatRequest(Long seatId, Long eventId)` → holdSeatUseCase → 201 `ApiResponse.ok(HoldSeatResult)`
- `GET /{id}` → getReservationUseCase.getReservation(id, userId) → ok(detail)
- `GET /me` → getMyReservations(userId)
- `DELETE /{id}` → cancelReservationUseCase.cancel(new CancelCommand(id, userId, ReleaseReason.CANCELLED)) → ok()
dto: `HoldSeatRequest`(record, `@NotNull` 필드).

### `in/web/QueueController.java`
`@RequestMapping("/api/queue")`. `POST /{eventId}/enter` (X-User-Id) → enter → ok(QueueStatus). `GET /{eventId}` → getStatus → ok.

### `in/messaging/PaymentEventConsumer.java`
`@Component @RequiredArgsConstructor`. `@RabbitListener(queues = RabbitConfig.PAYMENT_COMPLETED_Q)` onCompleted(PaymentCompletedEvent e) → confirmReservationUseCase.confirm(e.getReservationId()). `@RabbitListener(queues = RabbitConfig.PAYMENT_FAILED_Q)` onFailed(PaymentFailedEvent e) → cancelReservationUseCase.cancel(new CancelCommand(e.getReservationId(), null, ReleaseReason.PAYMENT_FAILED)).

### `in/scheduler/ExpiredReservationScheduler.java`
`@Component @RequiredArgsConstructor`. `@Scheduled(fixedDelay=30000)` sweep(): loadReservationPort.findExpiredPending(now) 각각 → cancel(new CancelCommand(id, null, ReleaseReason.EXPIRED))  (혹은 별도 expire 경로). 로그 남김.

## 8. 테스트 (Wave 4) — `src/test/java/.../HoldSeatServiceIntegrationTest.java`
`@SpringBootTest @ActiveProfiles("test") @Testcontainers`. Testcontainers: `MySQLContainer("mysql:8")`, `GenericContainer("redis:7").withExposedPorts(6379)`. `@DynamicPropertySource`로 datasource/redis host·port 주입(+`spring.flyway.enabled=true` MySQL일 때). RabbitMQ 빈은 `@MockBean`으로 대체(또는 @MockBean PublishSeatReleasedPort, ValidateSeatPort=항상 available). 시나리오: 같은 seatId로 N개 스레드가 holdSeat 동시 호출 → 정확히 1건 성공, 나머지 BusinessException(SEAT_ALREADY_HELD/LOCK_ACQUISITION_FAILED). CountDownLatch + ExecutorService.
> Docker 미기동 시 실행 불가 — 코드만 작성, 실행 검증은 Docker 기동 후.

## 9. 갱신 사항(중요)
- §3 LoadReservationPort에 `boolean existsActiveBySeatId(Long seatId)` 추가(위 6 참조). Repository에 `boolean existsBySeatIdAndStatusIn(Long seatId, Collection<ReservationStatus> statuses)` 파생쿼리로 구현.
