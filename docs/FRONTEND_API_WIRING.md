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

## 3. 라우트별 핵심 매핑 (요약)

> 전체 라우트/스토어 매핑은 추후 테이블 형태로 확장할 예정이며, 여기서는 요청사항과 직접 관련된 `/calculator` 섹션을 우선 고정합니다.

- `/login`, `/signup`, `/oauth/callback`, `/onboarding`
  - `lib/api/auth.ts`, `lib/api/user.ts`
  - `/api/v1/auth/signup`, `/api/v1/auth/login`, `/api/v1/auth/oauth/kakao`, `/api/v1/auth/me`, `/api/v1/user/onboarding`

- `/market`
  - `lib/api/stock.ts`
  - `/api/v1/market/indices`, `/api/v1/market/news`, `/api/v1/market/movers`
  - Redis 캐시 + `X-Cache-*` 헤더 → `stock-store`의 캐시 메타로 반영

- `/trade`, `/portfolio`, `/shop`, `/ranking`, `/mypage`, `/oracle`
  - 기존 명세/코드와 동일 (Trade/Portfolio/Game/Ai API)

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

### 4.4 통화(currency) 처리 향후 계획

- **현재 상태 (백엔드)**:
  - `CalcDividendResponse.currency`, `CalcTaxResponse.currency` 필드는 존재하지만,
  - `CalcService`에서는 아직 통화/환율 전략을 도입하지 않았으므로 **항상 `null`** 로 내려보냄.
- **향후 확장 계획**:
  - `/api/v1/calc/dividend|tax`에 `currency` 쿼리 파라미터를 추가 (`USD`, `KRW` 등).
  - 서버에서 Wallet/Portfolio의 통화 단위를 기준으로 일관된 계산을 수행하고, 응답 DTO의 `currency` 필드를 실제 값으로 채움.
  - 프론트에서는:
    - `currency` 값에 따라 금액 표시 포맷(소수점/단위/심볼)을 조정.
    - 통화 선택 드롭다운(예: `USD`, `KRW`)을 `calculator` 페이지에 추가하고, 선택값을 쿼리 파라미터로 전달.

## 5. 유지보수 원칙

- 이 파일은 **프론트-백엔드 연동 관점의 인덱스**로 사용하며, 실제 계약 변경이 발생하면 반드시 아래 문서와 함께 수정해야 합니다.
  - `docs/FULL_SPECIFICATION.md`
  - `docs/BACKEND_DEVELOPMENT_PLAN.md`
  - `docs/FRONTEND_DEVELOPMENT_PLAN.md`
- 주기적으로 `src/lib/api/*.ts`, `src/stores/*.ts`, `src/app/**/page.tsx`를 스캔하여,
  - 이 문서의 매핑과 실제 import/사용 API가 어긋나지 않는지 확인하는 것을 권장합니다.

