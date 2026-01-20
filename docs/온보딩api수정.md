# MadCamp02 백엔드 누락 기능 정리 (2026-01-20 기준)

## 1. User / Onboarding 모듈

- **1.1 User 모듈 상태**
  - 계획서 Status 테이블: User 모듈 진행률 80%, ⚠️ Update Req.
  - 누락/보완 필요 항목:
    - `users.is_public`, `users.is_ranking_joined` 컬럼과 Entity 매핑 보완 (Flyway V2 + JPA 엔티티).
    - `UserMeResponse`에 `birthDate`, `birthTime`, `gender`, `calendarType`, `sajuElement`, `zodiacSign`, `isPublic`, `isRankingJoined`, `avatarUrl` 등을 포함하도록 DTO 정합성 점검.

- **1.2 `POST /api/v1/user/onboarding` (Phase 2) 구현 확인**
  - 설계는 다음을 요구:
    - 입력: `nickname`, `birthDate`, `birthTime`, `gender`, `calendarType`.
    - 처리:
      - 한국천문연구원 API를 통한 양력/음력 변환.
      - `SajuCalculator.calculatePrecise()`로 연/월/일/시 4주 사주 계산.
      - `users.birth_date/birth_time/gender/calendar_type/saju_element/zodiac_sign` 컬럼을 새 값으로 덮어쓰기 (idempotent 재온보딩).
    - 출력: 온보딩 후 내 정보(`UserMeResponse` 또는 사주 요약 DTO).
  - 문서 상 "향후 실행 계획"으로만 존재하므로, 실제 코드에서 위 스펙을 충족하는지 검증 및 미구현 시 구현 필요.

- **1.3 온보딩 완료 해석 및 소셜 플래그 처리**
  - 정책:
    - 온보딩 완료 = `users.birth_date IS NOT NULL` AND `users.saju_element IS NOT NULL`.
    - 소셜 로그인 응답의 `isNewUser`는 라우팅 힌트이며, 권한 판단은 항상 JWT+DB 상태로 수행.
  - 누락 가능 포인트:
    - 실제 코드에서 이 해석 규칙을 명시적으로 사용하고 있는지 (`hasCompletedOnboarding` 유틸, 서비스 레벨 검증 등).

## 2. Auth / OAuth 플로우 보강

- **2.1 `/api/v1/auth/signup` 이후 자동 로그인/온보딩 연계**
  - 프론트 계획서에는 "회원가입 성공 시 자동 로그인 + `/onboarding`" 플로우가 명시되어 있으나, 백엔드 관점에서:
    - Signup 응답 DTO 구조(`AuthResponse` vs 단순 메시지)와, 즉시 로그인 시 필요한 정보(이메일/비밀번호) 전달 방식이 명확히 정리되어야 함.
  - 누락/보완 항목:
    - Signup 응답을 프론트 플로우와 일관되게 정리 (예: 회원가입 후 별도 데이터 필요 없음 → 프론트가 동일 크리덴셜로 로그인).

- **2.2 OAuth Backend-Driven 플로우의 `/oauth/callback` 쿼리 계약**
  - 요구사항:
    - 리다이렉트: `/oauth/callback?accessToken=...&refreshToken=...&isNewUser=true|false`.
  - 필요 확인 항목:
    - 실제 `OAuth2SuccessHandler`에서 위 형태로 파라미터를 넣어주고 있는지.
    - 토큰 만료/에러 시의 예외 처리 플로우.

## 3. Calculation API (Phase 6.4)

- **3.1 배당/세금 계산 API**
  - 설계만 존재:
    - `GET /api/v1/calc/dividend`: 보유 종목 기반 예상 배당금 및 세금 계산.
    - `GET /api/v1/calc/tax`: 실현 수익 기반 예상 양도소득세 계산.
  - 문서에는 비즈니스 로직 개요만 있고, 구현 완료 섹션이 없음.
  - 누락 기능:
    - `CalcController`, `CalcService` 구현.
    - 포트폴리오/거래 내역과의 연동 로직.

## 7. Redis 캐싱 확장 일부 (Phase 3.6 세부)

- **7.1 Market Indices/News 캐싱**
  - 설계:
    - `market:indices`, `market:news`, `market:movers` 키 + TTL + Stale 키.
    - `X-Cache-Status`, `X-Cache-Age`, `X-Data-Freshness` 헤더.
  - 상태:
    - 문서 상 "구현 대상"으로 표기, Movers 부분은 구현 완료, Indices/News는 확인 필요.
  - 잠재 누락 기능:
    - `MarketService.getIndices/getNews`의 Redis 캐싱 적용 여부.

## 8. 요약

- 핵심 미완료/점검 필요 기능:
  - User/Onboarding Phase 2 전체 구현 및 `/api/v1/user/onboarding`의 실제 동작 검증.
  - 온보딩 완료 해석(`birth_date + saju_element`) 규칙이 코드에 반영되어 있는지 확인.
  - Calc(배당/세금)
  - Redis 캐싱 확장(특히 Indices/News)과 Auth/OAuth 일부 보강.