# VERIFY_CHECKLIST — Phase 9: Frontend

- **UUID**: 216595fa
- **Type**: code
- **대상**: `frontend/`

---

## 빌드 / 구조
- [ ] `npm install` 성공 (frontend/)
- [ ] `npm run build` (`tsc && vite build`) 성공 — TypeScript 타입 에러 0
- [ ] 디렉터리 구조: `src/{types,api,store,hooks,components,pages}` 정리
- [ ] TypeScript strict 모드 활성 (tsconfig)

## 기능 — 인증 / API
- [ ] 로그인 성공 시 accessToken이 저장되고(Zustand+localStorage) 이후 요청에 `Authorization: Bearer` 자동 첨부
- [ ] axios 응답 인터셉터가 `{code,message,data}`를 언래핑하고 code≠SUCCESS 시 에러 처리
- [ ] 401 응답 시 로그아웃 + /login 이동
- [ ] ProtectedRoute가 미인증 사용자를 /login으로 보냄

## 기능 — 핵심 플로우
- [ ] 공연 목록/상세가 `/api/events`로 렌더됨 (페이징·상태 배지)
- [ ] 좌석 선택 페이지가 `SeatMap`(SVG)으로 좌석을 상태별 색으로 표시
- [ ] `useSeatSSE`가 EventSource로 연결되고 `seat-status` 수신 시 좌석을 갱신(invalidate)
- [ ] 좌석 선점(POST /reservations) 성공 시 결제 페이지로 이동
- [ ] 결제 페이지에 `ReservationTimer` 5분 카운트다운, 만료 시 자동 이동
- [ ] 결제 요청(POST /payments) 및 결과 표시
- [ ] 대기열 진입/순번 폴링, 내 예매 목록(GET /reservations/me) 동작

## 코드 품질 / 보안
- [ ] 모든 API 응답·컴포넌트 props에 명시적 TypeScript 타입 (any 남용 없음)
- [ ] EventSource 등 구독 자원이 cleanup(useEffect return)으로 해제됨
- [ ] 변경 코드에 보안 취약점 없음 (토큰 저장 방식 명시 — localStorage XSS 고려사항 주석/문서화)
- [ ] 기존 백엔드 테스트에 영향 없음 (frontend는 백엔드와 분리)

## 마무리
- [ ] `frontend/**` 외 파일 변경 없음 (백엔드 경계 준수)
- [ ] `frontend/node_modules`·`dist`가 .gitignore에 포함
- [ ] 완료 후 `.qe/TASK_LOG.md`의 Phase 9 항목을 ✅로 갱신
