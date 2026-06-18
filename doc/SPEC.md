# TicKetch — 실시간 티켓 예매 플랫폼 스펙

> MSA · 멀티모듈 · 헥사고날 아키텍처 · 분산환경 기반 백엔드 포트폴리오 프로젝트

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [기술 스택](#2-기술-스택)
3. [전체 아키텍처](#3-전체-아키텍처)
4. [멀티모듈 구조](#4-멀티모듈-구조)
5. [Common 모듈 설계](#5-common-모듈-설계)
6. [헥사고날 아키텍처](#6-헥사고날-아키텍처)
7. [서비스별 상세 설계](#7-서비스별-상세-설계)
8. [데이터 모델](#8-데이터-모델)
9. [Redis 설계](#9-redis-설계)
10. [RabbitMQ 설계](#10-rabbitmq-설계)
11. [API 설계](#11-api-설계)
12. [CI/CD — Jenkins](#12-cicd--jenkins)
13. [테스트 전략](#13-테스트-전략)
14. [전체 구현 계획](#14-전체-구현-계획)
15. [단계별 구현 계획](#15-단계별-구현-계획)

---

## 1. 프로젝트 개요

### 핵심 기능

| 기능 | 설명 |
|------|------|
| 실시간 좌석 조회 | SSE 기반 좌석 상태 변경 실시간 Push |
| 분산 좌석 선점 | Redis Redisson 분산락으로 동시 선점 충돌 방지 |
| 예약 타임아웃 | Redis TTL 5분 — 미결제 시 좌석 자동 회수 |
| 대기열 | Redis Sorted Set 기반 오픈런 트래픽 제어 |
| 비동기 결제 | RabbitMQ Dead Letter Queue 기반 결제 이벤트 처리 |
| 목업 결제 | PG 연동 없이 결제 플로우 시뮬레이션 |
| 공연 관리 | 관리자 공연·좌석 등록, 판매 현황 조회 |

### 포트폴리오 기술 포인트

- **MSA** — 서비스 독립 배포, Eureka 디스커버리, Spring Cloud Gateway
- **멀티모듈 Gradle** — common/events/security 공유 라이브러리 분리
- **헥사고날 아키텍처** — 도메인이 인프라를 모름 (Ports & Adapters)
- **분산락** — Redisson RLock으로 좌석 선점 Race Condition 방지
- **Redis TTL** — 임시 예약 자동 만료, Spring Scheduler 보조 회수
- **RabbitMQ DLQ** — 결제 실패 재시도 3회 후 Dead Letter 처리
- **Circuit Breaker** — Resilience4j로 서비스 장애 전파 차단
- **분산 추적** — Micrometer + Zipkin으로 TraceId 전파
- **Jenkins CI/CD** — Multibranch Pipeline, 변경 서비스만 빌드

---

## 2. 기술 스택

### Backend

| 항목 | 기술 | 버전 |
|------|------|------|
| Language | Java | 21 |
| Framework | Spring Boot | 3.3 |
| Build | Gradle (Multi-Module) | 8.x |
| ORM | Spring Data JPA + QueryDSL | 5.x |
| DB Migration | Flyway | - |
| Cache / Lock | Redis 7 + Redisson | - |
| Message Queue | RabbitMQ | 3.13 |
| Service Discovery | Spring Cloud Eureka | - |
| API Gateway | Spring Cloud Gateway | - |
| Config | Spring Cloud Config | - |
| Auth | JWT (JJWT 0.12) | - |
| Circuit Breaker | Resilience4j | - |
| Tracing | Micrometer Tracing + Zipkin | - |
| Docs | SpringDoc OpenAPI 3 | - |
| Test | JUnit 5 + Mockito + Testcontainers | - |

### Frontend

| 항목 | 기술 |
|------|------|
| Framework | React 18 + TypeScript |
| UI | Tailwind CSS + shadcn/ui |
| 상태관리 | Zustand (클라이언트) + React Query (서버) |
| 실시간 | SSE (EventSource API) |
| HTTP | Axios |

### Infrastructure

| 항목 | 기술 |
|------|------|
| 컨테이너 | Docker Compose |
| CI/CD | Jenkins (Multibranch Pipeline) |
| 코드 품질 | SonarQube (커버리지 60% 기준) |
| 모니터링 | Zipkin (분산 추적) |

---

## 3. 전체 아키텍처

```
[React Client]
      │  REST / SSE
      ▼
┌─────────────────────────────────────────────┐
│            API Gateway  :8080               │
│   JWT 검증 필터 · Rate Limiter · 라우팅      │
└──┬────────┬────────┬────────┬───────────────┘
   │        │        │        │
   ▼        ▼        ▼        ▼
[User    [Event   [Reserv.  [Payment
 :8081]   :8082]   :8083]    :8084]
   │        │        │        │
   ▼        ▼        ▼        ▼
[MySQL]  [MySQL]  [MySQL]  [MySQL]
              [Redis :6379] ◄─┘
              └── 분산락, TTL, 대기열

                   │  RabbitMQ :5672
                   ▼
          payment.exchange
            ├── payment.completed ──► Reservation (확정)
            │                    ──► Notification (메일)
            ├── payment.failed   ──► Reservation (회수)
            │                    ──► Notification (알림)
            └── payment.dlq      ──► Dead Letter (3회 실패)

[Notification :8085] ──► MongoDB (발송 이력)

[Eureka Server :8761]  ← 전 서비스 등록
[Config Server :8888]  ← 환경별 설정 중앙화
[Zipkin        :9411]  ← 분산 추적 대시보드
[Jenkins       :8090]  ← CI/CD
[SonarQube     :9000]  ← 코드 품질
```

### 서비스 간 통신

| 방식 | 대상 | 용도 |
|------|------|------|
| Feign (동기) | Reservation → Event | 좌석 유효성 검증 |
| Feign (동기) | Payment → Reservation | 예약 상태 검증 |
| RabbitMQ (비동기) | Payment → Reservation | 결제 완료/실패 처리 |
| RabbitMQ (비동기) | Payment → Notification | 확정/실패 알림 발송 |
| RabbitMQ (비동기) | Reservation → Event | 좌석 상태 변경 |

---

## 4. 멀티모듈 구조

```
tic-ketch/                              (root project)
├── settings.gradle
├── build.gradle                        (공통 의존성 정의)
├── docker-compose.yml
│
├── common/                             ← 공유 라이브러리
│   ├── ticketch-common/                API 응답, 예외, 유틸
│   ├── ticketch-events/                MQ 이벤트 DTO
│   └── ticketch-security/             JWT 파싱 유틸
│
├── infrastructure/                     ← 인프라 서비스
│   ├── eureka-server/
│   ├── config-server/
│   └── api-gateway/
│
└── services/                           ← 비즈니스 마이크로서비스
    ├── user-service/
    ├── event-service/
    ├── reservation-service/
    ├── payment-service/
    └── notification-service/
```

### settings.gradle

```groovy
rootProject.name = 'tic-ketch'

// Common
include ':common:ticketch-common'
include ':common:ticketch-events'
include ':common:ticketch-security'

// Infrastructure
include ':infrastructure:eureka-server'
include ':infrastructure:config-server'
include ':infrastructure:api-gateway'

// Services
include ':services:user-service'
include ':services:event-service'
include ':services:reservation-service'
include ':services:payment-service'
include ':services:notification-service'
```

### 모듈 의존 관계

```
ticketch-common  ◄── ticketch-events
ticketch-common  ◄── ticketch-security
ticketch-common  ◄── [모든 서비스]
ticketch-events  ◄── reservation-service (consumer)
ticketch-events  ◄── payment-service     (producer)
ticketch-events  ◄── notification-service(consumer)
ticketch-events  ◄── event-service       (consumer)
ticketch-security◄── api-gateway
ticketch-security◄── user-service        (발급)
ticketch-security◄── [JWT 검증 필요 서비스]
```

---

## 5. Common 모듈 설계

### ticketch-common

> 프레임워크 의존성 없이 모든 서비스가 공유하는 순수 Java 모듈

```
ticketch-common/src/main/java/com/ticketch/common/
├── response/
│   ├── ApiResponse.java         { code, message, data }
│   └── ErrorResponse.java       { code, message, errors[] }
├── exception/
│   ├── BusinessException.java   RuntimeException + ErrorCode
│   └── ErrorCode.java           Enum (USER_NOT_FOUND, SEAT_ALREADY_HELD, ...)
└── util/
    └── PageUtil.java
```

**ErrorCode 예시**

```java
public enum ErrorCode {
    // User
    USER_NOT_FOUND(404, "U001", "사용자를 찾을 수 없습니다"),
    DUPLICATE_EMAIL(409, "U002", "이미 사용 중인 이메일입니다"),
    INVALID_PASSWORD(401, "U003", "비밀번호가 올바르지 않습니다"),

    // Seat / Reservation
    SEAT_NOT_FOUND(404, "S001", "좌석을 찾을 수 없습니다"),
    SEAT_ALREADY_HELD(409, "S002", "이미 선점된 좌석입니다"),
    RESERVATION_EXPIRED(410, "R001", "예약 시간이 만료되었습니다"),
    RESERVATION_NOT_FOUND(404, "R002", "예약을 찾을 수 없습니다"),

    // Payment
    PAYMENT_NOT_FOUND(404, "P001", "결제 정보를 찾을 수 없습니다"),
    PAYMENT_ALREADY_COMPLETED(409, "P002", "이미 완료된 결제입니다"),

    // Common
    INTERNAL_SERVER_ERROR(500, "C001", "서버 오류가 발생했습니다"),
    INVALID_INPUT(400, "C002", "잘못된 입력값입니다");
}
```

---

### ticketch-events

> RabbitMQ Producer ↔ Consumer 간 공유 이벤트 DTO

```
ticketch-events/src/main/java/com/ticketch/events/
├── payment/
│   ├── PaymentCompletedEvent.java   { paymentId, reservationId, userId, amount }
│   └── PaymentFailedEvent.java      { paymentId, reservationId, userId, reason }
├── reservation/
│   ├── SeatHeldEvent.java           { reservationId, seatId, eventId, userId }
│   └── SeatReleasedEvent.java       { reservationId, seatId, eventId, reason }
└── notification/
    └── NotificationRequestEvent.java { userId, type, title, body }
```

---

### ticketch-security

> JWT 발급/파싱/검증 유틸 — User Service(발급), API Gateway(검증), 기타 서비스(파싱)

```
ticketch-security/src/main/java/com/ticketch/security/
├── JwtTokenProvider.java     토큰 생성/검증/파싱
├── JwtProperties.java        secret, accessExpiry, refreshExpiry (Config Server에서 주입)
└── UserPrincipal.java        { userId, email, role } — 토큰에서 추출한 인증 객체
```

---

## 6. 헥사고날 아키텍처

### 원칙

```
[외부 세계]          [어댑터]           [애플리케이션]      [도메인]
  HTTP Request  →  WebAdapter     →  InputPort(UseCase) → Domain Model
  MQ Message    →  MQConsumer     →  InputPort(UseCase) → Domain Service
                                  ←  OutputPort(인터페이스)
                   JpaAdapter    ←── implements LoadXxxPort
                   RedisAdapter  ←── implements CacheXxxPort
                   MQPublisher   ←── implements PublishXxxPort
```

**핵심 규칙**
- `domain` 패키지는 Spring · JPA · Redis 의존성 없음 (순수 Java)
- `application/port/out` 은 인터페이스만 — 구현체는 `adapter/out` 에
- `adapter/in/web` Controller는 UseCase 인터페이스만 호출
- `adapter/out/persistence` Entity는 Domain Model과 별도로 매핑

### 각 서비스 공통 패키지 구조

```
com.ticketch.{service}/
│
├── domain/
│   ├── model/                  핵심 도메인 객체 (순수 Java, 프레임워크 의존 없음)
│   └── service/                도메인 서비스 (비즈니스 규칙)
│
├── application/
│   ├── port/
│   │   ├── in/                 Input Port — UseCase 인터페이스
│   │   └── out/                Output Port — Repository/MQ/Cache 인터페이스
│   └── service/                UseCase 구현체 (Input Port implements)
│
└── adapter/
    ├── in/
    │   ├── web/                REST Controller + Request/Response DTO
    │   └── messaging/          RabbitMQ Consumer (MQ → UseCase 호출)
    └── out/
        ├── persistence/        JPA Entity + Repository + PersistenceAdapter
        ├── messaging/          RabbitMQ Publisher (Output Port 구현)
        ├── redis/              Redis Adapter (Output Port 구현)
        └── external/           Feign Client (외부 서비스 호출)
```

---

## 7. 서비스별 상세 설계

### User Service (:8081)

**역할**: 회원가입, 로그인, JWT 발급, 토큰 갱신/무효화

```
com.ticketch.userservice/
├── domain/
│   ├── model/
│   │   ├── User.java                 { id, email, encodedPassword, name, role }
│   │   └── RefreshToken.java         { tokenHash, userId, expiresAt }
│   └── service/
│       └── PasswordService.java      비밀번호 인코딩/검증 규칙
│
├── application/
│   ├── port/in/
│   │   ├── RegisterUserUseCase.java
│   │   ├── LoginUseCase.java
│   │   ├── ReissueTokenUseCase.java
│   │   └── LogoutUseCase.java
│   ├── port/out/
│   │   ├── LoadUserPort.java         findByEmail, findById
│   │   ├── SaveUserPort.java
│   │   ├── SaveRefreshTokenPort.java
│   │   └── BlacklistTokenPort.java   Redis 블랙리스트
│   └── service/
│       ├── RegisterUserService.java
│       └── AuthService.java          Login/Reissue/Logout
│
└── adapter/
    ├── in/web/
    │   ├── AuthController.java
    │   └── dto/                      RegisterRequest, LoginRequest, TokenResponse
    └── out/
        ├── persistence/
        │   ├── UserJpaEntity.java
        │   ├── UserJpaRepository.java
        │   └── UserPersistenceAdapter.java  implements LoadUserPort, SaveUserPort
        └── redis/
            └── TokenRedisAdapter.java       implements BlacklistTokenPort, SaveRefreshTokenPort
```

**테스트**: 단위 테스트
- `AuthService` — Mockito로 Port 목킹, JWT 발급/검증 로직 검증
- `PasswordService` — 순수 로직, 프레임워크 불필요

---

### Event Service (:8082)

**역할**: 공연·좌석 CRUD, 실시간 좌석 상태 SSE, 좌석 캐시

```
com.ticketch.eventservice/
├── domain/
│   ├── model/
│   │   ├── Event.java                { id, title, venue, eventDate, status }
│   │   ├── SeatGrade.java            { id, gradeName, price, colorCode }
│   │   └── Seat.java                 { id, eventId, row, number, status: AVAILABLE/HELD/SOLD }
│   └── service/
│       └── SeatStatusService.java    좌석 상태 전이 규칙
│
├── application/
│   ├── port/in/
│   │   ├── GetEventUseCase.java
│   │   ├── GetSeatsUseCase.java
│   │   ├── UpdateSeatStatusUseCase.java
│   │   └── CreateEventUseCase.java   (ADMIN)
│   ├── port/out/
│   │   ├── LoadEventPort.java
│   │   ├── LoadSeatPort.java
│   │   ├── SaveEventPort.java
│   │   ├── UpdateSeatStatusPort.java
│   │   ├── CacheSeatStatusPort.java  Redis 캐시
│   │   └── EmitSeatEventPort.java    SSE 발행
│   └── service/
│       ├── EventQueryService.java
│       └── SeatManagementService.java
│
└── adapter/
    ├── in/
    │   ├── web/
    │   │   ├── EventController.java
    │   │   ├── SeatController.java
    │   │   └── SeatSseController.java  SSE endpoint
    │   └── messaging/
    │       └── SeatReleaseConsumer.java  seat.release MQ → UpdateSeatStatusUseCase
    └── out/
        ├── persistence/              JPA Entity + Repository + Adapter
        ├── redis/
        │   └── SeatCacheRedisAdapter.java  implements CacheSeatStatusPort
        └── sse/
            └── SseEmitterAdapter.java      implements EmitSeatEventPort
```

**테스트**: 통합 테스트 (Testcontainers)
- Redis 캐시 + SSE Emitter + JPA 쿼리 — 실제 인프라 필요
- 좌석 상태 변경 → SSE 발행 → 캐시 갱신 흐름 검증

---

### Reservation Service (:8083)

**역할**: 좌석 임시 선점(분산락), TTL 관리, 대기열, 예약 확정/취소

```
com.ticketch.reservationservice/
├── domain/
│   ├── model/
│   │   ├── Reservation.java          { id, userId, seatId, eventId, status, expiresAt }
│   │   └── QueueEntry.java           { userId, eventId, score(timestamp) }
│   └── service/
│       └── ReservationDomainService.java  만료 여부 판단, 상태 전이 규칙
│
├── application/
│   ├── port/in/
│   │   ├── HoldSeatUseCase.java       좌석 임시 선점
│   │   ├── ConfirmReservationUseCase.java  결제 완료 후 확정
│   │   ├── CancelReservationUseCase.java
│   │   ├── GetReservationUseCase.java
│   │   └── QueueUseCase.java          대기열 진입/조회
│   ├── port/out/
│   │   ├── LoadReservationPort.java
│   │   ├── SaveReservationPort.java
│   │   ├── AcquireSeatLockPort.java   Redisson 분산락
│   │   ├── HoldSeatCachePort.java     Redis TTL
│   │   ├── ReleaseSeatCachePort.java
│   │   ├── ManageQueuePort.java       Sorted Set
│   │   ├── ValidateSeatPort.java      Feign → Event Service
│   │   └── PublishSeatReleasedPort.java  MQ 발행
│   └── service/
│       ├── HoldSeatService.java       핵심: 락 획득 → TTL 저장 → DB 저장
│       ├── ReservationConfirmService.java
│       └── QueueService.java
│
└── adapter/
    ├── in/
    │   ├── web/
    │   │   ├── ReservationController.java
    │   │   └── QueueController.java
    │   └── messaging/
    │       └── PaymentEventConsumer.java  payment.completed/failed 수신
    └── out/
        ├── persistence/
        ├── redis/
        │   ├── RedissonLockAdapter.java   implements AcquireSeatLockPort
        │   └── ReservationCacheAdapter.java  implements HoldSeatCachePort
        ├── messaging/
        │   └── SeatReleasedPublisher.java implements PublishSeatReleasedPort
        ├── external/
        │   └── EventServiceClient.java   Feign implements ValidateSeatPort
        └── scheduler/
            └── ExpiredReservationScheduler.java  5분 만료 보조 배치
```

**HoldSeat 핵심 플로우**
```
1. ValidateSeatPort → Event Service (좌석 존재·AVAILABLE 확인)
2. AcquireSeatLockPort → Redisson RLock (timeout 3s)
3. DB 조회 — 이미 HELD/SOLD이면 예외
4. Reservation 저장 (status=PENDING)
5. HoldSeatCachePort → Redis Hash, TTL=300s
6. LockRelease
```

**테스트**: 통합 테스트 (Testcontainers)
- Redis + MySQL + Redisson 분산락 — 실제 인프라 필수
- 동시 선점 시나리오 (멀티스레드) 검증

---

### Payment Service (:8084)

**역할**: 결제 요청(목업), 결제 이벤트 MQ 발행, 환불

```
com.ticketch.paymentservice/
├── domain/
│   ├── model/
│   │   └── Payment.java              { id, reservationId, userId, amount, status }
│   └── service/
│       └── MockPaymentProcessor.java  목업 결제 처리 (랜덤 성공/실패 시뮬레이션)
│
├── application/
│   ├── port/in/
│   │   ├── RequestPaymentUseCase.java
│   │   ├── CancelPaymentUseCase.java
│   │   └── GetPaymentUseCase.java
│   ├── port/out/
│   │   ├── LoadPaymentPort.java
│   │   ├── SavePaymentPort.java
│   │   ├── ProcessPaymentPort.java    목업 PG 호출
│   │   ├── ValidateReservationPort.java  Feign → Reservation Service
│   │   └── PublishPaymentEventPort.java  MQ 발행
│   └── service/
│       └── PaymentService.java
│
└── adapter/
    ├── in/web/
    │   ├── PaymentController.java
    │   └── dto/
    └── out/
        ├── persistence/
        ├── mock/
        │   └── MockPgAdapter.java       implements ProcessPaymentPort
        ├── external/
        │   └── ReservationServiceClient.java  Feign
        └── messaging/
            └── PaymentEventPublisher.java     implements PublishPaymentEventPort
```

**목업 결제 동작**
```
requestPayment() 호출
  → 90% 확률 성공 → PaymentCompletedEvent 발행
  → 10% 확률 실패 → PaymentFailedEvent 발행
```

**테스트**: 단위 테스트
- `PaymentService` — MockPgAdapter, PublishPaymentEventPort Mockito 목킹
- 결제 성공/실패 분기 로직 검증

---

### Notification Service (:8085)

**역할**: MQ 이벤트 수신 후 이메일/알림 발송, 발송 이력 저장

```
com.ticketch.notificationservice/
├── domain/
│   ├── model/
│   │   └── Notification.java         { id, userId, type, title, body, status, sentAt }
│   └── service/
│       └── NotificationFactory.java  이벤트 타입 → 알림 내용 생성
│
├── application/
│   ├── port/in/
│   │   └── SendNotificationUseCase.java
│   ├── port/out/
│   │   ├── SaveNotificationPort.java
│   │   └── SendEmailPort.java
│   └── service/
│       └── NotificationService.java
│
└── adapter/
    ├── in/messaging/
    │   └── PaymentNotificationConsumer.java  MQ → SendNotificationUseCase
    └── out/
        ├── persistence/
        │   └── NotificationMongoAdapter.java  implements SaveNotificationPort
        └── email/
            └── MockEmailAdapter.java          implements SendEmailPort (콘솔 출력)
```

**테스트**: 단위 테스트
- `NotificationService` — MockEmailAdapter Mockito 목킹
- 이벤트 타입별 알림 내용 생성 로직 검증

---

### API Gateway (:8080)

```
com.ticketch.gateway/
├── filter/
│   ├── JwtAuthenticationFilter.java  토큰 검증 → userId Header 주입
│   └── RateLimitFilter.java          Redis 기반 IP Rate Limit
├── config/
│   ├── RouteConfig.java              서비스별 라우팅 규칙
│   └── CorsConfig.java
└── exception/
    └── GatewayExceptionHandler.java
```

**라우팅 규칙**

```yaml
routes:
  - id: user-service
    uri: lb://user-service
    predicates:
      - Path=/api/auth/**, /api/users/**
    filters:
      - name: RequestRateLimiter  # 로그인 엔드포인트 Rate Limit

  - id: event-service
    uri: lb://event-service
    predicates:
      - Path=/api/events/**

  - id: reservation-service
    uri: lb://reservation-service
    predicates:
      - Path=/api/reservations/**, /api/queue/**
    filters:
      - JwtAuthenticationFilter  # 인증 필요

  - id: payment-service
    uri: lb://payment-service
    predicates:
      - Path=/api/payments/**
    filters:
      - JwtAuthenticationFilter
```

---

## 8. 데이터 모델

### User Service — MySQL

```sql
CREATE TABLE users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    role        ENUM('USER', 'ADMIN') DEFAULT 'USER',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE refresh_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    token_hash  VARCHAR(512) NOT NULL,
    expires_at  DATETIME NOT NULL,
    is_revoked  BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### Event Service — MySQL

```sql
CREATE TABLE events (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    venue       VARCHAR(255) NOT NULL,
    event_date  DATETIME NOT NULL,
    status      ENUM('UPCOMING', 'ON_SALE', 'SOLD_OUT', 'ENDED') DEFAULT 'UPCOMING',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE seat_grades (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id    BIGINT NOT NULL,
    grade_name  VARCHAR(50) NOT NULL,
    price       INT NOT NULL,
    color_code  VARCHAR(10),
    FOREIGN KEY (event_id) REFERENCES events(id)
);

CREATE TABLE seats (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id      BIGINT NOT NULL,
    seat_grade_id BIGINT NOT NULL,
    row_name      VARCHAR(10) NOT NULL,
    seat_number   INT NOT NULL,
    status        ENUM('AVAILABLE', 'HELD', 'SOLD') DEFAULT 'AVAILABLE',
    version       BIGINT DEFAULT 0,          -- Optimistic Lock 보조
    FOREIGN KEY (event_id) REFERENCES events(id),
    FOREIGN KEY (seat_grade_id) REFERENCES seat_grades(id)
);
```

### Reservation Service — MySQL

```sql
CREATE TABLE reservations (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    seat_id      BIGINT NOT NULL,
    event_id     BIGINT NOT NULL,
    status       ENUM('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED') DEFAULT 'PENDING',
    expires_at   DATETIME NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### Payment Service — MySQL

```sql
CREATE TABLE payments (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id     BIGINT NOT NULL UNIQUE,
    user_id            BIGINT NOT NULL,
    amount             INT NOT NULL,
    status             ENUM('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED') DEFAULT 'PENDING',
    failure_reason     VARCHAR(500),
    paid_at            DATETIME,
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Notification Service — MongoDB

```json
{
  "_id": "ObjectId",
  "userId": "Long",
  "type": "PAYMENT_COMPLETED | PAYMENT_FAILED | RESERVATION_EXPIRED",
  "title": "String",
  "body": "String",
  "status": "SENT | FAILED",
  "sentAt": "ISODate"
}
```

---

## 9. Redis 설계

| Key 패턴 | 타입 | TTL | 용도 |
|----------|------|-----|------|
| `seat:lock:{seatId}` | String | 30s | Redisson 분산락 |
| `reservation:temp:{reservationId}` | Hash | 300s | 임시 예약 정보 |
| `event:seats:{eventId}` | Hash | 60s | 좌석 상태 캐시 |
| `queue:{eventId}` | Sorted Set | - | 대기열 (score=timestamp) |
| `queue:size:{eventId}` | String | - | 대기열 길이 캐시 |
| `blacklist:jwt:{jti}` | String | AccessToken 만료 시간 | 로그아웃 토큰 |
| `ratelimit:{ip}` | String | 1s | Gateway Rate Limit |

---

## 10. RabbitMQ 설계

### Exchange / Queue / Binding

```
payment.exchange (topic)
  ├── routing_key: payment.completed
  │     └── payment.completed.queue
  │           ├── consumer: Reservation Service (예약 확정)
  │           └── consumer: Notification Service (확정 메일)
  ├── routing_key: payment.failed
  │     └── payment.failed.queue
  │           ├── consumer: Reservation Service (좌석 회수)
  │           └── consumer: Notification Service (실패 알림)
  └── DLX 설정
        └── payment.dlq  (3회 retry 후 Dead Letter)

seat.exchange (topic)
  └── routing_key: seat.released
        └── seat.released.queue
              └── consumer: Event Service (좌석 상태 AVAILABLE 변경 + SSE 발행)
```

### Dead Letter Queue 설정

```java
// payment.queue 설정
args.put("x-dead-letter-exchange", "payment.dlx");
args.put("x-message-ttl", 10000);      // 10초마다 재시도
args.put("x-max-retries", 3);
```

---

## 11. API 설계

### Auth (User Service)

```
POST  /api/auth/register     회원가입
POST  /api/auth/login        로그인 → { accessToken, refreshToken }
POST  /api/auth/refresh      AccessToken 재발급
POST  /api/auth/logout       로그아웃 (블랙리스트 등록)
GET   /api/users/me          내 프로필 (인증 필요)
```

### Events (Event Service)

```
GET   /api/events                  공연 목록 (페이징, status 필터)
GET   /api/events/{id}             공연 상세
GET   /api/events/{id}/seats       좌석 현황 (Redis 캐시)
GET   /api/events/{id}/seats/stream  SSE — 실시간 좌석 상태
POST  /api/admin/events            공연 등록 (ADMIN)
PUT   /api/admin/events/{id}       공연 수정 (ADMIN)
POST  /api/admin/events/{id}/seats 좌석 일괄 생성 (ADMIN)
GET   /api/admin/events/{id}/stats 판매 통계 (ADMIN)
```

### Reservations (Reservation Service)

```
POST   /api/reservations           좌석 임시 선점 (인증 필요)
GET    /api/reservations/{id}      예약 상세 + 남은 시간
DELETE /api/reservations/{id}      선점 취소
GET    /api/reservations/me        내 예약 목록
GET    /api/queue/{eventId}        대기열 순번 조회
POST   /api/queue/{eventId}/enter  대기열 진입
```

### Payments (Payment Service)

```
POST  /api/payments              결제 요청 (목업)
GET   /api/payments/{id}         결제 상태 조회
POST  /api/payments/{id}/cancel  결제 취소/환불
```

### 공통 응답 형식

```json
// 성공
{
  "code": "SUCCESS",
  "message": "ok",
  "data": { ... }
}

// 실패
{
  "code": "S002",
  "message": "이미 선점된 좌석입니다",
  "errors": []
}
```

---

## 12. CI/CD — Jenkins

### 브랜치 전략

| 브랜치 | 트리거 | 파이프라인 |
|--------|--------|-----------|
| `feature/**` | PR open/push | Build + Test + SonarQube |
| `develop` | push | Build + Test + Docker Build + Staging 배포 |
| `main` | push (tag) | Build + Test + Docker Push + Prod 배포 (수동 승인) |

### Multibranch Pipeline — 변경 서비스만 빌드

```groovy
// Jenkinsfile.root
pipeline {
    agent any
    stages {
        stage('Detect Changes') {
            steps {
                script {
                    def services = ['user-service', 'event-service',
                                    'reservation-service', 'payment-service',
                                    'notification-service']
                    def changed = sh(
                        script: "git diff --name-only HEAD~1 HEAD | cut -d/ -f1-2 | sort -u",
                        returnStdout: true
                    ).trim().split('\n')
                    env.CHANGED_SERVICES = services
                        .findAll { svc -> changed.any { it.contains(svc) } }
                        .join(',')
                }
            }
        }
        stage('Build Changed Services') {
            steps {
                script {
                    parallel env.CHANGED_SERVICES.split(',').collectEntries { svc ->
                        [svc, { build job: "tic-ketch/${svc}",
                                      parameters: [string(name: 'BRANCH', value: env.BRANCH_NAME)] }]
                    }
                }
            }
        }
    }
}
```

### 서비스 Jenkinsfile (공통 구조)

```groovy
pipeline {
    agent any
    environment {
        SERVICE_NAME = 'user-service'
        IMAGE_NAME   = "your-dockerhub/${SERVICE_NAME}"
        IMAGE_TAG    = "${env.BUILD_NUMBER}-${env.GIT_COMMIT[0..6]}"
    }
    stages {
        stage('Build & Test')        { /* Gradle test */ }
        stage('Integration Test')    { when { branch 'develop' } /* Testcontainers */ }
        stage('SonarQube')           { /* Quality Gate 60% */ }
        stage('Docker Build & Push') { when { anyOf { branch 'develop'; branch 'main' } } }
        stage('Deploy Staging')      { when { branch 'develop' } }
        stage('Deploy Production')   { when { branch 'main' }; input '승인' }
    }
    post {
        success { slackSend color: 'good', message: "✅ ${SERVICE_NAME} #${BUILD_NUMBER} 성공" }
        failure { slackSend color: 'danger', message: "❌ ${SERVICE_NAME} #${BUILD_NUMBER} 실패 ${BUILD_URL}" }
    }
}
```

### Jenkins 필수 플러그인

- Pipeline, Multibranch Pipeline, GitHub Branch Source
- Docker Pipeline, SonarQube Scanner, JaCoCo
- Slack Notification, Blue Ocean, Credentials Binding

### Jenkins Credentials

| ID | 용도 |
|----|------|
| `github-token` | GitHub Webhook 연동 |
| `dockerhub-credentials` | Docker Hub 푸시 |
| `sonarqube-token` | SonarQube 분석 |
| `slack-token` | 빌드 결과 알림 |
| `staging-server-ssh` | 스테이징 배포 SSH |

---

## 13. 테스트 전략

| 서비스 | 전략 | 근거 |
|--------|------|------|
| User Service | **단위 테스트** | JWT 발급/검증, 비밀번호 인코딩은 순수 로직 — 인프라 불필요 |
| Event Service | **통합 테스트** | Redis 캐시 + JPA 복잡 쿼리 + SSE Emitter가 인프라와 강결합 |
| Reservation Service | **통합 테스트** | 분산락 + Redis TTL + DB 트랜잭션이 핵심 — 실제 인프라로만 검증 가능 |
| Payment Service | **단위 테스트** | 목업 PG, MQ Publisher MockBean으로 충분 |
| Notification Service | **단위 테스트** | MQ Consumer 로직 + 이메일 Mock으로 충분 |

### 통합 테스트 환경 (Testcontainers)

```java
@SpringBootTest
@Testcontainers
class HoldSeatServiceIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8");

    @Container
    static GenericContainer<?> redis =
        new GenericContainer<>("redis:7").withExposedPorts(6379);
}
```

### SonarQube 품질 게이트

- 라인 커버리지 **60% 이상**
- 신규 코드 버그 **0**
- 보안 취약점 **0**

---

## 14. 전체 구현 계획

```
Phase 0: 인프라 기반 셋업          (1일)
Phase 1: Common 모듈               (1일)
Phase 2: User Service              (2일)
Phase 3: Event Service             (2일)
Phase 4: Reservation Service       (3일) ← 핵심 복잡도
Phase 5: Payment Service           (1일)
Phase 6: Notification Service      (1일)
Phase 7: API Gateway               (1일)
Phase 8: Jenkins CI/CD             (1일)
Phase 9: Frontend                  (3일)
Phase 10: 통합 테스트 & 정리       (1일)
─────────────────────────────────────────
총 예상                            약 17일
```

---

## 15. 단계별 구현 계획

---

### Phase 0: 인프라 기반 셋업

**목표**: 로컬 개발 환경 전체 구동

**작업 항목**
- [ ] `docker-compose.yml` 작성
  - MySQL×5 (서비스별 독립 DB), Redis, RabbitMQ
  - Zipkin, Eureka, Config Server
  - Jenkins, SonarQube
- [ ] `settings.gradle`, 루트 `build.gradle` 멀티모듈 설정
- [ ] `config-repo/` 생성 — 서비스별 `application.yml` 초안
- [ ] Eureka Server 구현 (`@EnableEurekaServer`)
- [ ] Config Server 구현 (`@EnableConfigServer`, Git 연동)

**완료 기준**: `docker-compose up` 후 Eureka 대시보드 접근 가능

---

### Phase 1: Common 모듈

**목표**: 모든 서비스가 공유할 라이브러리 구축

**작업 항목**
- [ ] `ticketch-common`
  - `ApiResponse<T>`, `ErrorResponse`
  - `BusinessException`, `ErrorCode` Enum (전체 에러코드 정의)
  - `GlobalExceptionHandler` (Spring MVC용 공통 제공)
- [ ] `ticketch-events`
  - `PaymentCompletedEvent`, `PaymentFailedEvent`
  - `SeatHeldEvent`, `SeatReleasedEvent`
  - `NotificationRequestEvent`
- [ ] `ticketch-security`
  - `JwtTokenProvider` (생성/파싱/검증)
  - `JwtProperties` (Config Server에서 secret 주입)
  - `UserPrincipal`

**완료 기준**: 각 모듈 `./gradlew :common:ticketch-common:build` 성공

---

### Phase 2: User Service

**목표**: 회원가입, 로그인, JWT 발급

**작업 항목**
- [ ] 헥사고날 패키지 구조 셋업
- [ ] Domain: `User`, `RefreshToken` 모델
- [ ] Application:
  - `RegisterUserService` (이메일 중복 검사, 비밀번호 인코딩)
  - `AuthService` (로그인 → JWT 발급, 갱신, 로그아웃)
- [ ] Adapter:
  - `UserController`, `AuthController`
  - `UserPersistenceAdapter` (JPA)
  - `TokenRedisAdapter` (RefreshToken 저장, 블랙리스트)
- [ ] Flyway 마이그레이션 SQL
- [ ] 단위 테스트 작성
- [ ] Swagger 문서 확인

**완료 기준**: 로그인 → AccessToken 반환 → `/api/users/me` 호출 성공

---

### Phase 3: Event Service

**목표**: 공연 조회, 실시간 좌석 현황 SSE

**작업 항목**
- [ ] Domain: `Event`, `SeatGrade`, `Seat`
- [ ] Application:
  - `EventQueryService` (목록, 상세)
  - `SeatManagementService` (상태 변경, 캐시 갱신)
- [ ] Adapter:
  - `EventController`, `SeatController`
  - `SeatSseController` — SSE endpoint
  - `SseEmitterAdapter` — Emitter 풀 관리
  - `SeatCacheRedisAdapter` — 좌석 상태 Redis 캐시
  - `SeatReleaseConsumer` — MQ Consumer
- [ ] Flyway 마이그레이션 SQL
- [ ] 통합 테스트 (Testcontainers: MySQL + Redis)

**완료 기준**: 좌석 상태 변경 → SSE 클라이언트 실시간 수신 확인

---

### Phase 4: Reservation Service

**목표**: 분산 좌석 선점, TTL 자동 회수, 대기열

**작업 항목**
- [ ] Redisson Config (`RedissonClient` Bean 설정)
- [ ] Domain: `Reservation`, `QueueEntry`, `ReservationDomainService`
- [ ] Application:
  - `HoldSeatService` (분산락 → TTL → DB 저장)
  - `ReservationConfirmService` (MQ 수신 후 확정)
  - `QueueService` (Sorted Set 진입/조회/팝)
- [ ] Adapter:
  - `ReservationController`, `QueueController`
  - `RedissonLockAdapter` — 분산락
  - `ReservationCacheAdapter` — Redis TTL
  - `PaymentEventConsumer` — payment.completed/failed 수신
  - `SeatReleasedPublisher` — MQ 발행
  - `EventServiceClient` — Feign (좌석 검증)
  - `ExpiredReservationScheduler` — 5분 만료 배치 (보조)
- [ ] Circuit Breaker 설정 (Feign → Event Service Fallback)
- [ ] 통합 테스트 (Testcontainers: MySQL + Redis, 동시 선점 시나리오)

**완료 기준**
- 동시에 동일 좌석 선점 요청 시 1건만 성공
- 5분 후 Redis TTL 만료 → Scheduler 회수 확인

---

### Phase 5: Payment Service

**목표**: 목업 결제, RabbitMQ 이벤트 발행

**작업 항목**
- [ ] RabbitMQ Config (Exchange, Queue, DLQ, Binding 선언)
- [ ] Domain: `Payment`, `MockPaymentProcessor`
- [ ] Application: `PaymentService` (요청 → 목업 처리 → 이벤트 발행)
- [ ] Adapter:
  - `PaymentController`
  - `MockPgAdapter` — 90% 성공 시뮬레이션
  - `PaymentEventPublisher` — completed/failed 발행
  - `ReservationServiceClient` — Feign (예약 검증)
- [ ] 단위 테스트

**완료 기준**: 결제 요청 → MQ 발행 → Reservation Service 수신 후 상태 변경

---

### Phase 6: Notification Service

**목표**: 결제 이벤트 수신 후 알림 발송

**작업 항목**
- [ ] Domain: `Notification`, `NotificationFactory`
- [ ] Application: `NotificationService`
- [ ] Adapter:
  - `PaymentNotificationConsumer` — MQ 수신
  - `MockEmailAdapter` — 콘솔 출력 (개발용)
  - `NotificationMongoAdapter` — 발송 이력 저장
- [ ] 단위 테스트

**완료 기준**: 결제 완료 이벤트 → 콘솔에 이메일 내용 출력 + MongoDB 저장

---

### Phase 7: API Gateway

**목표**: 라우팅, JWT 검증, Rate Limiting

**작업 항목**
- [ ] `RouteConfig` — 서비스별 라우팅 규칙
- [ ] `JwtAuthenticationFilter` — 토큰 검증 → `X-User-Id` Header 주입
- [ ] `RateLimitFilter` — Redis 기반 IP Rate Limit
- [ ] `GatewayExceptionHandler` — 인증 오류 응답 포맷
- [ ] CORS 설정

**완료 기준**: Gateway 통해 전체 플로우 end-to-end 동작

---

### Phase 8: Jenkins CI/CD

**목표**: GitHub 연동 자동 빌드/배포 파이프라인

**작업 항목**
- [ ] Jenkins Docker 컨테이너 구성 (Docker-in-Docker)
- [ ] 각 서비스 `Dockerfile` 작성 (multi-stage build)
- [ ] `jenkins/Jenkinsfile.root` — 변경 서비스 감지 + 병렬 트리거
- [ ] 각 서비스 `Jenkinsfile` — Build/Test/SonarQube/Docker/Deploy
- [ ] `deploy/docker-compose.staging.yml`, `deploy-staging.sh`
- [ ] Jenkins Credentials 등록 (GitHub, DockerHub, SonarQube, Slack)
- [ ] GitHub Webhook 설정
- [ ] SonarQube Quality Gate 설정 (커버리지 60%)

**완료 기준**: `develop` push → 자동 빌드 → Staging 배포 → Slack 알림

---

### Phase 9: Frontend

**목표**: 핵심 예매 플로우 UI 구현

**작업 항목**
- [ ] 프로젝트 셋업 (Vite + React + TypeScript + Tailwind)
- [ ] 인증: 로그인/회원가입 페이지, Axios 인터셉터 (AccessToken 자동 첨부)
- [ ] 공연 목록/상세 페이지
- [ ] 좌석 선택 페이지
  - SVG 좌석 배치도 (`SeatMap.tsx`)
  - SSE 연결 훅 (`useSeatSSE.ts`) — 실시간 좌석 상태 반영
- [ ] 결제 페이지
  - 5분 카운트다운 타이머 (`ReservationTimer.tsx`)
  - 타임아웃 시 자동 페이지 이동
- [ ] 대기열 페이지 (`QueueWaiting.tsx`) — 순번 Polling
- [ ] 내 예매 목록

**완료 기준**: 좌석 선택 → 임시 선점 → 결제 → 확정 완전 플로우 동작

---

### Phase 10: 통합 테스트 & 정리

**목표**: 전체 플로우 검증 및 문서화

**작업 항목**
- [ ] end-to-end 시나리오 수동 테스트
  - 동시 선점 → 1건만 성공
  - 5분 타임아웃 → 자동 회수 → 타 사용자 선점 가능
  - 결제 실패 → DLQ 재시도 → 최종 실패 → 좌석 회수
- [ ] Zipkin 대시보드 — 서비스 간 TraceId 추적 확인
- [ ] Swagger UI 각 서비스별 API 문서 확인
- [ ] README.md — 아키텍처 다이어그램, 실행 방법, 기술 설명
- [ ] 환경변수 정리 (`.env.example`)

---

## 전체 파일 구조

```
tic-ketch/
├── settings.gradle
├── build.gradle
├── docker-compose.yml
├── docker-compose.staging.yml
│
├── common/
│   ├── ticketch-common/
│   │   └── src/main/java/com/ticketch/common/
│   │       ├── response/ApiResponse.java
│   │       ├── response/ErrorResponse.java
│   │       ├── exception/BusinessException.java
│   │       └── exception/ErrorCode.java
│   ├── ticketch-events/
│   │   └── src/main/java/com/ticketch/events/
│   │       ├── payment/PaymentCompletedEvent.java
│   │       ├── payment/PaymentFailedEvent.java
│   │       ├── reservation/SeatReleasedEvent.java
│   │       └── notification/NotificationRequestEvent.java
│   └── ticketch-security/
│       └── src/main/java/com/ticketch/security/
│           ├── JwtTokenProvider.java
│           ├── JwtProperties.java
│           └── UserPrincipal.java
│
├── infrastructure/
│   ├── eureka-server/
│   ├── config-server/
│   ├── api-gateway/
│   └── config-repo/
│       ├── application.yml
│       ├── user-service.yml
│       ├── event-service.yml
│       ├── reservation-service.yml
│       ├── payment-service.yml
│       └── notification-service.yml
│
├── services/
│   ├── user-service/
│   │   ├── Dockerfile
│   │   ├── Jenkinsfile
│   │   └── src/main/java/com/ticketch/userservice/
│   │       ├── domain/model/
│   │       ├── application/port/in/
│   │       ├── application/port/out/
│   │       ├── application/service/
│   │       └── adapter/in/web/ + adapter/out/persistence/ + adapter/out/redis/
│   │
│   ├── event-service/       (동일 구조)
│   ├── reservation-service/ (동일 구조 + adapter/out/redis/ + scheduler/)
│   ├── payment-service/     (동일 구조 + adapter/out/mock/ + adapter/out/messaging/)
│   └── notification-service/(동일 구조 + adapter/in/messaging/ + adapter/out/email/)
│
├── jenkins/
│   ├── Jenkinsfile.root
│   └── shared-library/vars/
│       ├── buildService.groovy
│       ├── dockerBuild.groovy
│       └── deployService.groovy
│
├── deploy/
│   └── scripts/
│       ├── deploy-staging.sh
│       └── deploy-prod.sh
│
└── frontend/
    └── src/
        ├── pages/
        ├── components/
        ├── hooks/
        └── store/
```
