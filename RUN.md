# TicKetch 로컬 실행 가이드

## 사전 요구
- Docker Desktop 실행 중 (Linux 컨테이너 모드)
- JDK 21, 프로젝트 루트에서 `gradlew` 사용

## 한 번에 실행 (권장)
```powershell
.\run-all.ps1
```
순서대로 자동 기동한다:
1. **인프라** (docker compose) — MySQL×4, MongoDB, Redis, RabbitMQ, Zipkin
2. **Eureka** (:8761) → **Config Server** (:8888)
3. **서비스** — user(8081)·event(8082)·reservation(8083)·payment(8084)·notification(8085) + **Gateway(8080)**

각 Spring 서비스는 개별 PowerShell 창에서 로그를 보여준다. 모두 Eureka에 등록되면 준비 완료(1~2분).

## 접속 지점
| 대상 | URL |
|------|-----|
| **API Gateway** (프론트 호출 진입점) | http://localhost:8080 |
| Eureka 대시보드 | http://localhost:8761 |
| RabbitMQ 관리 UI | http://localhost:15672 (admin/admin) |
| Zipkin 추적 | http://localhost:9411 |

## 종료
```powershell
.\stop-all.ps1            # 인프라 중지 (데이터 보존)
.\stop-all.ps1 -Down      # 컨테이너 제거 (볼륨 보존)
.\stop-all.ps1 -Purge     # 볼륨까지 삭제 (DB 초기화)
```
Spring 서비스 창은 각 창에서 Ctrl+C 또는 창 닫기로 종료.

## 설정 구조
- 인프라 정의: `docker-compose.yml` (Jenkins/SonarQube는 `--profile ci`로 별도)
- 서비스 설정: `config-repo/` (Config Server가 native로 제공)
  - `application.yml` = 공유(eureka·zipkin·jwt·redis·rabbitmq)
  - `{service}.yml` = 서비스별 포트·datasource
- DB 스키마는 각 서비스의 Flyway 마이그레이션이 기동 시 자동 적용

## 프론트엔드
프론트는 별도 저장소 `D:\git\tic-ketch-front` 에 있다.
```powershell
cd D:\git\tic-ketch-front
npm install   # 최초 1회
npm run dev   # http://localhost:5173 (Gateway :8080 호출)
```

## 트러블슈팅
- **서비스가 DB 연결 실패** → 인프라 컨테이너가 healthy인지 확인 (`docker compose ps`)
- **JWT 오류** → Config Server(:8888)가 먼저 떠 있어야 jwt.secret 주입됨
- **포트 충돌** → 8080~8085, 8761, 8888, 3307~3310, 6379, 5672, 27017, 9411 사용
