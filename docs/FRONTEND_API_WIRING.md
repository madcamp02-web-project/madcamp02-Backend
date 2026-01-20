# MadCamp02 프론트엔드 API 연결 명세 (프론트 기준 정리본)

> 이 문서는 **현재 프론트엔드가 실제로 구현·연결한 상태**를 기준으로 정리한 문서입니다.  
> 프론트에서 이미 끝난 부분은 설명 위주로만 남기고,  
> **백엔드가 계약을 유지하거나 추가로 구현해야 할 부분만 명확히 표시**합니다.
>
> 최종 스펙의 단일 진실(Single Source of Truth)은 여전히 아래 문서들입니다.
>
> - `docs/FULL_SPECIFICATION.md`
> - `docs/BACKEND_DEVELOPMENT_PLAN.md`
> - `docs/FRONTEND_DEVELOPMENT_PLAN.md`

표기 규칙:

- ✅ **프론트 완료**: 프론트 코드 구현/연결이 끝난 항목 (백엔드는 계약만 맞춰주면 됨)
- 🧩 **백엔드 책임**: 백엔드에서 구현·유지해야 할 내용 (필수 계약)

---

## 1. 글로벌 규칙

### 1.1 HTTP 클라이언트 (`src/lib/api/index.ts`)

- ✅ **프론트 완료**
  - Axios 인스턴스:
    - `baseURL = process.env.NEXT_PUBLIC_API_URL`
    - `Content-Type: application/json`, `withCredentials: true`, `timeout: 30초`
  - 요청 인터셉터:
    - `useAuthStore.getState().token` 을 읽어 `Authorization: Bearer {accessToken}` 자동 주입
  - 응답 인터셉터 (401 처리):
    - `response.status === 401` 이고, 원 요청이 `/api/v1/auth/refresh` 가 아니며 `_retry` 가 false 인 경우:
      - `api.post('/api/v1/auth/refresh')` 로 refresh 시도
      - 성공 시 새 accessToken 을 `authStore`/`localStorage` 에 저장 후 원 요청 1회 재시도
      - 실패 시 `authStore.logout()` + `/login` 으로 이동
  - Market API 응답 헤더 파싱:
    - `url` 이 `/api/v1/market` 로 시작하는 경우에만
      - `X-Cache-Status`, `X-Cache-Age`, `X-Data-Freshness` 를 읽어
      - `response.cacheMetadata = { status, age, freshness }` 로 주입
    - 에러 응답(`error.response`) 에도 동일하게 `cacheMetadata` 주입

- 🧩 **백엔드 책임**
  - `/api/v1/auth/refresh` 엔드포인트:
    - 유효한 `refreshToken` 을 바탕으로 새 `accessToken`(+ 선택적 `refreshToken`) 을 반환해야 함.
    - 실패 시 적절한 HTTP 코드/에러 바디(`ErrorResponse`) 를 내려야 함.
  - `/api/v1/market/**` 계열:
    - 모든 응답에 아래 3개 헤더를 일관되게 포함:
      - `X-Cache-Status`: `"HIT" | "MISS" | "STALE"`
      - `X-Cache-Age`: `number` (초 단위)
      - `X-Data-Freshness`: `"FRESH" | "STALE" | "EXPIRED"`

### 1.2 온보딩 완료 판단 (`hasCompletedOnboarding`)

- ✅ **프론트 완료**
  - 헬퍼: `src/lib/utils.ts` 의 `hasCompletedOnboarding(user)`
    - 구현:
      - `!!user?.birthDate`
      - `&& (!!user?.sajuElement || !!user?.saju?.element)`
  - 레이아웃 가드: `components/auth/AuthGuard.tsx`
    - 마운트 시 `checkAuth()` 호출 → `/api/v1/auth/me` 로 `user` 채움
    - 인증 실패 시 `/login` 으로 리다이렉트
    - 인증 성공 + `hasCompletedOnboarding(user) === false` + 현재 경로가 `/onboarding` 이 아니면:
      - `/onboarding` 으로 `router.replace`

- 🧩 **백엔드 책임**
  - `GET /api/v1/auth/me` 응답에 다음 필드를 일관되게 포함:
    - `birthDate`
    - `sajuElement` 또는 `saju.element`  
  → 백엔드의 `User.hasCompletedOnboarding()` 규칙과 프론트 헬퍼가 의미상 일치하도록 유지.

### 1.3 공통 에러 포맷 (`src/lib/api/error.ts`)

- ✅ **프론트 완료**
  - `parseError(error): ParsedError`:
    - `status`: `error.response?.status`
    - `code`: `response.data.error || response.data.code`
    - `message`: `response.data.message || response.data.error_description || error.message || 기본 메시지`
    - `fieldErrors`: `response.data.fieldErrors || response.data.errors`

- 🧩 **백엔드 책임**
  - 에러 응답 포맷:
    - 가능하면 `ErrorResponse{ status, error, message, fieldErrors? }` 형태 유지
    - 온보딩 전용 에러 코드(`ONBOARDING_001~003`) 는 `error` 필드에 실어 보내야 함.

---

## 2. 인증/온보딩 플로우

대상 라우트:

- `/login`, `/signup`, `/oauth/callback`, `/onboarding`, `/mypage`

주요 파일:

- `app/login/page.tsx`
- `app/signup/page.tsx`
- `app/oauth/callback/page.tsx`
- `app/onboarding/page.tsx`
- `app/(main)/mypage/page.tsx`
- `stores/auth-store.ts`, `stores/user-store.ts`
- `lib/api/auth.ts`, `lib/api/user.ts`

### 2.1 Auth / User API 모듈

- ✅ **프론트 완료**
  - `src/lib/api/auth.ts`
    - `signup(payload)` → `POST /api/v1/auth/signup`
    - `login(payload)` → `POST /api/v1/auth/login`
    - `kakaoLogin(accessToken)` → `POST /api/v1/auth/oauth/kakao`
    - `googleLogin(idToken)` → `POST /api/v1/auth/oauth/google`
    - `refreshToken()` → `POST /api/v1/auth/refresh`
    - `me()` → `GET /api/v1/auth/me`
  - `src/lib/api/user.ts`
    - `submitOnboarding(body: OnboardingRequest)` → `POST /api/v1/user/onboarding`
    - 그 외 `getProfile`, `updateProfile`, `getWallet`, 워치리스트 관련 API 구현 완료.

- 🧩 **백엔드 책임**
  - 위 엔드포인트들의 **경로 / 메서드 / DTO / 에러 포맷** 을 문서와 일치하게 유지.
  - `/api/v1/user/onboarding`, `/api/v1/auth/me` 가 사주/온보딩 관련 필드를 일관되게 반환.

### 2.2 Auth / User 스토어

- ✅ **프론트 완료**
  - `src/stores/auth-store.ts`
    - 상태: `user`, `token`, `refreshToken`, `isAuthenticated`, `isLoading`, `error`
    - 메서드:
      - `login`, `signup`, `loginWithKakao`, `loginWithGoogle`, `loginAsGuest`
      - `checkAuth` → `authApi.me()` 호출 후 `user-store.fetchProfile/fetchInventory/fetchWallet` 병렬 호출
      - `logout`
  - `src/stores/user-store.ts`
    - `profile`, `wallet`, `items`, 공개설정/랭킹참여 여부 등 상태 + 관련 API 연동 구현.

- 🧩 **백엔드 책임**
  - `/api/v1/auth/me`, `/api/v1/user/me`, `/api/v1/user/wallet` 등의 응답 필드를 스키마대로 유지.

### 2.3 `/signup` → 자동 로그인 → 온보딩 강제

- ✅ **프론트 완료 (`app/signup/page.tsx`)**
  1. `authStore.signup(formData)` → `POST /api/v1/auth/signup`
  2. 성공 시 같은 자격으로 `authStore.login({ email, password })` → `POST /api/v1/auth/login`
  3. `checkAuth()` 로 `/api/v1/auth/me` 재조회
  4. `hasCompletedOnboarding(user)` 결과에 따라:
     - 미완료: `/onboarding`
     - 완료: `/`

- 🧩 **백엔드 책임**
  - 가입 직후 동일 자격증명으로 `/auth/login` 이 정상 동작해야 함.

### 2.4 `/login` + 소셜 로그인

- ✅ **프론트 완료**
  - 이메일 로그인 (`app/login/page.tsx`)
    - `login({ email, password })` → `checkAuth()` → `hasCompletedOnboarding(user)` 로 `/onboarding` 또는 `/` 분기.
  - Kakao/Google Backend-Driven(Web)
    - 버튼 클릭 시:
      - `window.location.href = {BACKEND_URL}/oauth2/authorization/kakao|google`
  - Kakao/Google Frontend-Driven(SDK)
    - SDK 로 토큰 획득 후:
      - `loginWithKakao(accessToken)` / `loginWithGoogle(idToken)`
      - 응답 `isNewUser` + `hasCompletedOnboarding(user)` 로 온보딩 필요 여부 결정.

- 🧩 **백엔드 책임**
  - OAuth2 로그인 성공 시:
    - `/oauth/callback?accessToken=...&refreshToken=...&isNewUser=true|false` 로 리다이렉트.
  - `POST /api/v1/auth/oauth/kakao|google`:
    - 토큰 검증 후 `accessToken`, `refreshToken?`, `isNewUser` 를 포함한 응답 반환.

### 2.5 `/oauth/callback` 플로우

- ✅ **프론트 완료 (`app/oauth/callback/page.tsx`)**
  - 쿼리 파라미터:
    - `accessToken`, `refreshToken`, `isNewUser`, `error`
  - 처리:
    - 토큰을 `localStorage` 및 `authStore` 에 저장 후 `checkAuth()`
    - `needOnboarding = isNewUser === "true" || !hasCompletedOnboarding(user)`
    - `needOnboarding` 이면 `/onboarding`, 아니면 `/`

- 🧩 **백엔드 책임**
  - 리다이렉트 URL 의 쿼리 키 이름(`accessToken`, `refreshToken`, `isNewUser`, `error`) 을 변경하지 않도록 유지.

### 2.6 `/onboarding` 페이지 & 에러 코드(ONBOARDING_001~003)

- ✅ **프론트 완료 (`app/onboarding/page.tsx`)**
  - 입력 필드:
    - `nickname`, `birthDate`, `birthTime`, `gender`, `calendarType`, `investmentStyle`
  - 제출:
    - `userApi.submitOnboarding({ nickname, birthDate, birthTime?, gender, calendarType })`
    - 성공 시 `checkAuth()` 재호출 후 `/` 로 이동.
  - 에러 처리(`parseError` 사용):
    - `ONBOARDING_001`:
      - 입력값 유효성 에러 → 각 필드 옆에 메시지 표시 (`fieldErrors` 기반, 없으면 기본 안내)
    - `ONBOARDING_002`:
      - 상단 경고: “음력/윤달 변환 중 문제가 발생했습니다. 달력 종류와 생년월일을 다시 확인해주세요.”
    - `ONBOARDING_003`:
      - 상단 경고: “일시적인 오류입니다. 잠시 후 다시 시도해주세요.”

- 🧩 **백엔드 책임**
  - `POST /api/v1/user/onboarding` 에서 다음 에러 규약을 지킬 것:
    - `error = "ONBOARDING_001"`:
      - 가능한 경우 `fieldErrors` 에 필드별 메시지 포함 (`birthDate`, `birthTime`, `gender`, `calendarType` 등)
    - `error = "ONBOARDING_002"`:
      - 음력/윤달 변환 실패
    - `error = "ONBOARDING_003"`:
      - 그 외 사주 계산 예외

### 2.7 마이페이지 재온보딩 (`/mypage`)

- ✅ **프론트 완료 (`app/(main)/mypage/page.tsx`)**
  - 사주 관련 입력:
    - `birthDate`, `birthTime`, `gender`, `calendarType`, 현재 사주/띠 표시
  - “사주 다시 계산하기”:
    - `userApi.submitOnboarding({ nickname: profile.nickname, birthDate, birthTime?, gender, calendarType })`
    - 성공 후 `checkAuth()` 로 `/auth/me` 및 `user-store` 자동 동기화.

- 🧩 **백엔드 책임**
  - `/api/v1/user/onboarding` 이 **idempotent** 하게 동작:
    - 같은 유저가 여러 번 호출해도 사주 결과가 일관되고, 데이터 무결성이 깨지지 않도록 보장.

---

## 3. 마켓/캐시 헤더 (`/market`)

대상 라우트: `/market`  
주요 파일:

- `app/(main)/market/page.tsx`
- `stores/stock-store.ts`
- `lib/api/stock.ts`

### 3.1 Stock API & 스토어

- ✅ **프론트 완료**
  - `src/lib/api/stock.ts`
    - `getIndices()` → `GET /api/v1/market/indices`
    - `getNews()` → `GET /api/v1/market/news`
    - `getMovers()` → `GET /api/v1/market/movers`
  - `src/stores/stock-store.ts`
    - 상태:
      - `backendCache.indices|movers|news: { status: "HIT"|"MISS"|"STALE"|null; age: number|null; freshness: "FRESH"|"STALE"|"EXPIRED"|null }`
      - `isUsingCache.indices|movers|news` (프론트 localStorage 캐시 여부)
    - `fetchIndices / fetchMovers / fetchNews`:
      - 우선 localStorage 캐시를 UI 에 즉시 적용
      - API 응답의 `cacheMetadata` 를 읽어 `backendCache` 를 갱신
      - STALE 응답도 캐시 메타와 함께 반영

- 🧩 **백엔드 책임**
  - `/api/v1/market/indices|news|movers` 의 DTO/필드 구조를 스펙과 일치하게 유지.
  - 헤더 3종(`X-Cache-Status`, `X-Cache-Age`, `X-Data-Freshness`) 을 항상 포함하며, 실제 캐시 상태와 의미 있게 매핑.

### 3.2 `/market` 페이지 캐시 배지 UI

- ✅ **프론트 완료 (`app/(main)/market/page.tsx`)**
  - 헤더 우측 Cache 배지:
    - `backendCache.indices` 기준
    - 예: `HIT · 12s · FRESH`
  - 상단 안내 배너:
    - 백엔드 캐시가 `STALE` 인 경우:
      - “캐시된 데이터를 표시 중입니다. 최신 데이터를 불러오는 중입니다.”
    - 프론트 localStorage 캐시만 사용 중이고 백엔드 응답이 없는 경우:
      - “로컬 캐시 데이터를 표시 중입니다. 서버 연결을 확인하는 중입니다.”

- 🧩 **백엔드 책임**
  - 실제 캐시 시스템(Redis/DB) 상태와 헤더 값의 의미가 최대한 일치하도록 유지 (프론트는 단순 노출).

---

## 4. `/calculator` 페이지 & Calc API

대상 라우트: `/calculator`  
주요 파일:

- `app/(main)/calculator/page.tsx`
- `src/lib/api/calc.ts`

### 4.1 `/calculator` 페이지 (UI/UX)

- ✅ **프론트 완료 (`app/(main)/calculator/page.tsx`)**
  - 탭:
    - `배당 계산`(dividend)
    - `세금 계산`(tax)
  - 상태 관리:
    - 각 탭별 입력/응답/로딩/에러를 모두 로컬 `useState` 로 관리 (Zustand 스토어 없음)
    - 탭 전환 시 입력값은 유지 (정책 주석으로 명시)
  - 배당 탭:
    - 입력:
      - `배당 수익률(%)` → `assumedDividendYield = percent / 100`
      - `주당 배당액` → `dividendPerShare`
      - `세율(%)` → `taxRate = percent / 100`
    - 호출:
      - `calcApi.getDividend({ assumedDividendYield, dividendPerShare, taxRate })`
    - 응답(`CalcDividendResponse`) 표시:
      - `totalDividend`, `withholdingTax`, `netDividend` 를 카드 3개로 요약
  - 세금 탭:
    - 입력:
      - `세율(%)` → `taxRate = percent / 100`
    - 호출:
      - `calcApi.getTax({ taxRate })`
    - 응답(`CalcTaxResponse`) 표시:
      - `realizedProfit`, `taxBase`, `estimatedTax` 를 카드 3개로 요약
  - 통화:
    - `currency` 는 1차 버전에서 `null` 이라는 가정 하에, 통화 기호 없이 숫자만 포맷팅.

### 4.2 Calc API 모듈 (`src/lib/api/calc.ts`)

- ✅ **프론트 완료**

```ts
export interface CalcDividendResponse {
  totalDividend: number;
  withholdingTax: number;
  netDividend: number;
  currency: string | null;
}

export interface CalcTaxResponse {
  realizedProfit: number;
  taxBase: number;
  estimatedTax: number;
  currency: string | null;
}

export interface GetDividendParams {
  assumedDividendYield?: number;
  dividendPerShare?: number;
  taxRate?: number;
}

export interface GetTaxParams {
  taxRate?: number;
}

export const calcApi = {
  async getDividend(params: GetDividendParams): Promise<CalcDividendResponse> {
    const { data } = await api.get<CalcDividendResponse>("/api/v1/calc/dividend", { params });
    return data;
  },
  async getTax(params: GetTaxParams): Promise<CalcTaxResponse> {
    const { data } = await api.get<CalcTaxResponse>("/api/v1/calc/tax", { params });
    return data;
  },
};
```

- 🧩 **백엔드 책임**
  - `GET /api/v1/calc/dividend`
    - Query:
      - `assumedDividendYield?: number`
      - `dividendPerShare?: number`
      - `taxRate?: number`
    - Response(`CalcDividendResponse`):
      - `totalDividend`, `withholdingTax`, `netDividend`, `currency(null)`
  - `GET /api/v1/calc/tax`
    - Query:
      - `taxRate?: number`
    - Response(`CalcTaxResponse`):
      - `realizedProfit`, `taxBase`, `estimatedTax`, `currency(null)`
  - 계산 로직(지갑/포트폴리오에서 실현 손익/총자산을 어떻게 읽어오는지, 세율·과세표준 처리 등)은 전적으로 백엔드 책임.

---

## 5. 유지보수 원칙 & 백엔드 TODO 요약

- ✅ **프론트는 이미 다음을 구현 완료**
  - HTTP 클라이언트/토큰/401 재시도/Market 캐시 헤더 파싱
  - `auth`, `user`, `stock`, `calc` API 모듈
  - `auth-store`, `user-store`, `stock-store` (마켓 캐시 메타 포함)
  - `/login`, `/signup`, `/oauth/callback`, `/onboarding`, `/mypage`, `/market`, `/calculator` 라우트 로직
  - 온보딩/재온보딩 플로우 및 에러 코드(`ONBOARDING_001~003`) 처리

- 🧩 **백엔드가 반드시 유지해야 할 핵심 계약 (요약)**
  1. 인증/토큰:
     - `/api/v1/auth/login`, `/api/v1/auth/refresh`, `/api/v1/auth/me` 의 DTO·에러 포맷 유지.
  2. 온보딩:
     - `/api/v1/user/onboarding` 의 온보딩 전용 에러 코드(`ONBOARDING_001~003`) 및 `fieldErrors` 포맷 유지.
     - `/api/v1/auth/me` 가 온보딩 필드(`birthDate`, `sajuElement` 또는 `saju.element`) 를 포함.
  3. 마켓:
     - `/api/v1/market/indices|news|movers` 의 DTO 와 캐시 헤더 3종(`X-Cache-Status`, `X-Cache-Age`, `X-Data-Freshness`) 유지.
  4. Calc:
     - `/api/v1/calc/dividend`, `/api/v1/calc/tax` 의 쿼리 파라미터/응답 스키마와 계산 규칙을 스펙과 일치하게 구현·보존.

- 이 문서는 앞으로 **“백엔드가 맞춰야 할 프론트 계약 체크리스트”** 로만 사용하면 됩니다.
  - 스펙 변경 시:
    1. 먼저 `FULL_SPECIFICATION.md` / `BACKEND_DEVELOPMENT_PLAN.md` 를 수정
    2. 그 후 이 파일의 🧩 항목들이 실제 구현과 어긋나지 않는지만 확인

