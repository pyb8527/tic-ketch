<!-- chained-from: Qgenerate-spec -->
# TASK_REQUEST — Phase 9: Frontend

- **UUID**: 216595fa
- **Type**: code
- **Phase**: Phase 9 / 단계별 구현 계획 (doc/SPEC.md §15) — (Phase 8 Jenkins는 서버 미구축으로 보류)
- **대상 프로젝트**: `D:\git\tic-ketch-front` (**별도 독립 git 저장소** — 모노레포 아님)
  - 이하 체크리스트의 `frontend/` 경로는 모두 `D:\git\tic-ketch-front\` 루트 기준으로 읽는다 (예: `frontend/src/App.tsx` → `D:\git\tic-ketch-front\src\App.tsx`)
  - QE 스펙/이력은 tic-ketch 저장소에 유지, 코드 산출물만 새 저장소에 생성
- **스택**: Vite + React 18 + TypeScript(strict) · Tailwind CSS 3 · Zustand · @tanstack/react-query v5 · react-router-dom v6 · axios · 네이티브 EventSource(SSE)

---

## What (비즈니스 목표)
티켓 예매 핵심 플로우의 웹 UI를 구현한다: **로그인 → 공연 목록/상세 → 좌석 선택(실시간 SSE) → 임시 선점 → 결제(5분 타이머) → 확정**, 그리고 대기열·내 예매 목록.

**완료 조건(핵심)**
1. `npm install` 후 `npm run build`(tsc + vite) 성공 (타입 에러 0)
2. 로그인하면 토큰 저장 + 이후 요청에 Authorization 자동 첨부
3. 좌석 선택 페이지가 SSE로 좌석 상태 변경을 실시간 반영
4. 결제 페이지에 5분 카운트다운, 만료 시 자동 이동

## How (기술 구현 로직)
- 모든 API는 **Gateway :8080** 경유. `VITE_API_BASE_URL`(기본 `http://localhost:8080`)
- 응답 래퍼 `{code,message,data}` → axios 응답 인터셉터에서 `data.data` 언래핑, code≠SUCCESS면 throw
- 인증: 로그인 토큰을 Zustand+localStorage 저장, 요청 인터셉터가 `Authorization: Bearer` 첨부. 401 시 로그아웃→로그인 이동
- 서버 상태(공연/좌석/예약)는 **React Query**, 클라이언트 상태(인증)는 **Zustand**
- SSE: `new EventSource('{base}/api/events/{id}/seats/stream')` — `seat-status` 이벤트 수신 시 좌석 쿼리 무효화(refetch)
- 디자인: 기존 `doc/plan.html` 다크 테마 계승 (bg #0f1117, accent #6c63ff, accent2 #00d4aa). **shadcn/ui 대신 Tailwind 수제 컴포넌트**(빌드 단순화) — 의도된 결정
- `X-User-Id`는 Gateway가 토큰에서 주입하므로 프론트는 보내지 않음

---

## 체크리스트 (Atomic Items)

### Wave 1 — 스캐폴드 (먼저 단독 실행 → npm install)
- [ ] **프로젝트 스캐폴드**: `package.json`(deps: react, react-dom, react-router-dom, @tanstack/react-query, zustand, axios / devDeps: vite, @vitejs/plugin-react, typescript, tailwindcss@3, postcss, autoprefixer, @types/react, @types/react-dom / scripts: dev/build("tsc && vite build")/preview), `vite.config.ts`, `tsconfig.json`(strict), `tsconfig.node.json`, `index.html`, `tailwind.config.js`(content+테마 컬러 확장), `postcss.config.js`, `src/index.css`(@tailwind + 다크 배경), `src/vite-env.d.ts`, `.gitignore`(node_modules,dist), `.env`(VITE_API_BASE_URL=http://localhost:8080) → output: `frontend/package.json` 외 설정 파일 일괄

### Wave 2 — 타입·API·스토어 (스캐폴드 의존)
- [ ] **타입 정의**: `ApiResponse<T>`, `Page<T>`, `TokenResponse`, `User`, `EventDto`, `SeatDto`, `ReservationDto`, `PaymentDto`, `QueueStatus`, enum 문자열 타입 → output: `frontend/src/types/api.ts` <!-- depends_on: [스캐폴드] -->
- [ ] **Axios 클라이언트**: 인스턴스(baseURL=env), 요청 인터셉터(Bearer 첨부), 응답 인터셉터(`data.data` 언래핑 + code 검사 + 401 처리) → output: `frontend/src/api/client.ts` <!-- depends_on: [타입] -->
- [ ] **API 모듈**: `auth.ts`(register/login/me), `events.ts`(list/detail/seats), `reservations.ts`(hold/get/myList/cancel), `payments.ts`(request/get/cancel), `queue.ts`(enter/status) → output: `frontend/src/api/*.ts` <!-- depends_on: [클라이언트, 타입] -->
- [ ] **Auth 스토어(Zustand)**: `{accessToken, user, setAuth, logout}` + localStorage persist → output: `frontend/src/store/authStore.ts` <!-- depends_on: [타입] -->

### Wave 3 — 공통 컴포넌트·훅 (타입/스토어 의존, 페이지와 독립)
- [ ] **Layout + ProtectedRoute**: 헤더(로고/네비/로그인상태·로그아웃) + 인증 필요 라우트 가드(미인증 시 /login) → output: `frontend/src/components/Layout.tsx`, `frontend/src/components/ProtectedRoute.tsx` <!-- depends_on: [스토어] -->
- [ ] **useSeatSSE 훅**: EventSource 연결/해제, `seat-status` 수신 시 onUpdate 콜백, cleanup → output: `frontend/src/hooks/useSeatSSE.ts` <!-- depends_on: [스캐폴드] -->
- [ ] **ReservationTimer**: `expiresAt` 기준 mm:ss 카운트다운, 0 도달 시 `onExpire` 호출 → output: `frontend/src/components/ReservationTimer.tsx` <!-- depends_on: [스캐폴드] -->
- [ ] **SeatMap**: `seats[]`를 SVG 격자(행/번호)로 렌더, 상태별 색(AVAILABLE/HELD/SOLD), 클릭 시 `onSelect(seatId)` → output: `frontend/src/components/SeatMap.tsx` <!-- depends_on: [타입] -->

### Wave 4 — 페이지 (컴포넌트·훅·API 의존)
- [ ] **인증 페이지**: `LoginPage`(이메일/비번 → login → 토큰 저장 → 목록 이동), `RegisterPage`(가입 → 로그인 이동) → output: `frontend/src/pages/LoginPage.tsx`, `frontend/src/pages/RegisterPage.tsx` <!-- depends_on: [API, 스토어] -->
- [ ] **공연 페이지**: `EventListPage`(목록/페이징/상태배지), `EventDetailPage`(상세 + '좌석 선택' 버튼) → output: `frontend/src/pages/EventListPage.tsx`, `frontend/src/pages/EventDetailPage.tsx` <!-- depends_on: [API] -->
- [ ] **좌석 선택 페이지**: `SeatSelectionPage` — 좌석 조회(React Query) + `useSeatSSE` 실시간 갱신 + `SeatMap` + 선택 좌석 '선점'(POST /reservations) → 결제 페이지 이동 → output: `frontend/src/pages/SeatSelectionPage.tsx` <!-- depends_on: [SeatMap, useSeatSSE, API] -->
- [ ] **결제 페이지**: `PaymentPage` — 예약 상세 조회 + `ReservationTimer`(만료 시 목록 이동) + '결제하기'(POST /payments) → 결과 표시 → output: `frontend/src/pages/PaymentPage.tsx` <!-- depends_on: [ReservationTimer, API] -->
- [ ] **대기열·내예매 페이지**: `QueuePage`(진입 + 순번 폴링), `MyReservationsPage`(GET /reservations/me 목록) → output: `frontend/src/pages/QueuePage.tsx`, `frontend/src/pages/MyReservationsPage.tsx` <!-- depends_on: [API] -->

### Wave 5 — 라우팅 결선 + 빌드 검증
- [ ] **App 라우팅 결선**: `App.tsx`에 전체 라우트 연결(`/login`,`/register`,`/`,`/events/:id`,`/events/:id/seats`,`/payment/:reservationId`,`/queue/:eventId`,`/me`), Protected 적용. `npm install` 후 `npm run build` 성공 검증 → output: `frontend/src/App.tsx`(갱신) <!-- depends_on: [모든 페이지] -->

---

## Notes
- TypeScript **strict** — 모든 props/응답 타입 명시. `tsc && vite build`가 통과해야 완료
- `EventDto.eventDate`/`ReservationDto.expiresAt`은 ISO 문자열(LocalDateTime). 화면 표시 시 `new Date()` 파싱
- SSE `seat-status` 페이로드 형식이 불확실 → 수신 시 **좌석 쿼리 invalidate(refetch)** 방식으로 견고하게 처리
- 백엔드 실행 없이도 **빌드는 통과**해야 함(런타임 연동은 백엔드+Gateway 기동 시). 완료 기준은 빌드 성공 + 코드 정합성
- `GET /api/users/me` 응답 필드는 UserController 확인 후 `User` 타입 확정(미확정 시 `{id,email,name,role}` 가정, [UNVERIFIED] 태그)
- Role ownership: `frontend/**`만 생성/수정. 백엔드 변경 금지
