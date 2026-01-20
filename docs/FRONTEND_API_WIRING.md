# MadCamp02 프론트엔드 API 연결 명세 (초안)

## 1. 개요

- 이 문서는 **프론트 라우트/스토어 ↔ 백엔드 엔드포인트** 매핑을 한 곳에서 정리하기 위한 초안입니다.
- 최종 진실(Single Source of Truth)은 여전히
  - `docs/FULL_SPECIFICATION.md`
  - `docs/BACKEND_DEVELOPMENT_PLAN.md`
  - `docs/FRONTEND_DEVELOPMENT_PLAN.md`
  이며, 이 문서는 그 세 문서를 **프론트 개발자가 빠르게 탐색할 수 있는 인덱스/지도 역할**을 합니다.

## 2. 글로벌 규칙 (요약)

- **HTTP 클라이언트**
  - `src/lib/api/index.ts` 에서 Axios 인스턴스 공통 설정
  - `Authorization: Bearer {accessToken}` 자동 주입
  - 401 응답 시 `authApi.refresh`를 통해 토큰 재발급 후 한 번 재시도
  - Market API 응답 헤더
    - `X-Cache-Status` (`HIT`/`MISS`/`STALE`)
    - `X-Cache-Age` (초 단위)
    - `X-Data-Freshness` (`FRESH`/`STALE`/`EXPIRED`)
    - → `stock-store`에서 `backendCache` 메타(state)로 반영

- **온보딩 완료 판단 규칙**
  - 백엔드: `User.hasCompletedOnboarding() = (birthDate != null && sajuElement != null && !sajuElement.isBlank())`
  - 프론트: `hasCompletedOnboarding(user) = !!user?.birthDate && !!user?.sajuElement`
  - 온보딩 강제 규칙:
    - 일반 회원가입: `/signup → /api/v1/auth/signup → /api/v1/auth/login → /api/v1/auth/me → !hasCompletedOnboarding(user) 이면 /onboarding`
    - 소셜/OAuth: `/oauth/callback?accessToken&refreshToken&isNewUser → /api/v1/auth/me → needOnboarding = isNewUser === true || !hasCompletedOnboarding(user)`
    - 레이아웃 가드: 인증 상태에서 `hasCompletedOnboarding(user) === false` 이고 현재 경로가 `/onboarding`이 아니면 항상 `/onboarding`으로 `router.replace`

## 3. 라우트별 핵심 매핑 (요약/상세)

- **공통 참고**
  - 라우트 구조 및 상태 관리는 `FRONTEND_DEVELOPMENT_PLAN.md` 4~6장을,
  - 엔드포인트·DTO·에러 코드는 `FULL_SPECIFICATION.md` 5장을 단일 진실로 사용한다.

### 3.1 인증/온보딩 플로우 (`/login`, `/signup`, `/oauth/callback`, `/onboarding`)

- **주요 파일**
  - `app/login/page.tsx`
  - `app/signup/page.tsx`
  - `app/oauth/callback/page.tsx`
  - `app/onboarding/page.tsx`
  - `stores/auth-store.ts`, `stores/user-store.ts`
  - `lib/api/auth.ts`, `lib/api/user.ts`

- **백엔드 엔드포인트**
  - `POST /api/v1/auth/signup` — 일반 회원가입
  - `POST /api/v1/auth/login` — 일반 로그인
  - `POST /api/v1/auth/oauth/kakao`, `POST /api/v1/auth/oauth/google` — Frontend-Driven 소셜 로그인
  - `GET /api/v1/auth/me` — 현재 사용자 + 온보딩 상태 확인
  - `POST /api/v1/user/onboarding` — 온보딩/재온보딩 (정밀 사주 계산, idempotent)

- **플로우 요약**
  - **일반 회원가입 → 자동 로그인 → 온보딩 강제**
    1. `/signup` 폼에서 `authApi.signup` → `POST /api/v1/auth/signup`.
    2. 성공 시 같은 자격증명으로 `authApi.login` → `POST /api/v1/auth/login` 호출.
    3. `auth-store.checkAuth()`가 `GET /api/v1/auth/me`를 호출해 `user`를 채움.
    4. `hasCompletedOnboarding(user) === false`이면 `/onboarding`으로 `router.replace`.

  - **소셜 로그인 (Kakao/Google)**
    - **Backend-Driven (Web)**
      1. `/login`에서 `window.location.href = {BACKEND_URL}/oauth2/authorization/kakao`.
      2. 백엔드 `OAuth2SuccessHandler`가 로그인 완료 후 `/oauth/callback?accessToken&refreshToken&isNewUser`로 리다이렉트.
      3. `/oauth/callback/page.tsx`:
         - 쿼리에서 `accessToken`, `refreshToken`, `isNewUser` 추출.
         - `auth-store.setTokens()`로 저장 후 `checkAuth()` 호출 → `GET /api/v1/auth/me`.
         - `needOnboarding = isNewUser === "true" || !hasCompletedOnboarding(user)` 이면 `/onboarding`으로 이동, 아니면 대시보드(`/`)로 이동.

    - **Frontend-Driven (앱/SPA)**
      1. 클라이언트에서 Kakao/Google SDK로 토큰 획득.
      2. `authApi.kakaoLogin({ accessToken })` → `POST /api/v1/auth/oauth/kakao`.
      3. 응답의 `isNewUser`와 `/api/v1/auth/me` 결과를 조합해 `/onboarding` 강제 여부 결정.

  - **온보딩/재온보딩 (`/onboarding`, `/mypage`)**
    - 온보딩 페이지:
      - 입력 필드: `nickname`, `birthDate`, `birthTime`, `gender`, `calendarType`.
      - `userApi.submitOnboarding(body)` → `POST /api/v1/user/onboarding`.
      - 성공 시:
        - `auth-store.checkAuth()`로 `/api/v1/auth/me` 재조회,
        - 이후 메인 라우트(`/`)로 이동.
    - 마이페이지 재온보딩:
      - 동일 DTO/스토어(`user-store.profile`)를 사용.
      - “사주 다시 계산하기” 버튼이 같은 `submitOnboarding`을 호출.
      - 성공 후 프로필/지갑/인벤토리를 재조회하여 화면 동기화.

- **에러 처리**
  - `ErrorResponse.error` 값이 `ONBOARDING_001~003`인 경우:
    - `ONBOARDING_001`: 입력값 유효성 에러 → 필드 옆에 구체 메시지 표시.
    - `ONBOARDING_002`: 음력/양력 변환 에러 → 상단에 “음력/윤달 선택을 다시 확인해 주세요” 안내.
    - `ONBOARDING_003`: 일반 사주 계산 실패 → “일시적인 오류입니다. 잠시 후 다시 시도해주세요” 토스트.

### 3.2 마켓/캐시 헤더 (`/market`)

- **주요 파일**
  - `app/(main)/market/page.tsx`
  - `stores/stock-store.ts`
  - `lib/api/stock.ts`

- **백엔드 엔드포인트**
  - `GET /api/v1/market/indices`
  - `GET /api/v1/market/news`
  - `GET /api/v1/market/movers`

- **캐시 헤더 처리**
  - 모든 Market API 응답 헤더:
    - `X-Cache-Status`: `"HIT" | "MISS" | "STALE"`
    - `X-Cache-Age`: `number`(초)
    - `X-Data-Freshness`: `"FRESH" | "STALE" | "EXPIRED"`
  - `lib/api/index.ts` Axios 응답 인터셉터에서:
    - 헤더 값을 읽어 `stock-store.setBackendCacheMeta({ status, age, freshness })`로 저장.
  - UI:
    - 지수 카드/뉴스 리스트 상단에 “캐시 상태 배지” 표시 (예: `STALE`, `15s ago`).

### 3.3 Trade/Portfolio/Game/AI (`/trade`, `/portfolio`, `/shop`, `/ranking`, `/mypage`, `/oracle`)

- 이들 라우트는 기존 명세와 코드가 이미 세부적으로 맞춰져 있으므로, 이 문서에서는 **최소 매핑 정보**만 유지합니다.
- 자세한 스키마/플로우는 다음 문서를 참조:
  - 거래/포트폴리오: `FULL_SPECIFICATION.md` 5.3~5.4, `BACKEND_DEVELOPMENT_PLAN.md` 6.2~6.3
  - 상점/랭킹: `FULL_SPECIFICATION.md` 5.5, `FRONTEND_DEVELOPMENT_PLAN.md` 5.6~5.7
  - AI 도사: `FULL_SPECIFICATION.md` 5.6, `FRONTEND_DEVELOPMENT_PLAN.md` 5.8

## 4. `/calculator` 페이지 API 연결 명세

### 4.1 라우트 및 스토어

- **Route**: `/calculator`
- **주요 컴포넌트/스토어**:
  - `app/(main)/calculator/page.tsx`
  - (필요 시) 전용 Zustand 스토어 `calculator-store.ts` 또는 기존 `portfolio-store` 재사용

### 4.2 백엔드 엔드포인트 및 파라미터

- **배당금/세금 계산**
  - **Method/Path**: `GET /api/v1/calc/dividend`
  - **Auth**: `Authorization: Bearer {accessToken}` 필수
  - **Query 파라미터 (사용자 입력 기반)**:
    - `assumedDividendYield?: number`
      - 배당 수익률 (예: `0.03` → 3%)
      - 지갑의 `totalAssets`(총 자산)를 기준으로 `totalDividend = totalAssets × assumedDividendYield` 계산
    - `dividendPerShare?: number`
      - 주당 배당액 (예: `1.25`)
      - **1차 버전**에서는 실제 계산에 사용하지 않고, 향후 종목별 포지션 정보를 사용하는 2차 버전에서 활성화 예정
    - `taxRate?: number`
      - 배당소득세 세율 (예: `0.154` → 15.4%)
      - 없으면 0으로 취급 (세금 계산 생략)
  - **Response DTO**: `CalcDividendResponse`
    - `totalDividend: number` — 예상 총 배당금
    - `withholdingTax: number` — 예상 원천징수세
    - `netDividend: number` — 세후 수령액
    - `currency: string | null` — 1차 버전에서는 항상 `null` (통화/환율 전략 도입 전이기 때문)

- **양도소득세 계산**
  - **Method/Path**: `GET /api/v1/calc/tax`
  - **Auth**: `Authorization: Bearer {accessToken}` 필수
  - **Query 파라미터 (사용자 입력 기반)**:
    - `taxRate?: number`
      - 양도소득세 세율 (예: `0.22` → 22%)
      - 없으면 0으로 취급
  - **서버 측 계산 기준**:
    - `Wallet.realizedProfit`(실현 손익)을 과세 표준의 기초로 사용
    - `taxBase = max(realizedProfit, 0)`
    - `estimatedTax = taxBase × taxRate`
  - **Response DTO**: `CalcTaxResponse`
    - `realizedProfit: number` — 현재까지의 실현 손익
    - `taxBase: number` — 과세 표준 (손익이 음수/0이면 0)
    - `estimatedTax: number` — 예상 양도소득세
    - `currency: string | null` — 1차 버전에서는 `null`

### 4.3 프론트엔드 API 모듈 설계

- **파일**: `src/lib/api/calc.ts` (예상)
- **함수 시그니처 예시**:

```ts
// 배당금/세금 계산
export async function getDividend(params: {
  assumedDividendYield?: number;   // 배당 수익률 (0.03 = 3%)
  dividendPerShare?: number;       // 주당 배당액
  taxRate?: number;                // 세율
}): Promise<CalcDividendResponse> {
  return axiosInstance
    .get<CalcDividendResponse>("/api/v1/calc/dividend", { params })
    .then(res => res.data);
}

// 양도소득세 계산
export async function getTax(params: {
  taxRate?: number;                // 세율
}): Promise<CalcTaxResponse> {
  return axiosInstance
    .get<CalcTaxResponse>("/api/v1/calc/tax", { params })
    .then(res => res.data);
}
```

- **UI 입력 필드 (요약)**:
  - 배당 탭:
    - `배당 수익률 (%)` 입력 → `assumedDividendYield = percent / 100`
    - `주당 배당액` 입력 (향후 실제 포지션 기반 계산에 사용)
    - `세율 (%)` 입력 → `taxRate = percent / 100`
  - 세금 탭:
    - `세율 (%)` 입력 → `taxRate = percent / 100`

## 5. 유지보수 원칙

- 이 파일은 **프론트-백엔드 연동 관점의 인덱스**로 사용하며, 실제 계약 변경이 발생하면 반드시 아래 문서와 함께 수정해야 합니다.
  - `docs/FULL_SPECIFICATION.md`
  - `docs/BACKEND_DEVELOPMENT_PLAN.md`
  - `docs/FRONTEND_DEVELOPMENT_PLAN.md`
- 주기적으로 `src/lib/api/*.ts`, `src/stores/*.ts`, `src/app/**/page.tsx`를 스캔하여,
  - 이 문서의 매핑과 실제 import/사용 API가 어긋나지 않는지 확인하는 것을 권장합니다.

