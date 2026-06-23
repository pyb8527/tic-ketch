# Phase 6 CONTRACTS — Notification Service (단일 진실 공급원)

> 모든 Wave 팀원은 이 파일을 그대로 따른다. 시그니처/패키지/이름 임의 변경 금지.
> 루트 패키지: `com.ticketch.notificationservice` (이하 `…`)
> 소스 경로: `services/notification-service/src/main/java/com/ticketch/notificationservice/`
> 참조: `services/payment-service`(헥사고날), `services/event-service`(MQ Consumer + MessageConverter). **Feign/Redis/JPA 미사용, MongoDB 사용.**

## 0. 공통 규칙
- 도메인 모델: 순수 Java (JDK + Lombok + `com.ticketch.common.*`). NotificationFactory는 @Component(도메인 서비스) 허용
- Output Port = 인터페이스(`application/port/out`), 구현체 `@Component`(`adapter/out`)
- UseCase = 인터페이스(`application/port/in`), 구현체 `@Service`(`application/service`)
- 공유 이벤트 DTO (com.ticketch.events.payment, **그대로 사용, 신규 정의 금지**):
  - `PaymentCompletedEvent{paymentId, reservationId, userId, seatId, eventId, amount(Integer), paidAt}` (getter)
  - `PaymentFailedEvent{paymentId, reservationId, userId, seatId, eventId, reason, failedAt}` (getter)

## 1. 도메인 (Wave 1)

### `domain/model/NotificationType.java`
```java
public enum NotificationType { PAYMENT_COMPLETED, PAYMENT_FAILED, RESERVATION_EXPIRED }
```
### `domain/model/NotificationStatus.java`
```java
public enum NotificationStatus { SENT, FAILED }
```
### `domain/model/Notification.java`
순수 Java. `@Getter @Builder @AllArgsConstructor`. 필드:
`String id; Long userId; NotificationType type; String title; String body; NotificationStatus status; java.time.LocalDateTime sentAt;`
- `static Notification create(Long userId, NotificationType type, String title, String body)` → status=null, sentAt=null, id=null
- `void markSent()` → status=SENT; sentAt=LocalDateTime.now();
- `void markFailed()` → status=FAILED; sentAt=LocalDateTime.now();

### `domain/service/NotificationFactory.java`
`@Component`. `public Notification create(Long userId, NotificationType type, Long reservationId, Integer amount)`:
- switch(type):
  - PAYMENT_COMPLETED → title="결제 완료", body="예약 #"+reservationId+" 결제가 완료되었습니다." + (amount!=null ? " 금액: "+amount+"원" : "")
  - PAYMENT_FAILED → title="결제 실패", body="예약 #"+reservationId+" 결제가 실패했습니다."
  - RESERVATION_EXPIRED → title="예약 만료", body="예약 #"+reservationId+" 가 만료되었습니다."
- `return Notification.create(userId, type, title, body);`
- Korean Javadoc.

## 2. Input Port (Wave 2) — `application/port/in/SendNotificationUseCase.java`
```java
void send(SendNotificationCommand command);
record SendNotificationCommand(Long userId, NotificationType type, Long reservationId, Integer amount) {}
```
import `…domain.model.NotificationType`.

## 3. Output Ports (Wave 2) — `application/port/out/`
```java
// SaveNotificationPort
Notification save(Notification notification);
// SendEmailPort
boolean send(Notification notification);   // true=발송 성공
```
import `…domain.model.Notification`.

## 4. 설정/부트스트랩 (Wave 1)

### `NotificationServiceApplication.java` + resources
```java
@SpringBootApplication(scanBasePackages = {"com.ticketch.notificationservice", "com.ticketch.common"})
@EnableDiscoveryClient
```
- `application.yml`: `spring.application.name: notification-service` + `spring.config.import: optional:configserver:http://localhost:8888`
- `src/test/resources/application-test.yml`: `spring.rabbitmq.listener.simple.auto-startup:false`, `eureka.client.enabled:false`, `spring.data.mongodb.uri: mongodb://localhost:27017/ticketch_notification_test` (단위 테스트는 컨텍스트 미로딩이라 미사용이나 일관성 위해 작성)

### `config/RabbitConfig.java`
`@Configuration`. 상수:
`PAYMENT_EXCHANGE="payment.exchange"`, `PAYMENT_COMPLETED_KEY="payment.completed"`, `PAYMENT_FAILED_KEY="payment.failed"`,
`PAYMENT_COMPLETED_Q="payment.completed.notification.queue"`, `PAYMENT_FAILED_Q="payment.failed.notification.queue"`.
Bean: `TopicExchange paymentExchange()`, `Queue paymentCompletedQueue()`(durable), `Queue paymentFailedQueue()`(durable), `Binding paymentCompletedBinding()`(completedQ→exchange, PAYMENT_COMPLETED_KEY), `Binding paymentFailedBinding()`(failedQ→exchange, PAYMENT_FAILED_KEY), `Jackson2JsonMessageConverter messageConverter()`.
> event-service RabbitMQConfig 스타일. RabbitTemplate 불필요(소비 전용). MessageConverter Bean이 있으면 Boot가 리스너 팩토리에 적용.

## 5. Out 어댑터 (Wave 3)

### `out/persistence/` (3파일)
- `NotificationDocument` `@Document(collection="notifications")` `@Getter @Builder @NoArgsConstructor @AllArgsConstructor`. 필드: `@Id String id; Long userId; NotificationType type; String title; String body; NotificationStatus status; LocalDateTime sentAt;`. `toDomain()`, `static fromDomain(Notification n)`. (org.springframework.data.mongodb.core.mapping.Document, org.springframework.data.annotation.Id)
- `NotificationMongoRepository extends MongoRepository<NotificationDocument, String>` (org.springframework.data.mongodb.repository.MongoRepository)
- `NotificationMongoAdapter` `@Component @RequiredArgsConstructor implements SaveNotificationPort`: `save(Notification n)` → `repository.save(NotificationDocument.fromDomain(n)).toDomain()`

### `out/email/MockEmailAdapter.java` implements `SendEmailPort`
`@Component @Slf4j`: `send(Notification n)` → `log.info("📧 [MOCK EMAIL] userId={} type={} | {} - {}", n.getUserId(), n.getType(), n.getTitle(), n.getBody()); return true;`

## 6. Application 서비스 (Wave 3)

### `application/service/NotificationService.java` implements `SendNotificationUseCase`
`@Service @RequiredArgsConstructor @Slf4j`. 의존: NotificationFactory, SendEmailPort, SaveNotificationPort.
- `public void send(SendNotificationCommand cmd)`:
  1. `Notification n = notificationFactory.create(cmd.userId(), cmd.type(), cmd.reservationId(), cmd.amount());`
  2. `boolean ok = sendEmailPort.send(n);`
  3. `if (ok) n.markSent(); else n.markFailed();`
  4. `saveNotificationPort.save(n);`
- SendNotificationCommand는 `SendNotificationUseCase.SendNotificationCommand`.

## 7. In 어댑터 (Wave 3) — `adapter/in/messaging/PaymentNotificationConsumer.java`
`@Component @RequiredArgsConstructor @Slf4j`. 의존: SendNotificationUseCase.
- `@RabbitListener(queues = RabbitConfig.PAYMENT_COMPLETED_Q)` `public void onPaymentCompleted(PaymentCompletedEvent e)` →
  `sendNotificationUseCase.send(new SendNotificationUseCase.SendNotificationCommand(e.getUserId(), NotificationType.PAYMENT_COMPLETED, e.getReservationId(), e.getAmount()));`
- `@RabbitListener(queues = RabbitConfig.PAYMENT_FAILED_Q)` `public void onPaymentFailed(PaymentFailedEvent e)` →
  `sendNotificationUseCase.send(new SendNotificationUseCase.SendNotificationCommand(e.getUserId(), NotificationType.PAYMENT_FAILED, e.getReservationId(), null));`
- imports: org.springframework.amqp.rabbit.annotation.RabbitListener, RabbitConfig 상수, NotificationType, events.

## 8. 단위 테스트 (Wave 4)
### `…/application/service/NotificationServiceTest.java`
`@ExtendWith(MockitoExtension.class)`. `@Mock SendEmailPort sendEmailPort; @Mock SaveNotificationPort saveNotificationPort;`. **NotificationFactory는 실제 객체 사용**: `@BeforeEach`에서 `notificationService = new NotificationService(new NotificationFactory(), sendEmailPort, saveNotificationPort);` (생성자 주입). 로그는 SLF4J Logger로 시나리오 출력.
- test1 발송성공: `when(sendEmailPort.send(any())).thenReturn(true);` → `service.send(new SendNotificationCommand(1L, NotificationType.PAYMENT_COMPLETED, 5L, 50000));` → `ArgumentCaptor<Notification>` 로 `verify(saveNotificationPort).save(captor.capture())` → `assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT)`
- test2 발송실패: `when(sendEmailPort.send(any())).thenReturn(false);` → send → save된 Notification.status==FAILED
### `…/domain/service/NotificationFactoryTest.java`
순수 단위(no Mockito 필요). `NotificationFactory f = new NotificationFactory();`
- PAYMENT_COMPLETED: `f.create(1L, NotificationType.PAYMENT_COMPLETED, 5L, 50000)` → title "결제 완료" 포함, body에 "5" 포함, amount 포함
- PAYMENT_FAILED: title "결제 실패"
- AssertJ 사용.
