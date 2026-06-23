# TicKetch 로컬 실행 가이드

## 사전 요구
- Docker Desktop 실행 중 (Linux 컨테이너 모드)

## 전체 실행 (명령 한 줄)
인프라 + Spring MSA 8개를 모두 컨테이너로 빌드/기동한다. compose가 `depends_on` + healthcheck로 기동 순서를 잡는다.
```powershell
docker compose -f docker-compose.yml -f docker-compose.app.yml up -d --build
```
- 최초 실행은 이미지 빌드로 수 분 소요 (Gradle 캐시는 BuildKit 캐시 마운트로 공유)
- 상태 확인: `docker compose -f docker-compose.yml -f docker-compose.app.yml ps`
- 로그:     `docker compose -f docker-compose.yml -f docker-compose.app.yml logs -f api-gateway`
- 종료:     `docker compose -f docker-compose.yml -f docker-compose.app.yml down`
- 초기화:   `... down -v`  (볼륨까지 삭제 → DB 초기화)

> 컨테이너 내부는 `localhost` 대신 compose 서비스명으로 통신하므로 `SPRING_PROFILES_ACTIVE=docker`(config-repo의 `*-docker.yml`)가 적용된다.

## 접속 지점
| 대상 | URL |
|------|-----|
| **API Gateway** (프론트 호출 진입점) | http://localhost:8080 |
| Eureka 대시보드 (서비스 등록 확인) | http://localhost:8761 |
| RabbitMQ 관리 UI | http://localhost:15672 (admin/admin) |
| Zipkin 추적 | http://localhost:9411 |
| 서비스 직접 포트 | user 8081 · event 8082 · reservation 8083 · payment 8084 · notification 8085 |

모든 서비스가 Eureka에 등록되면 준비 완료 (최초 빌드 포함 수 분).

## 구성 파일
- `docker-compose.yml` — 인프라 (MySQL×4, MongoDB, Redis, RabbitMQ, Zipkin). Jenkins/SonarQube는 `--profile ci`로 별도
- `docker-compose.app.yml` — Spring 서비스 8개 (Eureka·Config·Gateway + 5개 비즈니스)
- `Dockerfile` — 모든 모듈이 공유하는 파라미터화 멀티스테이지 빌드
- `config-repo/` — Config Server가 제공하는 설정
  - `application.yml` / `application-docker.yml` — 공유(eureka·zipkin·jwt·redis·rabbitmq)
  - `{service}.yml` / `{service}-docker.yml` — 서비스별 포트·datasource (docker 프로파일은 컨테이너 주소)
- DB 스키마는 각 서비스 Flyway 마이그레이션이 기동 시 자동 적용

## 프론트엔드 (별도 저장소)
프론트는 `D:\git\tic-ketch-front` 에 있다.
```powershell
cd D:\git\tic-ketch-front
npm install   # 최초 1회
npm run dev   # http://localhost:5173 (Gateway :8080 호출)
```

## 트러블슈팅
- **서비스가 계속 재시작** → Config Server(:8888)/DB가 healthy 되기 전 기동되면 `restart: on-failure`로 자동 재시도된다. `docker compose ... logs -f <service>`로 확인
- **포트 충돌** → 8080~8085, 8761, 8888, 3307~3310, 6379, 5672, 27017, 9411 사용
- **이미지 재빌드** → 코드 변경 후 `... up -d --build`
