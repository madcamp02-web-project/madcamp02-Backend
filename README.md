# MadCamp02 Backend - 주술사 -

Finnhub 실시간 시세 + 모의투자(거래/포트폴리오) + 게이미피케이션(가챠/인벤토리/랭킹) + 사주(온보딩) + (향후) AI 상담을 제공하는 **Spring Boot 기반 백엔드**입니다.

---

## 기술 스택

### Backend (Core)

- **언어/런타임**: Java 21 (LTS)
- **프레임워크**: Spring Boot 3.4.x
- **보안**: Spring Security 6.x (OAuth2 Client + JWT, Hybrid Auth)
- **데이터 접근**: Spring Data JPA
- **실시간**: Spring WebSocket (STOMP), Endpoint `/ws-stomp`
- **DB 마이그레이션**: Flyway
- **캐시/브로커**: Redis 7 (캐싱, 최신가 저장, Pub/Sub 보조)
- **RDBMS**: PostgreSQL 18 (docker-compose 기준)

### Frontend / AI

- **Frontend**: Next.js 16 + React 19 + TypeScript + Tailwind + Zustand + STOMP.js
- **AI**: Python 3.11+ Gemini api

---

## 시스템 아키텍처

```mermaid
flowchart TB
  FE[Frontend\nNext.js + STOMP.js] -->|HTTPS| BE[Backend\nSpring Boot]
  FE -->|WSS (STOMP)\n/ws-stomp| BE

  BE -->|JPA| PG[(PostgreSQL)]
  BE -->|Cache| RD[(Redis)]
  BE -->|REST| FH[Finnhub API]
  BE -->|WebSocket| FHWS[Finnhub WS\nTrades]
```

---

## 핵심 기능 요약

- **인증/온보딩**
  - Hybrid Auth 지원(Backend-Driven Redirect + Frontend-Driven Token API)
  - 온보딩 완료 기준: `birthDate` && `sajuElement` (별도 플래그 없음)
- **시장 데이터**
  - `/api/v1/market/indices|news|movers`
  - 지수는 **ETF 기반**(SPY/QQQ/DIA)으로 제공 (지수 심볼 미지원 이슈 회피)
  - Redis 캐싱 + Stale fallback 전략(헤더: `X-Cache-Status`, 등)
- **거래/포트폴리오 엔진**
  - 비관적 락 기반 트랜잭션으로 동시성 제어
  - 주문 체결 알림: STOMP `/user/queue/trade`
- **게임화**
  - 가챠(코인 차감, 중복 재추첨, 실패 코드), 인벤토리/장착(카테고리 단일 장착), 랭킹
- **실시간**
  - Finnhub Trades WebSocket 수신 → Redis 최신가 저장 → STOMP 브로드캐스트
  - 지수 브로드캐스트: `/topic/stock.indices` (주기)

---

## 빠른 시작(로컬)

### 1) 환경 변수

레포의 예시 파일을 복사해 `.env`를 만든 뒤 값을 채우세요.

- 예시 파일: `env.example`
- 애플리케이션 설정: `src/main/resources/application.yml`, `application-dev.yml`

```bash
cp env.example .env
```

### 2) 인프라 실행 (Docker)

`docker-compose.yml`에 Postgres/Redis/Backend 서비스가 정의되어 있습니다.

```bash
docker compose up -d
```

백엔드까지 같이 빌드/실행하려면:

```bash
docker compose up -d --build
```

### 3) 백엔드 실행 (Gradle)

Docker로 백엔드를 띄우지 않고(=로컬에서 직접 실행) 싶으면 아래처럼 실행합니다.

```bash
./gradlew bootRun
```

또는 빌드:

```bash
./gradlew clean build
```
---

## 주요 엔드포인트(요약)

> 상세 스키마/DTO/에러 코드는 `docs/FULL_SPECIFICATION.md`가 기준입니다.

### Auth

- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/auth/me`
- `POST /api/v1/auth/oauth/kakao` (Frontend-Driven)
- `POST /api/v1/auth/oauth/google` (Frontend-Driven)

### User

- `GET /api/v1/user/me`
- `PUT /api/v1/user/me`
- `POST /api/v1/user/onboarding` (온보딩/재온보딩 공용, idempotent)
- `GET /api/v1/user/wallet`
- `GET /api/v1/user/watchlist`
- `POST /api/v1/user/watchlist`
- `DELETE /api/v1/user/watchlist/{ticker}`

### Market / Stock

- `GET /api/v1/market/indices`
- `GET /api/v1/market/news`
- `GET /api/v1/market/movers`
- `GET /api/v1/stock/search`
- `GET /api/v1/stock/quote/{ticker}`
- `GET /api/v1/stock/candles/{ticker}` (EODHD + DB 캐싱, Quota 관리)

### Trade

- `GET /api/v1/trade/available-balance`
- `POST /api/v1/trade/order`
- `GET /api/v1/trade/portfolio`
- `GET /api/v1/trade/history`

### Game

- `GET /api/v1/game/items`
- `POST /api/v1/game/gacha`
- `GET /api/v1/game/inventory`
- `PUT /api/v1/game/equip/{itemId}`
- `GET /api/v1/game/ranking`

### Realtime (STOMP)

- **Endpoint**: `/ws-stomp`
- **Topics**
  - `/topic/stock.indices`
  - `/topic/stock.ticker.{ticker}`
  - `/user/queue/trade`

---