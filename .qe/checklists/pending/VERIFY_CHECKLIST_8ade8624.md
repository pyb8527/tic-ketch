# VERIFY_CHECKLIST — Phase 5: Payment Service

- **UUID**: 8ade8624
- **Type**: code (payment)
- **대상**: `services/payment-service`

---

## 빌드 / 구조
- [ ] `./gradlew :services:payment-service:build` 성공
- [ ] 패키지 구조가 헥사고날 규칙(domain/application/adapter) 및 reservation-service 컨벤션과 일치
- [ ] `domain` 패키지에 Spring/JPA import 없음 (순수 Java)
- [ ] 모든 Output Port 인터페이스에 대응하는 `adapter/out` 구현체가 1:1 존재

## 기능 — 결제 플로우
- [ ] `POST /api/payments` 호출 시 Payment가 PENDING으로 저장됨
- [ ] 목업 PG 성공 시 Payment가 COMPLETED로 전이되고 `paid_at`이 설정됨
- [ ] 목업 PG 성공 시 `payment.completed` 키로 `PaymentCompletedEvent` 발행 (seatId/eventId 포함)
- [ ] 목업 PG 실패 시 Payment가 FAILED로 전이되고 `payment.failed` 키로 `PaymentFailedEvent` 발행 (reason 포함)
- [ ] 이벤트의 `seatId`/`eventId`가 Reservation Feign 조회 결과로 채워짐
- [ ] 예약 status가 PENDING이 아니면 결제 요청이 거부됨(예외)

## 기능 — 통신 / DLQ
- [ ] `ValidateReservationPort`(Feign) 호출 실패 시 Circuit Breaker Fallback 동작 (예외 전파 차단)
- [ ] MQ 발행 DTO가 `common:ticketch-events` 기존 클래스 사용 (중복 정의 없음)
- [ ] `payment.exchange`(topic) + `payment.dlx` + `payment.dlq`가 선언됨
- [ ] 발행 키(`payment.completed`/`payment.failed`)가 Reservation 구독 큐 바인딩과 일치 (연동 확인)

## 기능 — 조회 / 취소
- [ ] `GET /api/payments/{id}` 가 결제 상태 반환
- [ ] `POST /api/payments/{id}/cancel` 시 COMPLETED 결제가 REFUNDED로 전이
- [ ] 공통 응답 형식(`ApiResponse`, ticketch-common) 준수

## 테스트 / 보안
- [ ] PaymentService 단위 테스트 통과 — 성공/실패 분기 + 이벤트 발행 검증
- [ ] 기존 테스트(user/event/reservation-service) 전부 통과 (회귀 없음)
- [ ] 변경 코드에 OWASP Top 10 보안 취약점 없음
- [ ] **결제 보안 검토**: 금액(amount) 신뢰 경계 명시 — 목업 범위에서는 요청 바디 수신 허용하되, 실제 PG 전환 시 서버측 가격 검증 필요함을 코드/주석에 명시 (Esecurity-officer 또는 수동 검토)
- [ ] 결제 요청 시 본인(X-User-Id) 예약에 대해서만 결제 가능 (소유권 검증)

## 마무리
- [ ] `services/payment-service/**` 외 파일 변경 없음 (경계 준수)
- [ ] 완료 후 `.qe/TASK_LOG.md`의 Phase 5 항목을 ✅로 갱신
