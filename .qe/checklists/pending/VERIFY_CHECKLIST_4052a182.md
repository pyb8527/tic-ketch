# VERIFY_CHECKLIST — Phase 6: Notification Service

- **UUID**: 4052a182
- **Type**: code
- **대상**: `services/notification-service`

---

## 빌드 / 구조
- [ ] `./gradlew :services:notification-service:build` 성공
- [ ] 패키지 구조가 헥사고날 규칙(domain/application/adapter)과 일치
- [ ] `domain` 패키지에 Spring/Mongo import 없음 (순수 Java; @Component 도메인 서비스는 예외 허용)
- [ ] 모든 Output Port 인터페이스에 대응하는 `adapter/out` 구현체가 1:1 존재

## 기능 — 알림 발송
- [ ] `payment.completed` 수신 시 PAYMENT_COMPLETED 알림이 생성됨
- [ ] `payment.failed` 수신 시 PAYMENT_FAILED 알림이 생성됨
- [ ] 이메일 발송 성공 시 Notification.status=SENT, sentAt 설정 후 저장
- [ ] 이메일 발송 실패 시 Notification.status=FAILED로 저장
- [ ] `NotificationFactory`가 타입별로 적절한 title/body를 생성 (결제완료/실패 구분)
- [ ] MockEmailAdapter가 콘솔(log)에 이메일 내용을 출력

## 기능 — 메시징 / 저장
- [ ] Notification 전용 큐(`payment.*.notification.queue`)가 `payment.exchange`에 바인딩됨 (Reservation 큐와 이름 충돌 없음)
- [ ] `Jackson2JsonMessageConverter`로 PaymentCompletedEvent/FailedEvent 역직렬화 가능
- [ ] MQ 수신 DTO가 `common:ticketch-events` 기존 클래스 사용 (중복 정의 없음)
- [ ] `NotificationMongoAdapter`가 SaveNotificationPort를 구현하고 MongoRepository로 저장

## 테스트
- [ ] NotificationService 단위 테스트 통과 — 발송 성공(SENT)/실패(FAILED) 분기
- [ ] NotificationFactory 단위 테스트 통과 — 타입별 title/body 검증
- [ ] 기존 테스트(user/event/reservation/payment) 전부 통과 (회귀 없음)
- [ ] 변경 코드에 OWASP Top 10 보안 취약점 없음

## 마무리
- [ ] `services/notification-service/**` 외 파일 변경 없음 (경계 준수)
- [ ] 완료 후 `.qe/TASK_LOG.md`의 Phase 6 항목을 ✅로 갱신
