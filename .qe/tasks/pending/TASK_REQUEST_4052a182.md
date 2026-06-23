<!-- chained-from: Qgenerate-spec -->
# TASK_REQUEST — Phase 6: Notification Service

- **UUID**: 4052a182
- **Type**: code
- **Phase**: Phase 6 / 단계별 구현 계획 (doc/SPEC.md §15)
- **대상 모듈**: `services/notification-service`
- **참조 컨벤션**: `services/payment-service`(헥사고날·RabbitMQ), `services/event-service`(MQ Consumer·MessageConverter)

---

## What (비즈니스 목표)
결제 이벤트(`payment.completed`/`payment.failed`)를 **RabbitMQ로 수신**해 사용자에게 **알림(목업 이메일)을 발송**하고, **발송 이력을 MongoDB에 저장**한다.

**완료 조건(핵심)**
1. `payment.completed` 수신 → 콘솔에 결제완료 이메일 출력 + MongoDB 저장(status=SENT)
2. `payment.failed` 수신 → 결제실패 알림 출력 + MongoDB 저장
3. Phase 5 Payment가 `payment.exchange`에 발행하면 자동 연동

## How (기술 구현 로직)
- **MQ 소비**: Notification 전용 큐 `payment.completed.notification.queue` / `payment.failed.notification.queue`를 `payment.exchange`(topic, 기존)에 바인딩. (Reservation의 큐와 별개 — topic이라 각자 사본 수신)
- **이벤트 DTO**: `com.ticketch.events.payment.PaymentCompletedEvent`/`PaymentFailedEvent` (기존, 신규 정의 금지). Jackson 역직렬화를 위해 `Jackson2JsonMessageConverter` Bean 제공 (Boot가 리스너 팩토리에 적용)
- **저장소**: **MongoDB** (`spring-boot-starter-data-mongodb`) — JPA/Flyway 아님. `notifications` 컬렉션
- **이메일**: `MockEmailAdapter` — 실제 발송 없이 콘솔(log) 출력, 항상 성공 반환
- 패키지 루트 `com.ticketch.notificationservice`. **Feign/Redis 미사용**. 테스트는 **단위 테스트(Mockito)** — Mongo/Rabbit 실인프라 불필요

## 데이터 모델 (doc/SPEC.md §8, MongoDB notifications)
`{ _id(String), userId(Long), type(PAYMENT_COMPLETED|PAYMENT_FAILED|RESERVATION_EXPIRED), title(String), body(String), status(SENT|FAILED), sentAt(ISODate) }`

---

## 체크리스트 (Atomic Items)

### 부트스트랩 / 설정 / 도메인 (Wave 1)
- [ ] **부트스트랩**: `NotificationServiceApplication`(@SpringBootApplication scanBasePackages={notificationservice, common}, @EnableDiscoveryClient) + `application.yml`(name=notification-service, config import) + 테스트용 `application-test.yml`(rabbitmq listener auto-startup:false, eureka.client.enabled:false, mongodb uri localhost) → output: `services/notification-service/src/main/java/com/ticketch/notificationservice/NotificationServiceApplication.java`, `.../resources/application.yml`, `.../test/resources/application-test.yml`
- [ ] **RabbitConfig**: 상수 `PAYMENT_EXCHANGE="payment.exchange"`, `PAYMENT_COMPLETED_KEY="payment.completed"`, `PAYMENT_FAILED_KEY="payment.failed"`, `PAYMENT_COMPLETED_Q="payment.completed.notification.queue"`, `PAYMENT_FAILED_Q="payment.failed.notification.queue"`. Bean: TopicExchange + 2 Queue(durable) + 2 Binding + `Jackson2JsonMessageConverter messageConverter()` → output: `.../config/RabbitConfig.java`
- [ ] **Domain enums**: `NotificationType{PAYMENT_COMPLETED,PAYMENT_FAILED,RESERVATION_EXPIRED}`, `NotificationStatus{SENT,FAILED}` → output: `.../domain/model/NotificationType.java`, `.../domain/model/NotificationStatus.java`
- [ ] **Domain model + factory**: `Notification`{id(String),userId,type,title,body,status,sentAt} (`static create(userId,type,title,body)`, `markSent()`, `markFailed()`) + `NotificationFactory`(@Component, `create(userId,type,reservationId,amount)` → 타입별 title/body 생성) → output: `.../domain/model/Notification.java`, `.../domain/service/NotificationFactory.java`

### 포트 (Wave 2)
- [ ] **Input Port**: `SendNotificationUseCase`(`void send(SendNotificationCommand)` + record SendNotificationCommand(Long userId, NotificationType type, Long reservationId, Integer amount)) → output: `.../application/port/in/SendNotificationUseCase.java` <!-- depends_on: [Domain enums] -->
- [ ] **Output Ports**: `SaveNotificationPort`(`Notification save(Notification)`), `SendEmailPort`(`boolean send(Notification)`) → output: `.../application/port/out/` <!-- depends_on: [Domain model] -->

### 어댑터 + 서비스 (Wave 3)
- [ ] **Mongo Persistence**: `NotificationDocument`(@Document("notifications"), @Id String id, toDomain/fromDomain) + `NotificationMongoRepository extends MongoRepository<NotificationDocument,String>` + `NotificationMongoAdapter`(@Component implements SaveNotificationPort) → output: `.../adapter/out/persistence/` <!-- depends_on: [Output Ports] -->
- [ ] **MockEmailAdapter**: @Component implements SendEmailPort — 콘솔(@Slf4j) 출력 후 true 반환 → output: `.../adapter/out/email/MockEmailAdapter.java` <!-- depends_on: [Output Ports] -->
- [ ] **NotificationService**: implements SendNotificationUseCase. send(): factory.create → sendEmailPort.send → 성공 시 markSent/실패 시 markFailed → saveNotificationPort.save → output: `.../application/service/NotificationService.java` <!-- depends_on: [Output Ports, Domain model+factory] -->
- [ ] **PaymentNotificationConsumer**: @Component, `@RabbitListener(PAYMENT_COMPLETED_Q)` onCompleted(PaymentCompletedEvent) → send(PAYMENT_COMPLETED cmd), `@RabbitListener(PAYMENT_FAILED_Q)` onFailed(PaymentFailedEvent) → send(PAYMENT_FAILED cmd) → output: `.../adapter/in/messaging/PaymentNotificationConsumer.java` <!-- depends_on: [NotificationService, RabbitConfig] -->

### 테스트 (Wave 4 — 단위, Mockito)
- [ ] **NotificationService 단위 테스트**: SendEmailPort/SaveNotificationPort Mockito 목킹 + 실제 NotificationFactory. (1) 이메일 성공 → status=SENT로 save 검증, (2) 이메일 실패(send=false) → status=FAILED로 save 검증 → output: `services/notification-service/src/test/java/com/ticketch/notificationservice/application/service/NotificationServiceTest.java` <!-- depends_on: [NotificationService] -->
- [ ] **NotificationFactory 단위 테스트**: 타입별(PAYMENT_COMPLETED/FAILED) title·body 생성 검증 → output: `services/notification-service/src/test/java/com/ticketch/notificationservice/domain/service/NotificationFactoryTest.java` <!-- depends_on: [Domain model+factory] -->

---

## Notes
- `PaymentCompletedEvent{paymentId,reservationId,userId,seatId,eventId,amount,paidAt}` / `PaymentFailedEvent{...,reason,failedAt}` — common:ticketch-events 기존 DTO 그대로 사용
- Notification 큐는 Reservation 큐와 **이름이 달라야** 함(`*.notification.queue`) — 같은 topic exchange의 같은 키에 각각 바인딩해 둘 다 수신
- MongoDB는 단위 테스트에선 불필요(SaveNotificationPort 목킹). 실 구동 시 docker-compose MongoDB 필요
- Role ownership: `services/notification-service/**`만 수정. common/타 서비스 변경 금지
