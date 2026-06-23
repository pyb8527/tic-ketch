# Phase 9 CONTRACTS — Frontend (단일 진실 공급원)

> **별도 프로젝트**. 모든 경로의 루트 = `D:\git\tic-ketch-front\` (이하 `<root>`).
> 스택: Vite + React 18 + TypeScript(strict) · Tailwind 3 · Zustand · @tanstack/react-query v5 · react-router-dom v6 · axios.
> 완료 기준: `npm run build`(= `tsc && vite build`) 통과(타입 에러 0).

## 0. 공통 규칙
- 모든 API는 Gateway(`VITE_API_BASE_URL`, 기본 `http://localhost:8080`) 경유. 응답 래퍼 `{code,message,data}`.
- import는 상대경로(`../types/api`). 별칭(@) 미사용.
- 테마: 다크. bg `#0f1117`, surface `#1a1d27`, accent `#6c63ff`, accent2 `#00d4aa`, danger `#ff6b6b`, text `#e5e7eb`, muted `#94a3b8`.
- 좌석/예약/결제 상태 색: AVAILABLE/PENDING=accent2, HELD/PROCESSING=accent4(#ffd93d), SOLD/CONFIRMED=muted, FAILED/CANCELLED/EXPIRED=danger.

## 1. 스캐폴드 (Wave 1) — `<root>` 직하 설정 파일

### `package.json`
```json
{
  "name": "tic-ketch-front",
  "private": true,
  "version": "0.1.0",
  "type": "module",
  "scripts": { "dev": "vite", "build": "tsc -b && vite build", "preview": "vite preview" },
  "dependencies": {
    "axios": "^1.7.9",
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "react-router-dom": "^6.28.0",
    "zustand": "^5.0.2",
    "@tanstack/react-query": "^5.62.0"
  },
  "devDependencies": {
    "@types/react": "^18.3.12",
    "@types/react-dom": "^18.3.1",
    "@vitejs/plugin-react": "^4.3.4",
    "autoprefixer": "^10.4.20",
    "postcss": "^8.4.49",
    "tailwindcss": "^3.4.17",
    "typescript": "^5.7.2",
    "vite": "^6.0.3"
  }
}
```
### `vite.config.ts`
```ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
export default defineConfig({ plugins: [react()], server: { port: 5173 } });
```
### `tsconfig.json` (단일 파일, strict; `tsc -b` 대신 단순화 위해 references 없이)
```json
{
  "compilerOptions": {
    "target": "ES2020", "useDefineForClassFields": true, "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext", "skipLibCheck": true, "moduleResolution": "bundler",
    "allowImportingTsExtensions": true, "resolveJsonModule": true, "isolatedModules": true,
    "noEmit": true, "jsx": "react-jsx", "strict": true,
    "noUnusedLocals": false, "noUnusedParameters": false, "noFallthroughCasesInSwitch": true
  },
  "include": ["src"]
}
```
> package.json build script을 `"tsc && vite build"`로 하고 tsconfig는 위 단일 파일로(references/tsconfig.node 불필요). vite.config.ts는 noEmit tsc 대상에서 제외되지 않지만 allowImportingTsExtensions+isolatedModules로 통과. 문제가 있으면 `"build": "vite build"`로 단순화 가능하나 1차로 `tsc && vite build` 사용.
### `index.html` (`<root>/index.html`)
```html
<!doctype html><html lang="ko"><head><meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>TicKetch</title></head>
<body><div id="root"></div><script type="module" src="/src/main.tsx"></script></body></html>
```
### `tailwind.config.js`
```js
/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: { extend: { colors: {
    bg: '#0f1117', surface: '#1a1d27', accent: '#6c63ff', accent2: '#00d4aa',
    danger: '#ff6b6b', warn: '#ffd93d'
  } } },
  plugins: [],
};
```
### `postcss.config.js`
```js
export default { plugins: { tailwindcss: {}, autoprefixer: {} } };
```
### `src/index.css`
```css
@tailwind base; @tailwind components; @tailwind utilities;
body { @apply bg-bg text-gray-200; margin: 0; font-family: system-ui, -apple-system, sans-serif; }
```
### `src/vite-env.d.ts`
```ts
/// <reference types="vite/client" />
interface ImportMetaEnv { readonly VITE_API_BASE_URL?: string; }
interface ImportMeta { readonly env: ImportMetaEnv; }
```
### `src/main.tsx`
```tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import App from './App';
import './index.css';
const queryClient = new QueryClient();
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter><App /></BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>
);
```
### `src/App.tsx` (Wave 1 = 임시 placeholder, Wave 5에서 라우트 결선)
Wave 1엔 최소 `export default function App(){ return <div>TicKetch</div>; }` 만. Wave 5에서 교체.
### `.gitignore`: `node_modules`, `dist`, `*.local`, `.env.local`
### `.env`: `VITE_API_BASE_URL=http://localhost:8080`

## 2. 타입 (Wave 2) — `src/types/api.ts`
```ts
export interface ApiResponse<T> { code: string; message: string; data: T; }
export interface Page<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number; }
export interface TokenResponse { accessToken: string; refreshToken: string; }
export interface User { id: number; email: string; name: string; role: string; }
export type EventStatus = 'UPCOMING'|'ON_SALE'|'SOLD_OUT'|'ENDED';
export interface EventDto { id: number; title: string; venue: string; eventDate: string; status: EventStatus; }
export type SeatStatus = 'AVAILABLE'|'HELD'|'SOLD';
export interface SeatDto { id: number; seatGradeId: number; rowName: string; seatNumber: number; status: SeatStatus; }
export type ReservationStatus = 'PENDING'|'CONFIRMED'|'CANCELLED'|'EXPIRED';
export interface ReservationDto { id: number; seatId: number; eventId: number; status: ReservationStatus; expiresAt: string; remainingSeconds: number; }
export interface HoldSeatResult { reservationId: number; expiresAt: string; }
export type PaymentStatus = 'PENDING'|'COMPLETED'|'FAILED'|'REFUNDED';
export interface PaymentDto { id: number; reservationId: number; amount: number; status: PaymentStatus; paidAt: string | null; }
export interface PaymentResult { paymentId: number; status: string; }
export interface QueueStatus { position: number; totalWaiting: number; }
export interface LoginRequest { email: string; password: string; }
export interface RegisterRequest { email: string; password: string; name: string; }
```

## 3. API 클라이언트 (Wave 2) — `src/api/client.ts`
```ts
import axios, { AxiosResponse } from 'axios';
import { useAuthStore } from '../store/authStore';
import type { ApiResponse } from '../types/api';
const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';
export const api = axios.create({ baseURL });
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err?.response?.status === 401) useAuthStore.getState().logout();
    const msg = err?.response?.data?.message ?? err.message ?? '요청 실패';
    return Promise.reject(new Error(msg));
  }
);
// 공통 언래퍼: {code,message,data} → data, code≠SUCCESS면 throw
export async function unwrap<T>(p: Promise<AxiosResponse<ApiResponse<T>>>): Promise<T> {
  const res = await p;
  if (res.data.code !== 'SUCCESS') throw new Error(res.data.message);
  return res.data.data;
}
```
> client는 store를 import한다(순환 없음 — store는 client를 import하지 않음).

## 4. API 모듈 (Wave 2) — `src/api/*.ts`
모두 `import { api, unwrap } from './client'`, `import type {...} from '../types/api'`.
```ts
// auth.ts
export const register = (body: RegisterRequest) => unwrap<number>(api.post('/api/auth/register', body));
export const login = (body: LoginRequest) => unwrap<TokenResponse>(api.post('/api/auth/login', body));
export const getMe = () => unwrap<User>(api.get('/api/users/me'));
// events.ts
export const getEvents = (page = 0) => unwrap<Page<EventDto>>(api.get('/api/events', { params: { page, size: 20 } }));
export const getEvent = (id: number) => unwrap<EventDto>(api.get(`/api/events/${id}`));
export const getSeats = (eventId: number) => unwrap<SeatDto[]>(api.get(`/api/events/${eventId}/seats`));
// reservations.ts
export const holdSeat = (body: { seatId: number; eventId: number }) => unwrap<HoldSeatResult>(api.post('/api/reservations', body));
export const getReservation = (id: number) => unwrap<ReservationDto>(api.get(`/api/reservations/${id}`));
export const getMyReservations = () => unwrap<ReservationDto[]>(api.get('/api/reservations/me'));
export const cancelReservation = (id: number) => unwrap<null>(api.delete(`/api/reservations/${id}`));
// payments.ts
export const requestPayment = (body: { reservationId: number; amount: number }) => unwrap<PaymentResult>(api.post('/api/payments', body));
export const getPayment = (id: number) => unwrap<PaymentDto>(api.get(`/api/payments/${id}`));
// queue.ts
export const enterQueue = (eventId: number) => unwrap<QueueStatus>(api.post(`/api/queue/${eventId}/enter`));
export const getQueueStatus = (eventId: number) => unwrap<QueueStatus>(api.get(`/api/queue/${eventId}`));
```
> `api.post('/api/auth/login', body)` 의 제네릭 추론을 위해 unwrap에 타입 인자를 명시(위처럼). axios 호출은 `AxiosResponse<ApiResponse<T>>`로 추론되도록 `api.post<ApiResponse<T>>(...)`가 더 안전 — 각 함수에서 `api.post<ApiResponse<number>>('/api/auth/register', body)` 형태로 작성.

## 5. Auth 스토어 (Wave 2) — `src/store/authStore.ts`
```ts
import { create } from 'zustand';
import type { User } from '../types/api';
const TOKEN_KEY = 'ticketch_token';
interface AuthState {
  accessToken: string | null;
  user: User | null;
  setToken: (token: string) => void;
  setUser: (user: User | null) => void;
  logout: () => void;
}
export const useAuthStore = create<AuthState>((set) => ({
  accessToken: localStorage.getItem(TOKEN_KEY),
  user: null,
  setToken: (token) => { localStorage.setItem(TOKEN_KEY, token); set({ accessToken: token }); },
  setUser: (user) => set({ user }),
  logout: () => { localStorage.removeItem(TOKEN_KEY); set({ accessToken: null, user: null }); },
}));
```

## 6. 컴포넌트·훅 (Wave 3)
### `src/components/Layout.tsx`
`export default function Layout({ children }: { children: React.ReactNode })`. 상단 헤더(좌: 로고 'TicKetch' Link to "/", 우: 인증 시 '내 예매'(/me)·로그아웃 버튼 / 미인증 시 '로그인'(/login)). `useAuthStore`로 accessToken 확인, logout 후 navigate('/login'). 본문 `<main className="max-w-5xl mx-auto p-6">{children}</main>`.
### `src/components/ProtectedRoute.tsx`
`export default function ProtectedRoute({ children }: { children: React.ReactNode })`: accessToken 없으면 `<Navigate to="/login" replace />`, 있으면 children. (react-router-dom Navigate)
### `src/hooks/useSeatSSE.ts`
`export function useSeatSSE(eventId: number, onSeatUpdate: () => void): void`. useEffect에서 `const base = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'; const es = new EventSource(`${base}/api/events/${eventId}/seats/stream`);` → `es.addEventListener('seat-status', () => onSeatUpdate());` cleanup `es.close()`. onSeatUpdate를 deps에 포함하되 호출부에서 useCallback 권장(주석).
### `src/components/ReservationTimer.tsx`
`export default function ReservationTimer({ expiresAt, onExpire }: { expiresAt: string; onExpire: () => void })`. expiresAt(ISO)까지 남은 초를 setInterval(1s)로 계산, mm:ss 표시(0이면 onExpire 1회 호출 후 정지). useEffect cleanup으로 clearInterval. 남은 1분 이하면 danger 색.
### `src/components/SeatMap.tsx`
`export default function SeatMap({ seats, selectedSeatId, onSelect }: { seats: SeatDto[]; selectedSeatId: number | null; onSelect: (seatId: number) => void })`. rowName으로 그룹핑→정렬, 각 좌석을 버튼/사각형으로 격자 렌더(SVG 또는 div grid 허용). 색: AVAILABLE=accent2(클릭 가능), HELD=warn(비활성), SOLD=muted(비활성), 선택됨=accent. AVAILABLE만 onSelect 호출. 범례 포함.

## 7. 페이지 (Wave 4) — `src/pages/*.tsx`
공통: `useNavigate`, React Query(`useQuery`/`useMutation`), 에러는 alert 또는 인라인 메시지. 모든 페이지는 Layout 안에서 렌더(App에서 감싸거나 각 페이지가 Layout 사용 — App.tsx에서 Layout으로 감싸는 방식 채택, 페이지는 콘텐츠만).
- `LoginPage.tsx`: 이메일/비번 form → `login()` → `setToken(res.accessToken)` → `getMe()` 호출해 `setUser` → navigate('/'). 가입 링크.
- `RegisterPage.tsx`: 이메일/비번/이름 → `register()` → navigate('/login').
- `EventListPage.tsx`: `useQuery(['events',page], ()=>getEvents(page))` → 카드 그리드, 상태 배지, 페이지네이션. 카드 클릭 → `/events/${id}`.
- `EventDetailPage.tsx`: `useParams` id → `getEvent` → 상세 + '좌석 선택' 버튼(`/events/${id}/seats`). (대기열 '입장' 버튼 → `/queue/${id}` 선택)
- `SeatSelectionPage.tsx`: id=useParams. `useQuery(['seats',id], ()=>getSeats(id))`. `useSeatSSE(id, ()=>queryClient.invalidateQueries({queryKey:['seats',id]}))` (useCallback). `SeatMap` + 선택 상태. '선점하기' → `useMutation(holdSeat)` → 성공 시 navigate(`/payment/${reservationId}`). 미인증이면 ProtectedRoute가 막음(라우트에 적용).
- `PaymentPage.tsx`: reservationId=useParams. `getReservation` → 좌석/만료 표시 + `ReservationTimer expiresAt onExpire={()=>navigate('/')}`. '결제하기' → `requestPayment({reservationId, amount})` (amount는 임시 고정값 예: 50000 또는 좌석등급 미연동이므로 50000 고정, 주석) → 결과 status 표시(성공 시 '예매 완료' + '내 예매' 링크).
- `QueuePage.tsx`: eventId=useParams. 입장 버튼 → `enterQueue`. `useQuery(['queue',eventId], ()=>getQueueStatus(eventId), { refetchInterval: 2000 })` 순번 폴링 표시.
- `MyReservationsPage.tsx`: `useQuery(['myReservations'], getMyReservations)` 목록(상태 배지, 취소 버튼 → cancelReservation + invalidate).

## 8. 라우팅 (Wave 5) — `src/App.tsx`
```tsx
import { Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
// pages import ...
export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/" element={<EventListPage />} />
        <Route path="/events/:id" element={<EventDetailPage />} />
        <Route path="/events/:id/seats" element={<ProtectedRoute><SeatSelectionPage /></ProtectedRoute>} />
        <Route path="/payment/:reservationId" element={<ProtectedRoute><PaymentPage /></ProtectedRoute>} />
        <Route path="/queue/:eventId" element={<ProtectedRoute><QueuePage /></ProtectedRoute>} />
        <Route path="/me" element={<ProtectedRoute><MyReservationsPage /></ProtectedRoute>} />
      </Routes>
    </Layout>
  );
}
```
> Wave 5에서 `npm run build` 통과까지 책임. 타입 에러 시 수정.
