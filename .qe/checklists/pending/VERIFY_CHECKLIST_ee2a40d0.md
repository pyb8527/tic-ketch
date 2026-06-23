# VERIFY_CHECKLIST — Phase 4: Reservation Service

- **UUID**: ee2a40d0
- **Type**: code
- **대상**: `services/reservation-service`

---

## 빌드 / 구조
- [ ] `./gradlew :services:reservation-service:build` 성공
- [ ] 패키지 구조가 헥사고날 규칙(domain/application/adapter) 및 event-service 컨벤션과 일치
- [ ] `domain` 패키지에 Spring/JPA/Redis import 없음 (순수 Java)
- [ ] 모든 Output Port 인터페이스에 대응하는 `adapter/out` 구현체가 1:1 존재

## 기능 — 분산락 / TTL
- [ ] 동일 좌석에 대한 동시 선점 요청 시 정확히 1건만 PENDING 성공, 나머지는 `SEAT_ALREADY_HELD` 예외
- [ ] 선점 성공 시 `reservation:temp:{id}` Redis Hash가 TTL 300s로 생성됨
- [ ] `ReservationDomainService.isExpired`가 `expiresAt` 기준으로 만료 판정
- [ ] `ExpiredReservationScheduler`가 만료된 PENDING 예약을 EXPIRED로 전이하고 좌석 회수 이벤트 발행

## 기능 — 메시징 / 통신
- [ ] `payment.completed` 수신 → 예약 CONFIRMED 전이
- [ ] `payment.failed` 수신 → 예약 CANCELLED 전이 + `seat.released` 발행
- [ ] `ValidateSeatPort`(Feign) 호출 실패 시 Circuit Breaker Fallback 동작 (예외 전파 차단)
- [ ] MQ 발행/수신 DTO가 `common:ticketch-events` 기존 클래스를 사용 (중복 정의 없음)

## 기능 — 대기열 / API
- [ ] `POST /api/queue/{eventId}/enter` 시 Sorted Set에 timestamp score로 등록
- [ ] `GET /api/queue/{eventId}` 가 본인 순번 반환
- [ ] `POST /api/reservations` 201 + 예약 ID 반환, `GET /api/reservations/{id}` 남은 시간 포함 응답
- [ ] 공통 응답 형식(`ApiResponse`/`ErrorResponse`, ticketch-common) 준수

## 테스트 / 품질
- [ ] 통합 테스트(Testcontainers MySQL+Redis) 통과 — 동시 선점 시나리오 포함
- [ ] 기존 테스트(user/event-service) 전부 통과 (회귀 없음)
- [ ] 변경 코드에 OWASP Top 10 보안 취약점 없음 (입력 검증, 권한 체크)
- [ ] Reservation 도메인이 인증 정보(X-User-Id) 소유권 검증 — 타인 예약 취소 불가

## 마무리
- [ ] `services/reservation-service/**` 외 파일 변경 없음 (경계 준수)
- [ ] 완료 후 `.qe/TASK_LOG.md`의 Phase 4 항목을 ✅로 갱신
