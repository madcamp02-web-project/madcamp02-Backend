# 🎨 MadCamp02: 프론트엔드 개발 계획서

**Ver 1.0 - Frontend Development Blueprint**

---

## 📋 목차

1. [현황 분석](#1-현황-분석)
2. [아키텍처 설계](#2-아키텍처-설계)
3. [디렉토리 구조](#3-디렉토리-구조)
4. [페이지 설계](#4-페이지-설계)
5. [컴포넌트 설계](#5-컴포넌트-설계)
6. [상태 관리 설계](#6-상태-관리-설계)
7. [API 통신 레이어](#7-api-통신-레이어)
8. [실시간 통신 설계](#8-실시간-통신-설계)
9. [인증 시스템](#9-인증-시스템)
10. [UI/UX 시스템](#10-uiux-시스템)
11. [테스트 전략](#11-테스트-전략)
12. [개발 로드맵](#12-개발-로드맵)

---

## 1. 현황 분석

### 1.1 현재 구현 완료 항목

| 영역 | 파일/컴포넌트 | 상태 | 비고 |
|------|-------------|------|------|
| **레이아웃** | `components/layout/app-layout.tsx` | ✅ 완료 | Header + Sidebar 통합 |
| **헤더** | `components/dashboard/header.tsx` | ✅ 완료 | 검색, 알림, 프로필 |
| **사이드바** | `components/dashboard/sidebar.tsx` | ✅ 완료 | 네비게이션 메뉴 |
| **대시보드** | `app/page.tsx` | ✅ 완료 | 드래그 가능 위젯 그리드 |
| **차트** | `components/dashboard/stock-chart.tsx` | ⚠️ 부분 | Area 차트 (캔들 필요) |
| **매수/매도** | `components/dashboard/trade-panel.tsx` | ⚠️ 부분 | UI만 (API 연동 필요) |
| **관심종목** | `components/dashboard/watchlist.tsx` | ⚠️ 부분 | Mock 데이터 사용 |
| **포트폴리오** | `components/dashboard/portfolio-summary.tsx` | ⚠️ 부분 | Mock 데이터 사용 |
| **AI 챗봇** | `components/dashboard/ai-chatbot.tsx` | ⚠️ 부분 | 하드코딩 응답 |
| **아바타** | `components/dashboard/avatar-display.tsx` | ✅ 완료 | 수익률 기반 효과 |
| **미니랭킹** | `components/dashboard/mini-ranking.tsx` | ⚠️ 부분 | Mock 데이터 사용 |
| **AI 도사** | `app/oracle/page.tsx` | ⚠️ 부분 | 사주/별자리/오하아사 탭 |
| **가챠** | `app/gacha/page.tsx` | ✅ 완료 | 캡슐토이 UI 완성 |
| **랭킹** | `app/ranking/page.tsx` | ⚠️ 부분 | 포디움 UI 완성 |
| **마이페이지** | `app/mypage/page.tsx` | ⚠️ 부분 | 인벤토리/설정 탭 |

### 1.2 신규 구현 필요 항목

| 영역 | 우선순위 | 상태 |
|------|---------|------|
| 인증 시스템 (OAuth2 + JWT) | 🔴 Critical | ❌ 미구현 |
| 온보딩 페이지 | 🔴 Critical | ❌ 미구현 |
| 상태 관리 (Zustand) | 🔴 Critical | ❌ 미구현 |
| API 클라이언트 레이어 | 🔴 Critical | ❌ 미구현 |
| WebSocket (STOMP) 연동 | 🔴 Critical | ❌ 미구현 |
| 캔들 차트 | 🟡 High | ❌ 미구현 |
| SSE 스트리밍 (AI) | 🟡 High | ❌ 미구현 |
| 배당금 계산기 | 🟢 Medium | ❌ 미구현 |
| 알림 드롭다운 | 🟢 Medium | ❌ 미구현 |
| 종목 검색 자동완성 | 🟢 Medium | ❌ 미구현 |

### 1.3 현재 기술 스택

```
✅ 설치됨:
├── Next.js 16.0.10
├── React 19.2.0
├── TypeScript 5.x
├── Tailwind CSS 4.x
├── Shadcn/UI (New York)
├── Recharts 2.15.4
├── Lucide React 0.454.0
├── tw-animate-css 1.3.3
└── date-fns 4.1.0

🆕 추가 필요:
├── zustand ^4.x (상태 관리)
├── @stomp/stompjs ^7.x (WebSocket)
├── sockjs-client ^1.x (WebSocket 폴백)
├── axios ^1.x (HTTP 클라이언트)
├── next-auth ^5.x (인증)
├── lightweight-charts ^4.x (캔들 차트)
├── dayjs ^1.x (날짜)
└── immer ^10.x (불변 상태)
```

---

## 2. 아키텍처 설계

### 2.1 레이어 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER                      │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Pages (app/)                                           │ │
│  │  └── React Server Components + Client Components        │ │
│  └─────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Components (components/)                                │ │
│  │  ├── UI Components (ui/) - Shadcn/UI                    │ │
│  │  ├── Feature Components (dashboard/, auth/)             │ │
│  │  └── Layout Components (layout/)                        │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       STATE LAYER                            │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Zustand Stores (stores/)                                │ │
│  │  ├── auth-store.ts      (인증 상태)                      │ │
│  │  ├── user-store.ts      (유저/지갑)                      │ │
│  │  ├── stock-store.ts     (실시간 주가)                    │ │
│  │  ├── portfolio-store.ts (포트폴리오)                     │ │
│  │  └── notification-store.ts (알림)                        │ │
│  └─────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Custom Hooks (hooks/)                                   │ │
│  │  ├── use-auth.ts        (인증 훅)                        │ │
│  │  ├── use-websocket.ts   (WebSocket 연결)                 │ │
│  │  ├── use-stock-price.ts (실시간 가격)                    │ │
│  │  ├── use-ai-chat.ts     (AI 스트리밍)                    │ │
│  │  └── use-notifications.ts (알림)                         │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      SERVICE LAYER                           │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  API Client (lib/api/)                                   │ │
│  │  ├── index.ts           (Axios 인스턴스)                 │ │
│  │  ├── auth.ts            (인증 API)                       │ │
│  │  ├── user.ts            (유저 API)                       │ │
│  │  ├── trade.ts           (거래 API)                       │ │
│  │  ├── stock.ts           (주식 API)                       │ │
│  │  ├── game.ts            (게임/가챠 API)                  │ │
│  │  └── chat.ts            (AI 상담 API)                    │ │
│  └─────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  WebSocket Client (lib/)                                 │ │
│  │  └── stomp-client.ts    (STOMP over SockJS)              │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      EXTERNAL LAYER                          │
│  ├── REST API (Spring Boot)                                  │
│  ├── WebSocket (STOMP Broker)                                │
│  └── SSE (FastAPI AI Server)                                 │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 데이터 흐름

```
[User Action]
      │
      ▼
[Component] ─── dispatch ───▶ [Zustand Store]
      │                              │
      │                              ▼
      │                       [State Update]
      │                              │
      ▼                              ▼
[API Call] ◀──────────────── [Re-render]
      │
      ▼
[Backend Response]
      │
      ▼
[Store Update] ──▶ [UI Update]
```

### 2.3 실시간 데이터 흐름

```
[STOMP Server] ─── WebSocket ───▶ [stomp-client.ts]
                                        │
                                        ▼
                               [useWebSocket Hook]
                                        │
                                        ▼
                               [stock-store.ts]
                                        │
                                        ▼
                               [Component Re-render]
                                        │
                                        ▼
                               [UI Update (가격, 차트)]
```

---

## 3. 디렉토리 구조

### 3.1 최종 디렉토리 구조

```
📁 madcamp02/
├── 📁 app/                              # Next.js App Router
│   ├── layout.tsx                       # ✅ 루트 레이아웃 (Geist 폰트, Analytics)
│   ├── globals.css                      # ✅ 글로벌 스타일 (oklch 컬러 시스템)
│   ├── page.tsx                         # ✅ 대시보드 (드래그 가능 위젯 그리드)
│   ├── loading.tsx                      # 🆕 글로벌 로딩
│   ├── not-found.tsx                    # 🆕 404 페이지
│   │
│   ├── 📁 login/
│   │   └── page.tsx                     # 🆕 로그인 (일반 + Google + Kakao OAuth)
│   ├── 📁 signup/
│   │   └── page.tsx                     # 🆕 회원가입 (이메일/비밀번호/닉네임)
│   ├── 📁 onboarding/
│   │   └── page.tsx                     # 🆕 온보딩 (생년월일 입력 → 사주 계산)
│   ├── 📁 oracle/
│   │   └── page.tsx                     # ✅ AI 도사 상담 (사주/별자리/오하아사 탭)
│   ├── 📁 gacha/
│   │   └── page.tsx                     # ✅ 가챠 (캡슐 토이 머신 UI)
│   ├── 📁 ranking/
│   │   └── page.tsx                     # ✅ 랭킹 (포디움 + 전체 순위)
│   ├── 📁 mypage/
│   │   └── page.tsx                     # ✅ 마이페이지 (개요/인벤토리/아바타/설정 탭)
│   └── 📁 calculator/
│       └── page.tsx                     # 🆕 배당금 계산기
│
├── 📁 components/
│   ├── 📁 auth/                         # 🆕 인증 컴포넌트
│   │   ├── login-form.tsx               # 🆕 일반 로그인 + OAuth 버튼
│   │   ├── signup-form.tsx              # 🆕 일반 회원가입 폼
│   │   ├── onboarding-form.tsx          # 생년월일 입력 → 사주 계산
│   │   └── protected-route.tsx          # 인증 보호 래퍼
│   │
│   ├── 📁 dashboard/                    # 대시보드 컴포넌트
│   │   ├── header.tsx                   # ✅ 헤더 (기존)
│   │   ├── sidebar.tsx                  # ✅ 사이드바 (기존)
│   │   ├── stock-chart.tsx              # ✅ Area 차트 (기존)
│   │   ├── candle-chart.tsx             # 🆕 캔들스틱 차트
│   │   ├── trade-panel.tsx              # ⚠️ 매수/매도 (API 연동 필요)
│   │   ├── watchlist.tsx                # ⚠️ 관심종목 (API 연동 필요)
│   │   ├── portfolio-summary.tsx        # ⚠️ 포트폴리오 (API 연동 필요)
│   │   ├── ai-chatbot.tsx               # ⚠️ AI 챗봇 위젯 (SSE 연동 필요)
│   │   ├── avatar-display.tsx           # ✅ 아바타 (기존)
│   │   ├── mini-ranking.tsx             # ⚠️ 미니 랭킹 (API 연동 필요)
│   │   ├── notification-dropdown.tsx    # 🆕 알림 드롭다운
│   │   ├── stock-search.tsx             # 🆕 종목 검색 (자동완성)
│   │   └── dividend-calculator.tsx      # 🆕 배당금 계산기 위젯
│   │
│   ├── 📁 layout/
│   │   └── app-layout.tsx               # ✅ 앱 레이아웃 (기존)
│   │
│   ├── 📁 ui/                           # ✅ Shadcn/UI (기존 유지)
│   │   └── ... (기존 파일들)
│   │
│   ├── 📁 common/                       # 🆕 공통 컴포넌트
│   │   ├── loading-spinner.tsx          # 로딩 스피너
│   │   ├── skeleton-card.tsx            # 스켈레톤 UI
│   │   ├── empty-state.tsx              # 빈 상태
│   │   ├── error-boundary.tsx           # 에러 바운더리
│   │   └── price-badge.tsx              # 가격 변동 배지
│   │
│   └── theme-provider.tsx               # ✅ 테마 (기존)
│
├── 📁 hooks/                            # 커스텀 훅
│   ├── use-mobile.ts                    # ✅ 모바일 감지 (기존)
│   ├── use-toast.ts                     # ✅ 토스트 (기존)
│   ├── use-auth.ts                      # 🆕 인증 상태
│   ├── use-websocket.ts                 # 🆕 WebSocket 연결
│   ├── use-stock-price.ts               # 🆕 실시간 가격 구독
│   ├── use-ai-chat.ts                   # 🆕 AI SSE 스트리밍
│   ├── use-notifications.ts             # 🆕 알림
│   └── use-portfolio.ts                 # 🆕 포트폴리오
│
├── 📁 stores/                           # 🆕 Zustand 스토어
│   ├── auth-store.ts                    # 인증 상태
│   ├── user-store.ts                    # 유저/지갑 정보
│   ├── stock-store.ts                   # 실시간 주가
│   ├── portfolio-store.ts               # 포트폴리오
│   ├── notification-store.ts            # 알림
│   └── ui-store.ts                      # UI 상태 (모달, 사이드바 등)
│
├── 📁 lib/
│   ├── 📁 api/                          # 🆕 API 클라이언트
│   │   ├── index.ts                     # Axios 인스턴스 + 인터셉터
│   │   ├── auth.ts                      # 인증 API
│   │   ├── user.ts                      # 유저 API
│   │   ├── trade.ts                     # 거래 API
│   │   ├── stock.ts                     # 주식 API
│   │   ├── game.ts                      # 게임/가챠 API
│   │   ├── chat.ts                      # AI 상담 API
│   │   └── notification.ts              # 알림 API
│   │
│   ├── stomp-client.ts                  # 🆕 STOMP WebSocket
│   ├── saju-calculator.ts               # 🆕 오행/띠 계산
│   ├── utils.ts                         # ✅ 유틸리티 (기존)
│   ├── constants.ts                     # 🆕 상수 정의
│   └── mock-data.ts                     # ✅ Mock 데이터 (개발용)
│
├── 📁 types/                            # 🆕 TypeScript 타입 정의
│   ├── user.ts                          # 유저/인증 타입
│   ├── stock.ts                         # 주식/시세 타입
│   ├── trade.ts                         # 거래 타입
│   ├── game.ts                          # 게임/가챠 타입
│   ├── chat.ts                          # AI 상담 타입
│   └── api.ts                           # API 응답 타입
│
├── 📁 providers/                        # 🆕 Context Providers
│   ├── auth-provider.tsx                # 인증 Provider
│   ├── websocket-provider.tsx           # WebSocket Provider
│   └── query-provider.tsx               # React Query Provider (선택)
│
├── 📁 styles/
│   └── globals.css                      # ⚠️ 정리 필요 (app/globals.css와 통합)
│
├── 📁 public/                           # ✅ 정적 파일 (기존)
│   └── ... (아바타, 아이콘 등)
│
├── 📁 __tests__/                        # 🆕 테스트 파일
│   ├── 📁 unit/
│   │   ├── saju-calculator.test.ts
│   │   └── utils.test.ts
│   ├── 📁 components/
│   │   └── trade-panel.test.tsx
│   └── 📁 e2e/
│       ├── auth.spec.ts
│       ├── trade.spec.ts
│       └── gacha.spec.ts
│
├── .env.local                           # 🆕 환경 변수
├── .env.example                         # 🆕 환경 변수 예시
├── middleware.ts                        # 🆕 인증 미들웨어
├── next.config.mjs                      # ✅ Next.js 설정 (기존)
├── package.json                         # ⚠️ 패키지 추가 필요
├── tsconfig.json                        # ✅ TypeScript 설정 (기존)
└── tailwind.config.ts                   # 🆕 Tailwind 설정 (커스텀)
```

---

## 4. 페이지 설계

### 4.1 페이지 라우팅 맵

| 경로 | 페이지 | 레이아웃 | 인증 | 상태 | 설명 |
|------|--------|---------|------|------|------|
| `/login` | LoginPage | None | ❌ | 🆕 | 일반 로그인 + OAuth (Google, Kakao) |
| `/signup` | SignupPage | None | ❌ | 🆕 | 일반 회원가입 |
| `/onboarding` | OnboardingPage | None | ✅ | 🆕 | 생년월일 입력 (사주 계산) |
| `/` | DashboardPage | AppLayout | ✅ | ✅ | 메인 대시보드 |
| `/oracle` | OraclePage | AppLayout | ✅ | ✅ | AI 도사 상담 |
| `/gacha` | GachaPage | AppLayout | ✅ | ✅ | 가챠 (자판기) |
| `/ranking` | RankingPage | AppLayout | ✅ | ✅ | 수익률 랭킹 |
| `/mypage` | MyPage | AppLayout | ✅ | ✅ | 마이페이지 |
| `/calculator` | CalculatorPage | AppLayout | ✅ | 🆕 | 배당금 계산기 |

### 4.2 페이지별 상세 설계

#### 4.2.1 로그인 페이지 (`/login`) 🆕 업데이트

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│                    MadCamp02                            │
│               "차트는 운명을 말한다"                     │
│                                                         │
│        ┌─────────────────────────────────────┐          │
│        │  📧 이메일                          │          │
│        │  ┌──────────────────────────────┐  │          │
│        │  │  user@example.com            │  │          │
│        │  └──────────────────────────────┘  │          │
│        │                                    │          │
│        │  🔒 비밀번호                        │          │
│        │  ┌──────────────────────────────┐  │          │
│        │  │  ••••••••                    │  │          │
│        │  └──────────────────────────────┘  │          │
│        │                                    │          │
│        │  ┌──────────────────────────────┐  │          │
│        │  │        로그인               │  │          │
│        │  └──────────────────────────────┘  │          │
│        │                                    │          │
│        │  ─────── 또는 ───────             │          │
│        │                                    │          │
│        │  ┌──────────────────────────────┐  │          │
│        │  │  🔵 Google로 계속하기        │  │          │
│        │  └──────────────────────────────┘  │          │
│        │  ┌──────────────────────────────┐  │          │
│        │  │  🟡 카카오로 계속하기         │  │          │
│        │  └──────────────────────────────┘  │          │
│        │                                    │          │
│        │  아직 계정이 없으신가요? [회원가입] │          │
│        └─────────────────────────────────────┘          │
│                                                         │
│              배경: 별자리/사주 애니메이션                │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**구성 요소:**
- 로고 + 슬로건
- 🆕 이메일/비밀번호 입력 폼 (일반 로그인)
- 🆕 로그인 버튼
- 소셜 로그인 구분선 ("또는")
- Google OAuth 버튼
- 🆕 Kakao OAuth 버튼
- 🆕 회원가입 페이지 링크
- 별자리/운세 테마 배경 애니메이션

#### 4.2.1-1 회원가입 페이지 (`/signup`) 🆕

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│                    MadCamp02                            │
│                    회원가입                              │
│                                                         │
│        ┌─────────────────────────────────────┐          │
│        │  👤 닉네임                          │          │
│        │  ┌──────────────────────────────┐  │          │
│        │  │  투자도사                     │  │          │
│        │  └──────────────────────────────┘  │          │
│        │                                    │          │
│        │  📧 이메일                          │          │
│        │  ┌──────────────────────────────┐  │          │
│        │  │  user@example.com            │  │          │
│        │  └──────────────────────────────┘  │          │
│        │                                    │          │
│        │  🔒 비밀번호                        │          │
│        │  ┌──────────────────────────────┐  │          │
│        │  │  ••••••••                    │  │          │
│        │  └──────────────────────────────┘  │          │
│        │                                    │          │
│        │  🔒 비밀번호 확인                   │          │
│        │  ┌──────────────────────────────┐  │          │
│        │  │  ••••••••                    │  │          │
│        │  └──────────────────────────────┘  │          │
│        │                                    │          │
│        │  ┌──────────────────────────────┐  │          │
│        │  │      회원가입 완료           │  │          │
│        │  └──────────────────────────────┘  │          │
│        │                                    │          │
│        │  이미 계정이 있으신가요? [로그인]  │          │
│        └─────────────────────────────────────┘          │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**구성 요소:**
- 닉네임 입력 (필수)
- 이메일 입력 (필수, 중복 체크)
- 비밀번호 입력 (필수, 8자 이상)
- 비밀번호 확인 (일치 검증)
- 회원가입 버튼
- 로그인 페이지 링크

#### 4.2.2 온보딩 페이지 (`/onboarding`)

```
┌─────────────────────────────────────────────────────────┐
│  Step 1 of 2                                            │
│                                                         │
│  📅 생년월일을 입력해주세요                              │
│  ┌────────────────────────────────┐                     │
│  │  1995 / 05 / 20                │                     │
│  └────────────────────────────────┘                     │
│                                                         │
│  ✨ 닉네임                                               │
│  ┌────────────────────────────────┐                     │
│  │  투자도사                       │                     │
│  └────────────────────────────────┘                     │
│                                                         │
│  ┌──────────────────────┐                               │
│  │       다음 →         │                               │
│  └──────────────────────┘                               │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  Step 2 of 2                                            │
│                                                         │
│  🔥 당신의 사주는 "화(火)"입니다!                        │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │  🔥 화(火) - 열정과 확장의 기운                    │   │
│  │  • 성향: 열정적, 고위험/고수익 선호               │   │
│  │  • 행운의 색: Red                                 │   │
│  │  • 행운의 숫자: 3, 7                              │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  ┌──────────────────────┐                               │
│  │    투자 시작하기 🚀   │                              │
│  └──────────────────────┘                               │
└─────────────────────────────────────────────────────────┘
```

**구성 요소:**
- 단계 인디케이터
- 생년월일 Date Picker
- 닉네임 입력
- 사주 결과 표시 (오행, 띠, 행운 정보)
- 시작 버튼

#### 4.2.3 대시보드 페이지 (`/`)

```
┌──────────────────────────────────────────────────────────────────┐
│ [Header: 로고 | 검색 | 코인 | 알림 | 프로필]                      │
├───────────┬──────────────────────────────────────────────────────┤
│           │                                                      │
│  Sidebar  │  ┌────────────────────┬───────────┐                 │
│           │  │                    │           │                 │
│  - 대시보드│  │   주식 차트        │  아바타   │                 │
│  - AI도사  │  │   (3 cols)         │ (1 col)   │                 │
│  - 가챠    │  │                    │           │                 │
│  - 랭킹    │  ├────────────────────┴───────────┤                 │
│  - 마이페이지│ │                                │                 │
│           │  │  매수/매도 (1)  │  관심종목 (2)  │                 │
│  ─────────│  │                                │                 │
│  AI상담   │  ├──────────────────┬─────────────┤                 │
│  예수금   │  │                  │             │                 │
│           │  │  포트폴리오 (2)   │ AI챗봇 (2)  │                 │
│           │  │                  │             │                 │
│           │  ├──────────────────┴─────────────┤                 │
│           │  │                                │                 │
│           │  │        미니 랭킹 (2)            │                 │
│           │  │                                │                 │
│           │  └────────────────────────────────┘                 │
└───────────┴──────────────────────────────────────────────────────┘
```

**구성 요소:**
- 드래그 가능한 위젯 그리드 (4 cols)
- 위젯: 차트, 아바타, 매수/매도, 관심종목, 포트폴리오, AI챗봇, 미니랭킹
- 편집 모드 토글

#### 4.2.4 AI 도사 페이지 (`/oracle`)

```
┌──────────────────────────────────────────────────────────────────┐
│ [Header]                                                         │
├───────────┬──────────────────────────────────────────────────────┤
│           │  ┌────────────────────────────┬────────────────────┐ │
│  Sidebar  │  │                            │                    │ │
│           │  │      AI 도사 챗봇           │  사이드 패널        │ │
│           │  │      (2 cols)              │  (1 col)           │ │
│           │  │                            │                    │ │
│           │  │  ┌──────────────────────┐ │  [사주] [별자리]    │ │
│           │  │  │ 🧙 도사 메시지        │ │  [오하아사]         │ │
│           │  │  └──────────────────────┘ │                    │ │
│           │  │                            │  ┌──────────────┐ │ │
│           │  │  ┌──────────────────────┐ │  │ 내 사주 정보  │ │ │
│           │  │  │ 👤 사용자 메시지      │ │  │ 화(火)        │ │ │
│           │  │  └──────────────────────┘ │  │ 오행 밸런스   │ │ │
│           │  │                            │  └──────────────┘ │ │
│           │  │  [빠른 질문 버튼들]        │                    │ │
│           │  │  [입력창] [전송]          │  ┌──────────────┐ │ │
│           │  │                            │  │ 실시간 시장   │ │ │
│           │  └────────────────────────────┴──┴──────────────┴──┘ │
└───────────┴──────────────────────────────────────────────────────┘
```

**구성 요소:**
- AI 챗봇 (SSE 스트리밍)
- 빠른 질문 버튼
- 사주/별자리/오하아사 탭
- 실시간 시장 위젯

#### 4.2.5 가챠 페이지 (`/gacha`)

```
┌──────────────────────────────────────────────────────────────────┐
│ [Header]                                                         │
├───────────┬──────────────────────────────────────────────────────┤
│           │  ┌────────────────────────────┬────────────────────┐ │
│  Sidebar  │  │                            │                    │ │
│           │  │    🎰 캡슐 토이 머신        │  획득 가능 아이템   │ │
│           │  │       (2 cols)             │    (1 col)         │ │
│           │  │                            │                    │ │
│           │  │   ┌──────────────────┐    │  ┌──────────────┐  │ │
│           │  │   │   유리 돔        │    │  │ 황금 왕관    │  │ │
│           │  │   │   (캡슐들)       │    │  │ 불꽃 오라    │  │ │
│           │  │   └──────────────────┘    │  │ ...          │  │ │
│           │  │   ┌──────────────────┐    │  └──────────────┘  │ │
│           │  │   │   머신 본체      │    │                    │ │
│           │  │   │   (레버, 배출구)  │    │  ┌──────────────┐  │ │
│           │  │   └──────────────────┘    │  │ 확률 정보     │  │ │
│           │  │                            │  │ Common: 60%  │  │ │
│           │  │   [🎰 뽑기 (500 코인)]     │  │ Rare: 25%    │  │ │
│           │  │                            │  │ Epic: 12%    │  │ │
│           │  │   [결과 표시 영역]         │  │ Legendary:3% │  │ │
│           │  └────────────────────────────┴──┴──────────────┴──┘ │
└───────────┴──────────────────────────────────────────────────────┘
```

**구성 요소:**
- 캡슐 토이 머신 애니메이션
- 레버 돌리기 인터랙션
- 결과 표시 (아이템 + 희귀도)
- 아이템 목록 + 확률 정보

#### 4.2.6 랭킹 페이지 (`/ranking`)

```
┌──────────────────────────────────────────────────────────────────┐
│ [Header]                                                         │
├───────────┬──────────────────────────────────────────────────────┤
│           │  🏆 수익률 랭킹                내 순위: #42          │
│  Sidebar  │                                                      │
│           │  [이번 달] [이번 주] [전체 기간]                      │
│           │                                                      │
│           │  ┌──────────┬──────────┬──────────┐                 │
│           │  │  🥈 2위   │  🥇 1위   │  🥉 3위   │                 │
│           │  │          │  👑      │          │                 │
│           │  │ +128.45% │ +156.32% │ +98.21%  │                 │
│           │  └──────────┴──────────┴──────────┘                 │
│           │                                                      │
│           │  ┌──────────────────────────────────────────────┐   │
│           │  │ 4. 데이트레이더    +65.21%    👁              │   │
│           │  │ 5. 장기투자왕      +54.32%    👁              │   │
│           │  │ 6. 분산투자러      +48.76%    👁              │   │
│           │  │ ...                                          │   │
│           │  └──────────────────────────────────────────────┘   │
└───────────┴──────────────────────────────────────────────────────┘
```

**구성 요소:**
- 기간 선택 탭
- 포디움 (Top 3)
- 전체 순위 리스트
- 사용자 프로필 보기 버튼

#### 4.2.7 마이페이지 (`/mypage`)

```
┌──────────────────────────────────────────────────────────────────┐
│ [Header]                                                         │
├───────────┬──────────────────────────────────────────────────────┤
│           │  ┌──────────────────────────────────────────────┐   │
│  Sidebar  │  │  [커버 이미지]                                │   │
│           │  │  [아바타] 투자도사  🔥화속성  Rank #42       │   │
│           │  │           2,500 코인  +6.65%                 │   │
│           │  └──────────────────────────────────────────────┘   │
│           │                                                      │
│           │  [개요] [인벤토리] [아바타 꾸미기] [설정]            │
│           │                                                      │
│           │  ┌──────────────────────────────────────────────┐   │
│           │  │ 개요 탭:                                      │   │
│           │  │ ┌────────┬────────┬────────┬────────┐        │   │
│           │  │ │ 예수금  │ 평가금액│ 실현수익│ 보유아이템│       │   │
│           │  │ └────────┴────────┴────────┴────────┘        │   │
│           │  │                                              │   │
│           │  │ [보유 종목 리스트]                            │   │
│           │  └──────────────────────────────────────────────┘   │
└───────────┴──────────────────────────────────────────────────────┘
```

**구성 요소:**
- 프로필 헤더 (아바타, 정보, 통계)
- 탭 네비게이션 (개요, 인벤토리, 아바타, 설정)
- 각 탭별 콘텐츠

#### 4.2.8 배당금 계산기 페이지 (`/calculator`)

```
┌──────────────────────────────────────────────────────────────────┐
│ [Header]                                                         │
├───────────┬──────────────────────────────────────────────────────┤
│           │  📊 배당금 계산기                                     │
│  Sidebar  │                                                      │
│           │  ┌──────────────────────────────────────────────┐   │
│           │  │ 요약                                          │   │
│           │  │ ┌────────┬────────┬────────┬────────┐        │   │
│           │  │ │연간배당 │ 원천세  │ 실수령  │ 평균수익│       │   │
│           │  │ │$156.40 │ $23.46 │$132.94 │ 2.34% │        │   │
│           │  │ └────────┴────────┴────────┴────────┘        │   │
│           │  └──────────────────────────────────────────────┘   │
│           │                                                      │
│           │  ┌──────────────────────────────────────────────┐   │
│           │  │ 종목별 상세                                   │   │
│           │  │ ┌────────────────────────────────────────┐   │   │
│           │  │ │ AAPL  20주  $0.96  $19.20  2026-02-09 │   │   │
│           │  │ │ MSFT  10주  $3.00  $30.00  2026-03-15 │   │   │
│           │  │ └────────────────────────────────────────┘   │   │
│           │  └──────────────────────────────────────────────┘   │
└───────────┴──────────────────────────────────────────────────────┘
```

**구성 요소:**
- 배당금 요약 카드
- 종목별 배당 상세 테이블
- 다음 배당일 캘린더

---

## 5. 컴포넌트 설계

### 5.1 공통 컴포넌트

#### `components/common/loading-spinner.tsx`
- **용도**: 데이터 로딩 중 표시
- **Props**: `size`, `color`, `text`
- **사용처**: 모든 비동기 작업

#### `components/common/skeleton-card.tsx`
- **용도**: 초기 로딩 시 콘텐츠 플레이스홀더
- **Props**: `width`, `height`, `className`
- **사용처**: 대시보드 위젯, 리스트

#### `components/common/empty-state.tsx`
- **용도**: 데이터가 없을 때 표시
- **Props**: `icon`, `title`, `description`, `action`
- **사용처**: 포트폴리오, 관심종목, 인벤토리

#### `components/common/error-boundary.tsx`
- **용도**: 컴포넌트 에러 캐치
- **Props**: `fallback`, `onError`
- **사용처**: 페이지/위젯 래핑

#### `components/common/price-badge.tsx`
- **용도**: 가격 변동 표시 (+/-%)
- **Props**: `value`, `percent`, `size`
- **사용처**: 주가, 수익률 표시

### 5.2 인증 컴포넌트 🆕 업데이트

#### `components/auth/login-form.tsx` 🆕 업데이트
- **용도**: 로그인 폼 (일반 + OAuth)
- **기능**:
  - 🆕 이메일/비밀번호 입력 폼
  - 🆕 일반 로그인 버튼
  - Google OAuth 버튼
  - 🆕 Kakao OAuth 버튼
  - 🆕 회원가입 링크
- **연동**: `POST /api/v1/auth/login`, `next-auth`

#### `components/auth/signup-form.tsx` 🆕
- **용도**: 일반 회원가입 폼
- **기능**:
  - 닉네임 입력 (필수)
  - 이메일 입력 (필수, 중복 체크)
  - 비밀번호 입력 (필수, 8자 이상)
  - 비밀번호 확인 (일치 검증)
  - 회원가입 버튼
  - 로그인 페이지 링크
- **연동**: `POST /api/v1/auth/signup`

#### `components/auth/onboarding-form.tsx`
- **용도**: 신규 유저 온보딩 (사주 입력)
- **기능**: 생년월일 선택 → 사주 계산 → 결과 표시
- **연동**: `POST /api/v1/user/onboarding`

#### `components/auth/protected-route.tsx`
- **용도**: 인증 필요 페이지 보호
- **기능**: 미인증 시 `/login` 리다이렉트
- **사용처**: `(main)` 레이아웃

### 5.3 대시보드 컴포넌트

#### `components/dashboard/candle-chart.tsx` 🆕
- **라이브러리**: `lightweight-charts`
- **기능**:
  - OHLC 캔들스틱
  - 거래량 바 차트
  - 이동평균선 (MA5, MA20)
  - 시간 범위 선택
  - 실시간 업데이트 (WebSocket)
- **Props**: `ticker`, `resolution`, `onPriceUpdate`

#### `components/dashboard/stock-search.tsx` 🆕
- **기능**:
  - 자동완성 검색
  - 최근 검색 기록
  - 관심종목 추가 버튼
- **연동**: `GET /api/v1/stock/search`

#### `components/dashboard/notification-dropdown.tsx` 🆕
- **기능**:
  - 알림 목록 드롭다운
  - 읽음/안읽음 상태
  - 알림 타입별 아이콘
  - 전체 읽음 처리
- **연동**: `GET /api/v1/notifications`

#### `components/dashboard/dividend-calculator.tsx` 🆕
- **기능**:
  - 포트폴리오 기반 배당 계산
  - 세금 계산 (15%)
  - 다음 배당일 표시
- **연동**: `GET /api/v1/calc/dividend`

### 5.4 컴포넌트 Props 인터페이스

```typescript
// 예시: 차트 컴포넌트
interface CandleChartProps {
  ticker: string;
  resolution: '1' | '5' | '15' | '30' | '60' | 'D' | 'W' | 'M';
  height?: number;
  showVolume?: boolean;
  showMA?: boolean;
  onPriceUpdate?: (price: number) => void;
}

// 예시: 거래 패널
interface TradePanelProps {
  ticker: string;
  currentPrice: number;
  onOrderSubmit?: (order: TradeOrder) => void;
}

// 예시: 알림 드롭다운
interface NotificationDropdownProps {
  notifications: Notification[];
  onRead: (id: string) => void;
  onReadAll: () => void;
}
```

---

## 6. 상태 관리 설계

### 6.1 Zustand 스토어 구조

#### `stores/auth-store.ts`

```typescript
interface AuthState {
  // 상태
  user: User | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  
  // 액션
  setUser: (user: User) => void;
  setToken: (token: string) => void;
  logout: () => void;
  checkAuth: () => Promise<boolean>;
}
```

#### `stores/user-store.ts`

```typescript
interface UserState {
  // 상태
  profile: UserProfile | null;
  wallet: Wallet | null;
  saju: SajuInfo | null;
  
  // 액션
  setProfile: (profile: UserProfile) => void;
  setWallet: (wallet: Wallet) => void;
  updateWallet: (partial: Partial<Wallet>) => void;
  fetchUserData: () => Promise<void>;
}
```

#### `stores/stock-store.ts`

```typescript
interface StockState {
  // 상태
  prices: Map<string, StockPrice>;
  watchlist: string[];
  selectedTicker: string | null;
  
  // 액션
  updatePrice: (ticker: string, price: StockPrice) => void;
  setWatchlist: (tickers: string[]) => void;
  addToWatchlist: (ticker: string) => void;
  removeFromWatchlist: (ticker: string) => void;
  selectTicker: (ticker: string) => void;
}
```

#### `stores/portfolio-store.ts`

```typescript
interface PortfolioState {
  // 상태
  holdings: PortfolioItem[];
  totalValue: number;
  totalProfit: number;
  totalProfitPercent: number;
  isLoading: boolean;
  
  // 액션
  setHoldings: (holdings: PortfolioItem[]) => void;
  updateHolding: (ticker: string, data: Partial<PortfolioItem>) => void;
  recalculateTotals: () => void;
  fetchPortfolio: () => Promise<void>;
}
```

#### `stores/notification-store.ts`

```typescript
interface NotificationState {
  // 상태
  notifications: Notification[];
  unreadCount: number;
  
  // 액션
  setNotifications: (notifications: Notification[]) => void;
  addNotification: (notification: Notification) => void;
  markAsRead: (id: string) => void;
  markAllAsRead: () => void;
  fetchNotifications: () => Promise<void>;
}
```

#### `stores/ui-store.ts`

```typescript
interface UIState {
  // 상태
  sidebarOpen: boolean;
  editMode: boolean;
  selectedModal: string | null;
  
  // 액션
  toggleSidebar: () => void;
  setEditMode: (mode: boolean) => void;
  openModal: (modalId: string) => void;
  closeModal: () => void;
}
```

### 6.2 스토어 사용 패턴

```typescript
// 컴포넌트에서 사용
const { user, isAuthenticated } = useAuthStore();
const { prices, updatePrice } = useStockStore();

// 선택적 구독 (리렌더 최적화)
const user = useAuthStore((state) => state.user);
```

---

## 7. API 통신 레이어

### 7.1 Axios 인스턴스 설정

#### `lib/api/index.ts`

```typescript
// Axios 인스턴스 생성
// - baseURL 설정
// - timeout 설정
// - 기본 헤더 설정

// Request 인터셉터
// - JWT 토큰 자동 첨부
// - 요청 로깅

// Response 인터셉터
// - 에러 처리 (401, 403, 500)
// - 토큰 갱신 로직
// - 응답 데이터 정규화
```

### 7.2 에러 응답 처리

#### 백엔드 에러 응답 형식

백엔드에서 반환하는 모든 에러는 다음 형식의 `ErrorResponse`로 일관되게 전달됩니다:

```json
{
  "timestamp": "2026-01-17T12:00:00",
  "status": 400,
  "error": "TRADE_001",
  "message": "잔고가 부족합니다."
}
```

#### 에러 타입 정의 (`types/api.ts`)

```typescript
// 백엔드 에러 응답 타입
interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;   // 에러 코드 (TRADE_001 등)
  message: string; // 상세 메시지
}

// 에러 코드 enum (프론트엔드용)
enum ErrorCode {
  // 인증
  AUTH_EXPIRED_TOKEN = 'AUTH_001',
  AUTH_INVALID_TOKEN = 'AUTH_002',
  AUTH_ACCESS_DENIED = 'AUTH_003',
  AUTH_USER_NOT_FOUND = 'AUTH_004',
  AUTH_GOOGLE_TOKEN_INVALID = 'AUTH_005',
  AUTH_KAKAO_TOKEN_INVALID = 'AUTH_006',   // 🆕 카카오 토큰 검증 실패
  AUTH_PASSWORD_MISMATCH = 'AUTH_007',     // 🆕 비밀번호 불일치
  AUTH_EMAIL_DUPLICATE = 'AUTH_008',       // 🆕 이메일 중복
  // 거래
  TRADE_INSUFFICIENT_BALANCE = 'TRADE_001',
  TRADE_INSUFFICIENT_QUANTITY = 'TRADE_002',
  TRADE_MARKET_CLOSED = 'TRADE_003',
  TRADE_INVALID_TICKER = 'TRADE_004',
  // 게임
  GAME_INSUFFICIENT_COIN = 'GAME_001',
  GAME_ITEM_ALREADY_OWNED = 'GAME_002',
  GAME_ITEM_NOT_FOUND = 'GAME_003',
  // 사용자
  USER_NOT_FOUND = 'USER_001',
  USER_ALREADY_EXISTS = 'USER_002',
  // 서버
  INTERNAL_SERVER_ERROR = 'SERVER_001',
  EXTERNAL_API_ERROR = 'SERVER_002',
  // 검증
  VALIDATION_ERROR = 'VALIDATION_ERROR',
  BIND_ERROR = 'BIND_ERROR',
}
```

#### 에러 처리 예시 (Axios 인터셉터)

```typescript
// lib/api/index.ts
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const errorResponse: ErrorResponse = error.response?.data;
    
    switch (errorResponse?.error) {
      case 'AUTH_001':
      case 'AUTH_002':
        // 토큰 만료/무효 → 로그인 페이지로 리다이렉트
        authStore.getState().logout();
        router.push('/login');
        break;
      case 'AUTH_003':
        // 권한 없음 → 토스트 표시
        toast.error('접근 권한이 없습니다.');
        break;
      case 'TRADE_001':
        // 잔고 부족 → 토스트 표시
        toast.error('잔고가 부족합니다.');
        break;
      case 'GAME_001':
        // 코인 부족 → 코인 충전 유도
        toast.error('코인이 부족합니다. 수익을 코인으로 변환해보세요!');
        break;
      default:
        // 기타 에러 → 일반 에러 메시지
        toast.error(errorResponse?.message || '오류가 발생했습니다.');
    }
    
    return Promise.reject(error);
  }
);
```

### 7.3 API 모듈 구조

#### `lib/api/auth.ts`
| 함수 | 메서드 | 엔드포인트 |
|------|--------|-----------|
| `signup(email, password, nickname)` | POST | `/api/v1/auth/signup` | 🆕 일반 회원가입 |
| `login(email, password)` | POST | `/api/v1/auth/login` | 🆕 일반 로그인 |
| `loginWithGoogle(idToken)` | POST | `/api/v1/auth/oauth/google` | Google OAuth |
| `loginWithKakao(accessToken)` | POST | `/api/v1/auth/oauth/kakao` | 🆕 Kakao OAuth |
| `refreshToken()` | POST | `/api/v1/auth/refresh` |
| `logout()` | POST | `/api/v1/auth/logout` |
| `getCurrentUser()` | GET | `/api/v1/auth/me` |

#### `lib/api/user.ts`
| 함수 | 메서드 | 엔드포인트 |
|------|--------|-----------|
| `getMe()` | GET | `/api/v1/user/me` |
| `updateMe(data)` | PUT | `/api/v1/user/me` |
| `onboarding(data)` | POST | `/api/v1/user/onboarding` |
| `getWallet()` | GET | `/api/v1/user/wallet` |

#### `lib/api/trade.ts`
| 함수 | 메서드 | 엔드포인트 |
|------|--------|-----------|
| `submitOrder(order)` | POST | `/api/v1/trade/order` |
| `getPortfolio()` | GET | `/api/v1/trade/portfolio` |
| `getTradeHistory(params)` | GET | `/api/v1/trade/history` |
| `getHolding(ticker)` | GET | `/api/v1/trade/portfolio/{ticker}` |

#### `lib/api/stock.ts`
| 함수 | 메서드 | 엔드포인트 |
|------|--------|-----------|
| `getQuote(ticker)` | GET | `/api/v1/stock/quote/{ticker}` |
| `getCandles(ticker, params)` | GET | `/api/v1/stock/candles/{ticker}` |
| `searchStocks(query)` | GET | `/api/v1/stock/search` |
| `getProfile(ticker)` | GET | `/api/v1/stock/profile/{ticker}` |
| `getWatchlist()` | GET | `/api/v1/watchlist` |
| `addToWatchlist(ticker)` | POST | `/api/v1/watchlist` |
| `removeFromWatchlist(ticker)` | DELETE | `/api/v1/watchlist/{ticker}` |

#### `lib/api/game.ts`
| 함수 | 메서드 | 엔드포인트 |
|------|--------|-----------|
| `spin(count)` | POST | `/api/v1/game/gacha` |
| `getItems()` | GET | `/api/v1/game/items` |
| `getInventory()` | GET | `/api/v1/game/inventory` |
| `equipItem(itemId)` | PUT | `/api/v1/game/equip/{itemId}` |
| `getRanking(period)` | GET | `/api/v1/game/ranking` |
| `convertProfit()` | POST | `/api/v1/game/convert-profit` |

#### `lib/api/chat.ts`
| 함수 | 메서드 | 엔드포인트 |
|------|--------|-----------|
| `askOracle(message, context)` | POST (SSE) | `/api/v1/chat/ask` |
| `getHistory(sessionId)` | GET | `/api/v1/chat/history` |
| `deleteSession(sessionId)` | DELETE | `/api/v1/chat/session/{sessionId}` |

#### `lib/api/notification.ts`
| 함수 | 메서드 | 엔드포인트 |
|------|--------|-----------|
| `getNotifications()` | GET | `/api/v1/notifications` |
| `markAsRead(id)` | PUT | `/api/v1/notifications/{id}/read` |
| `markAllAsRead()` | PUT | `/api/v1/notifications/read-all` |

---

## 8. 실시간 통신 설계

### 8.1 STOMP WebSocket 클라이언트

#### `lib/stomp-client.ts`

```typescript
// 설정
// - WebSocket URL
// - SockJS 폴백
// - 재연결 로직
// - 하트비트

// 주요 기능
// - connect(): 연결
// - disconnect(): 연결 해제
// - subscribe(destination, callback): 구독
// - unsubscribe(subscriptionId): 구독 해제
// - send(destination, body): 메시지 전송
```

### 8.2 WebSocket 훅

#### `hooks/use-websocket.ts`

```typescript
// 기능
// - 연결 상태 관리
// - 자동 재연결
// - 구독 관리

// 반환값
// - isConnected: boolean
// - connect: () => void
// - disconnect: () => void
// - subscribe: (channel, callback) => unsubscribe
```

#### `hooks/use-stock-price.ts`

```typescript
// 기능
// - 종목별 가격 구독
// - 스토어 자동 업데이트
// - 구독/해제 최적화

// 사용법
// const price = useStockPrice('AAPL');
// const prices = useStockPrices(['AAPL', 'TSLA', 'NVDA']);
```

### 8.3 구독 채널

| 채널 | 용도 | 메시지 형식 |
|------|------|------------|
| `/topic/stock.{ticker}` | 종목 시세 | `{ticker, price, change, volume}` |
| `/user/queue/trade` | 체결 알림 | `{orderId, type, ticker, ...}` |
| `/user/queue/notification` | 개인 알림 | `{type, title, message}` |
| `/topic/ranking` | 랭킹 변동 | `{rankings: [...]}` |

---

## 9. 인증 시스템

### 9.1 next-auth 설정

#### `app/api/auth/[...nextauth]/route.ts`

```typescript
// Google OAuth Provider 설정
// JWT 콜백 (토큰 처리)
// Session 콜백 (세션 확장)
// 커스텀 페이지 (/login)
```

### 9.2 인증 미들웨어

#### `middleware.ts`

```typescript
// 보호 경로 정의
// - /, /oracle, /gacha, /ranking, /mypage, /calculator → 인증 필요
// - /login → 미인증만 접근 가능

// 리다이렉트 로직
// - 미인증 + 보호경로 → /login
// - 인증 + /login → /
// - 인증 + 온보딩 미완료 → /onboarding
```

### 9.3 인증 Provider

#### `providers/auth-provider.tsx`

```typescript
// SessionProvider 래핑
// 초기 인증 상태 확인
// 스토어 동기화
```

---

## 10. UI/UX 시스템

### 10.1 디자인 토큰

#### 색상
```css
--primary: oklch(0.75 0.18 85);      /* 금색 */
--accent: oklch(0.65 0.2 145);       /* 초록 (상승) */
--bull: oklch(0.65 0.2 145);         /* 상승 */
--bear: oklch(0.55 0.22 25);         /* 하락 */
--gold: oklch(0.8 0.16 85);          /* 황금 */
--oracle: oklch(0.6 0.18 280);       /* AI 도사 */
```

#### 희귀도 색상
```css
--rarity-common: #6b7280;
--rarity-rare: #3b82f6;
--rarity-epic: #a855f7;
--rarity-legendary: #f59e0b;
```

### 10.2 애니메이션

| 애니메이션 | 트리거 | 적용 대상 |
|-----------|--------|----------|
| `pulse-gold` | 수익률 +5% | 아바타 테두리 |
| `float` | 상시 | 아바타 |
| `shimmer` | 로딩 | 스켈레톤 |
| `trade-success` | 체결 성공 | 거래 버튼 |
| `shake` | 수익률 -5% | 아바타 |
| `bounce` | 가챠 | 캡슐 |

### 10.3 반응형 설계

| 브레이크포인트 | 너비 | 레이아웃 변경 |
|--------------|------|-------------|
| Mobile | < 640px | 단일 컬럼, 사이드바 숨김 |
| Tablet | 641-1024px | 2 컬럼 그리드 |
| Desktop | > 1024px | 4 컬럼 그리드, 사이드바 표시 |

### 10.4 접근성

- **키보드 내비게이션**: 모든 인터랙티브 요소
- **ARIA 라벨**: 버튼, 입력, 모달
- **포커스 링**: 가시적 포커스 상태
- **색상 대비**: WCAG AA 준수

---

## 11. 테스트 전략

### 11.1 테스트 파일 구조

```
__tests__/
├── unit/                    # 단위 테스트
│   ├── saju-calculator.test.ts
│   ├── utils.test.ts
│   └── stores/
│       ├── auth-store.test.ts
│       └── portfolio-store.test.ts
│
├── components/              # 컴포넌트 테스트
│   ├── trade-panel.test.tsx
│   ├── candle-chart.test.tsx
│   └── notification-dropdown.test.tsx
│
└── e2e/                     # E2E 테스트 (Playwright)
    ├── auth.spec.ts         # 로그인 → 온보딩
    ├── trade.spec.ts        # 검색 → 매수 → 확인
    ├── gacha.spec.ts        # 가챠 뽑기
    └── oracle.spec.ts       # AI 상담
```

### 11.2 테스트 시나리오

#### E2E 시나리오

1. **인증 플로우**
   - Google 로그인 → 온보딩 → 대시보드

2. **거래 플로우**
   - 종목 검색 → 차트 확인 → 매수 → 포트폴리오 확인

3. **가챠 플로우**
   - 코인 확인 → 가챠 뽑기 → 결과 확인 → 인벤토리 확인

4. **AI 상담 플로우**
   - 질문 입력 → 스트리밍 응답 → 대화 이력 확인

---

## 12. 개발 로드맵

### Phase 1: 인프라 구축 (Week 1)

| 태스크 | 파일 | 설명 |
|--------|------|------|
| 패키지 설치 | `package.json` | zustand, axios, next-auth 등 |
| Zustand 스토어 | `stores/*.ts` | 5개 스토어 생성 |
| API 클라이언트 | `lib/api/*.ts` | Axios + 모듈 분리 |
| 타입 정의 | `types/*.ts` | 모든 타입 정의 |
| 인증 설정 | `next-auth`, `middleware.ts` | OAuth + 미들웨어 |

### Phase 2: 인증 및 온보딩 (Week 1-2)

| 태스크 | 파일 | 설명 |
|--------|------|------|
| 로그인 페이지 | `app/login/page.tsx` | Google OAuth |
| 온보딩 페이지 | `app/onboarding/page.tsx` | 생년월일 + 사주 |
| 사주 계산기 | `lib/saju-calculator.ts` | 오행/띠 계산 |
| 보호 라우트 | `middleware.ts` | 인증 미들웨어 |

### Phase 3: 실시간 시스템 (Week 2)

| 태스크 | 파일 | 설명 |
|--------|------|------|
| STOMP 클라이언트 | `lib/stomp-client.ts` | WebSocket 연결 |
| 가격 구독 훅 | `hooks/use-stock-price.ts` | 실시간 가격 |
| 캔들 차트 | `components/dashboard/candle-chart.tsx` | lightweight-charts |
| 차트 교체 | 기존 `stock-chart.tsx` 수정 | Area → Candle |

### Phase 4: 거래 연동 (Week 2-3)

| 태스크 | 파일 | 설명 |
|--------|------|------|
| 거래 패널 연동 | `trade-panel.tsx` 수정 | API 연동 |
| 포트폴리오 연동 | `portfolio-summary.tsx` 수정 | 실시간 업데이트 |
| 관심종목 연동 | `watchlist.tsx` 수정 | CRUD |
| 종목 검색 | `stock-search.tsx` 신규 | 자동완성 |

### Phase 5: AI 시스템 (Week 3)

| 태스크 | 파일 | 설명 |
|--------|------|------|
| SSE 훅 | `hooks/use-ai-chat.ts` | 스트리밍 응답 |
| 챗봇 연동 | `ai-chatbot.tsx` 수정 | 실시간 타이핑 |
| 오라클 페이지 | `oracle/page.tsx` 수정 | 컨텍스트 주입 |

### Phase 6: 게임 연동 (Week 3-4)

| 태스크 | 파일 | 설명 |
|--------|------|------|
| 가챠 API 연동 | `gacha/page.tsx` 수정 | 서버 확률 |
| 인벤토리 연동 | `mypage/page.tsx` 수정 | 아이템 장착 |
| 랭킹 연동 | `ranking/page.tsx` 수정 | 실시간 랭킹 |
| 코인 변환 | 수익 → 코인 UI | 버튼 추가 |

### Phase 7: 고도화 (Week 4)

| 태스크 | 파일 | 설명 |
|--------|------|------|
| 알림 시스템 | `notification-dropdown.tsx` | 드롭다운 |
| 배당금 계산기 | `calculator/page.tsx` | 신규 페이지 |
| 모바일 최적화 | 전체 | 반응형 |
| 성능 최적화 | 번들, 렌더링 | 최적화 |

### Phase 8: 테스트 및 배포 (Week 5)

| 태스크 | 파일 | 설명 |
|--------|------|------|
| 단위 테스트 | `__tests__/unit/*` | Jest |
| E2E 테스트 | `__tests__/e2e/*` | Playwright |
| 환경 변수 | `.env.*` | 배포 설정 |
| Vercel 배포 | - | 프로덕션 |

---

## 📎 부록

### A. 환경 변수

```env
# .env.local
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws
NEXTAUTH_URL=http://localhost:3000
NEXTAUTH_SECRET=your-secret-key

# Google OAuth
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# 🆕 Kakao OAuth
KAKAO_CLIENT_ID=your-kakao-client-id
KAKAO_CLIENT_SECRET=your-kakao-client-secret
```

### B. 필수 패키지 설치 명령어

```bash
pnpm add zustand @stomp/stompjs sockjs-client axios next-auth lightweight-charts dayjs immer

pnpm add -D @types/sockjs-client @playwright/test
```

---

**문서 버전:** 1.2 (🆕 카카오 OAuth, 일반 회원가입/로그인 추가)  
**최종 수정일:** 2026-01-17  
**작성자:** MadCamp02 개발팀
