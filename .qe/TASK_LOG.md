# Task Log

| 날짜 | Phase | 작업 | 상태 |
|------|-------|------|------|
| 2026-06-23 | Phase 4 | Reservation Service (분산락·TTL·대기열·결제이벤트) — UUID ee2a40d0 | ✅ 완료 — 통합테스트 통과 (20스레드 동시선점 → 1성공/19실패) |
| 2026-06-23 | Phase 5 | Payment Service (목업 결제·RabbitMQ 이벤트 발행·DLQ) — UUID 8ade8624 | ✅ 완료 — 단위테스트 3건 통과 (성공/실패/예약상태 분기) |
| 2026-06-23 | Phase 6 | Notification Service (MQ 이벤트 수신·알림 발송·MongoDB 이력) — UUID 4052a182 | ✅ 완료 — 단위테스트 4건 통과 (발송 SENT/FAILED + Factory) |
| 2026-06-23 | Phase 7 | API Gateway (리액티브·JWT 검증→X-User-Id 주입·Rate Limit·CORS) — UUID dc189371 | ✅ 완료 — JWT 필터 단위테스트 3건 통과 (주입/무토큰401/무효401) |
| 2026-06-23 | Phase 8 | Jenkins CI/CD | ⏸️ 보류 — 빌드 서버/배포 환경 미구축, 추후 진행 |
| 2026-06-23 | Phase 9 | Frontend (React/Vite/TS, 예매 플로우 UI, SSE 실시간) — UUID 216595fa | ✅ 완료 — **별도 저장소 D:\git\tic-ketch-front**, npm run build 통과 (24 파일) |

> 이전 Phase(0~3: 인프라/common/User/Event)는 git 이력 참조.
