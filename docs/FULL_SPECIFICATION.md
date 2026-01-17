# 📁 MadCamp02: 최종 통합 명세서

**Ver 2.0 - Complete Edition (Frontend + Backend Integration)**

---

## 📋 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [시스템 아키텍처](#2-시스템-아키텍처)
3. [기술 스택](#3-기술-스택)
4. [데이터베이스 설계](#4-데이터베이스-설계)
5. [API 명세](#5-api-명세)
6. [프론트엔드 구조](#6-프론트엔드-구조)
7. [핵심 기능 상세](#7-핵심-기능-상세)
8. [게이미피케이션 시스템](#8-게이미피케이션-시스템)
9. [AI 시스템](#9-ai-시스템)
10. [실시간 통신](#10-실시간-통신)
11. [보안 및 인증](#11-보안-및-인증)
12. [UI/UX 가이드라인](#12-uiux-가이드라인)
13. [테스트 전략](#13-테스트-전략)
14. [배포 전략](#14-배포-전략)
15. [개발 로드맵](#15-개발-로드맵)

---

## 1. 프로젝트 개요

### 1.1 프로젝트 정보

| 항목 | 내용 |
|------|------|
| **프로젝트명** | MadCamp02 |
| **슬로건** | "차트는 운명을 말하고, 수익은 아바타를 춤추게 한다." |
| **버전** | 2.0 |
| **타겟 플랫폼** | Web (Desktop 우선, 모바일 반응형) |

### 1.2 프로젝트 정의

Finnhub 실시간 주가 데이터를 기반으로, **사용자의 투자 성과와 사주(Saju)가 결합되어 아바타와 상호작용**하는 RPG형 웹 모의투자 플랫폼.

### 1.3 핵심 차별점

1. **Narrative (서사):** 딱딱한 주식을 '운세'와 '캐릭터'로 풀어냄
2. **Gamification (게임화):** 투자 수익 → 게임 코인 → 가챠 → 아바타 커스터마이징
3. **Tech (기술):** RDBMS의 안정성 + Redis의 속도 + Gen-AI의 창의성을 결합한 하이브리드 아키텍처
4. **Personalization (개인화):** 사주/별자리 기반 맞춤형 투자 조언

### 1.4 핵심 사용자 시나리오

```
1. 사용자 가입 → 생년월일 입력 → 사주(오행) 계산
2. 초기 자금 $10,000 지급 → 모의투자 시작
3. 실시간 차트 확인 → 매수/매도 주문
4. AI 도사에게 종목 상담 (사주 기반 조언)
5. 수익 실현 → 게임 코인 획득
6. 가챠(자판기)로 아바타 아이템 획득
7. 아바타 커스터마이징 → 랭킹 경쟁
```

---

## 2. 시스템 아키텍처

### 2.1 전체 구조도

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                              │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  React (Next.js 16) + TypeScript + Tailwind CSS             │ │
│  │  ├── Zustand (상태 관리)                                     │ │
│  │  ├── STOMP.js (WebSocket)                                   │ │
│  │  ├── Lightweight Charts (캔들 차트)                          │ │
│  │  └── Shadcn/UI (컴포넌트)                                    │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              │
                    HTTPS / WSS (TLS 1.3)
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      GATEWAY LAYER                               │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  Nginx (Load Balancer + SSL Termination + Rate Limiting)   │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    APPLICATION LAYER                             │
│  ┌──────────────────────┐    ┌──────────────────────┐           │
│  │  Spring Boot 3.2     │    │  FastAPI (Python)    │           │
│  │  (Core Server)       │◄───│  (AI Server)         │           │
│  │  ├── REST API        │    │  ├── LLM Inference   │           │
│  │  ├── WebSocket       │    │  ├── Stable Diffusion│           │
│  │  ├── OAuth2          │    │  └── SSE Streaming   │           │
│  │  └── Transaction     │    └──────────────────────┘           │
│  └──────────────────────┘                                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                       DATA LAYER                                 │
│  ┌──────────────────┐  ┌──────────────────┐  ┌────────────────┐ │
│  │  PostgreSQL 16   │  │  Redis 7         │  │  Finnhub API   │ │
│  │  (Main DB)       │  │  (Cache/Pub-Sub) │  │  (Market Data) │ │
│  │  ├── Users       │  │  ├── Session     │  │  ├── WebSocket │ │
│  │  ├── Wallet      │  │  ├── Stock Price │  │  └── REST API  │ │
│  │  ├── Portfolio   │  │  ├── Ranking     │  └────────────────┘ │
│  │  ├── Trade Logs  │  │  └── Pub/Sub     │                     │
│  │  ├── Chat History│  └──────────────────┘                     │
│  │  └── Items       │                                            │
│  └──────────────────┘                                            │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 데이터 흐름

```
[Finnhub WebSocket] 
      │ 
      ▼ (실시간 시세)
[Spring Boot WebSocket Client]
      │
      ├──▶ [Redis] stock:{ticker}:price (캐시)
      │
      └──▶ [Redis Pub/Sub] channel: stock.update
                  │
                  ▼
      [Spring STOMP Broker]
                  │
                  ▼ (구독 종목만)
      [React Client] ──▶ UI 업데이트
```

---

## 3. 기술 스택

### 3.1 Frontend

| 기술 | 버전 | 용도 |
|------|------|------|
| Next.js | 16.x | React 프레임워크 |
| React | 19.x | UI 라이브러리 |
| TypeScript | 5.x | 타입 안전성 |
| Tailwind CSS | 4.x | 스타일링 |
| Shadcn/UI | Latest | UI 컴포넌트 |
| Zustand | 4.x | 전역 상태 관리 |
| @stomp/stompjs | 7.x | WebSocket 클라이언트 |
| lightweight-charts | 4.x | 캔들 차트 |
| Axios | 1.x | HTTP 클라이언트 |
| next-auth | 5.x | 인증 |
| dayjs | 1.x | 날짜 처리 |

### 3.2 Backend (Core)

| 기술 | 버전 | 용도 |
|------|------|------|
| Java | 21 LTS | 언어 |
| Spring Boot | 3.2.x | 프레임워크 |
| Spring Security | 6.x | 보안 |
| Spring WebSocket | 6.x | 실시간 통신 |
| Spring Data JPA | 3.x | ORM |
| Lombok | Latest | 보일러플레이트 제거 |
| MapStruct | Latest | DTO 매핑 |

### 3.3 Backend (AI)

| 기술 | 버전 | 용도 |
|------|------|------|
| Python | 3.11+ | 언어 |
| FastAPI | 0.100+ | API 서버 |
| PyTorch | 2.x | AI 프레임워크 |
| Transformers | Latest | LLM |
| Diffusers | Latest | Stable Diffusion |

### 3.4 Database & Cache

| 기술 | 버전 | 용도 |
|------|------|------|
| PostgreSQL | 16 | 메인 DB |
| Redis | 7.x | 캐시/Pub-Sub |

### 3.5 External APIs

| API | 용도 |
|-----|------|
| Finnhub | 미국 주식 실시간 데이터 |
| Google OAuth2 | 소셜 로그인 |
| 🆕 Kakao OAuth2 | 소셜 로그인 (카카오) |

---

## 4. 데이터베이스 설계

### 4.1 ERD (Entity-Relationship Diagram)

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│    users     │       │    wallet    │       │  portfolio   │
├──────────────┤       ├──────────────┤       ├──────────────┤
│ user_id (PK) │◄──────│ user_id (FK) │       │ pf_id (PK)   │
│ email        │       │ wallet_id    │       │ user_id (FK) │
│ nickname     │       │ cash_balance │       │ ticker       │
│ provider     │       │ realized_profit│     │ quantity     │
│ birth_date   │       │ total_assets │       │ avg_price    │
│ saju_element │       │ updated_at   │       └──────────────┘
│ zodiac_sign  │       └──────────────┘              │
│ avatar_url   │              │                      │
│ created_at   │              │                      │
└──────────────┘              │                      │
       │                      │                      │
       │               ┌──────▼──────┐               │
       │               │ trade_logs  │◄──────────────┘
       │               ├─────────────┤
       │               │ log_id (PK) │
       │               │ user_id (FK)│
       │               │ ticker      │
       │               │ trade_type  │
       │               │ price       │
       │               │ quantity    │
       │               │ fee         │
       │               │ trade_date  │
       │               └─────────────┘
       │
       │        ┌──────────────┐       ┌──────────────┐
       │        │    items     │       │  inventory   │
       │        ├──────────────┤       ├──────────────┤
       │        │ item_id (PK) │◄──────│ item_id (FK) │
       │        │ name         │       │ inv_id (PK)  │
       │        │ category     │       │ user_id (FK) │◄───┐
       │        │ rarity       │       │ is_equipped  │    │
       │        │ probability  │       │ acquired_at  │    │
       │        │ image_url    │       └──────────────┘    │
       │        └──────────────┘                           │
       │                                                   │
       └───────────────────────────────────────────────────┘
       │
       │        ┌──────────────────┐
       └───────►│   chat_history   │
                ├──────────────────┤
                │ chat_id (PK)     │
                │ user_id (FK)     │
                │ session_id       │
                │ messages (JSONB) │
                │ sentiment_score  │
                │ created_at       │
                └──────────────────┘
```

### 4.2 테이블 DDL

#### users (사용자)

```sql
CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),             -- 🆕 일반 회원용 (BCrypt 암호화), OAuth는 NULL
    nickname VARCHAR(50) NOT NULL,
    provider VARCHAR(20) DEFAULT 'LOCAL',  -- 🆕 LOCAL, GOOGLE, KAKAO
    birth_date DATE,                   -- 🆕 NULL 허용 (온보딩에서 입력)
    saju_element VARCHAR(10),          -- FIRE, WATER, WOOD, GOLD, EARTH
    zodiac_sign VARCHAR(20),           -- 띠 (Dragon, Snake, etc.)
    avatar_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
```

#### wallet (자산 원장)

```sql
CREATE TABLE wallet (
    wallet_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(user_id),
    cash_balance NUMERIC(19, 4) DEFAULT 10000.0000,   -- 투자 가능 예수금 ($)
    realized_profit NUMERIC(19, 4) DEFAULT 0.0000,    -- 실현 수익 (가챠 코인)
    total_assets NUMERIC(19, 4) DEFAULT 10000.0000,   -- 현금 + 평가금액
    game_coin INT DEFAULT 0,                          -- 가챠용 코인
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_wallet_user ON wallet(user_id);
```

#### portfolio (보유 주식)

```sql
CREATE TABLE portfolio (
    pf_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    ticker VARCHAR(10) NOT NULL,
    quantity INT NOT NULL CHECK (quantity >= 0),
    avg_price NUMERIC(19, 4) NOT NULL,
    UNIQUE(user_id, ticker)
);

CREATE INDEX idx_portfolio_user ON portfolio(user_id);
CREATE INDEX idx_portfolio_ticker ON portfolio(ticker);
```

#### trade_logs (거래 기록 - Immutable)

```sql
CREATE TABLE trade_logs (
    log_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    ticker VARCHAR(10) NOT NULL,
    trade_type VARCHAR(4) NOT NULL CHECK (trade_type IN ('BUY', 'SELL')),
    price NUMERIC(19, 4) NOT NULL,
    quantity INT NOT NULL,
    total_amount NUMERIC(19, 4) NOT NULL,
    fee NUMERIC(19, 4) DEFAULT 0,
    realized_pnl NUMERIC(19, 4),           -- 매도 시 실현 손익
    trade_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_trade_user ON trade_logs(user_id);
CREATE INDEX idx_trade_date ON trade_logs(trade_date);
CREATE INDEX idx_trade_ticker ON trade_logs(ticker);
```

#### chat_history (AI 대화 로그)

```sql
CREATE TABLE chat_history (
    chat_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    session_id UUID NOT NULL,
    messages JSONB NOT NULL,              -- [{"role":"user","content":"..."},...]
    sentiment_score FLOAT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_user ON chat_history(user_id);
CREATE INDEX idx_chat_session ON chat_history(session_id);
CREATE INDEX idx_chat_gin ON chat_history USING gin (messages);
```

#### items (아이템 마스터)

```sql
CREATE TABLE items (
    item_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(20) NOT NULL,        -- COSTUME, ACCESSORY, AURA, BACKGROUND
    rarity VARCHAR(20) NOT NULL,          -- COMMON, RARE, EPIC, LEGENDARY
    probability FLOAT NOT NULL,           -- 뽑기 확률 (0.01 = 1%)
    image_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_items_rarity ON items(rarity);
CREATE INDEX idx_items_category ON items(category);
```

#### inventory (사용자 인벤토리)

```sql
CREATE TABLE inventory (
    inv_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    item_id BIGINT NOT NULL REFERENCES items(item_id),
    is_equipped BOOLEAN DEFAULT FALSE,
    acquired_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, item_id)
);

CREATE INDEX idx_inventory_user ON inventory(user_id);
CREATE INDEX idx_inventory_equipped ON inventory(user_id, is_equipped);
```

#### watchlist (관심 종목)

```sql
CREATE TABLE watchlist (
    watchlist_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    ticker VARCHAR(10) NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, ticker)
);

CREATE INDEX idx_watchlist_user ON watchlist(user_id);
```

#### notifications (알림)

```sql
CREATE TABLE notifications (
    notif_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    type VARCHAR(30) NOT NULL,            -- TRADE_COMPLETE, PRICE_ALERT, etc.
    title VARCHAR(200) NOT NULL,
    message TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notif_user ON notifications(user_id);
CREATE INDEX idx_notif_unread ON notifications(user_id, is_read);
```

---

## 5. API 명세

### 5.1 인증 API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/v1/auth/signup` | 🆕 일반 회원가입 | ❌ |
| POST | `/api/v1/auth/login` | 🆕 일반 로그인 (이메일/비밀번호) | ❌ |
| POST | `/api/v1/auth/oauth/google` | Google OAuth 로그인 | ❌ |
| POST | `/api/v1/auth/oauth/kakao` | 🆕 Kakao OAuth 로그인 | ❌ |
| POST | `/api/v1/auth/refresh` | Access Token 갱신 | ❌ |
| POST | `/api/v1/auth/logout` | 로그아웃 | ✅ |
| GET | `/api/v1/auth/me` | 현재 사용자 정보 | ✅ |

#### POST /api/v1/auth/signup 🆕

일반 회원가입 (이메일, 비밀번호, 닉네임)

**Request:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123!",
  "nickname": "투자도사"
}
```

**Response (201):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6...",
  "expiresIn": 3600,
  "user": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "투자도사",
    "provider": "LOCAL",
    "sajuElement": null,
    "avatarUrl": null
  },
  "isNewUser": true
}
```

#### POST /api/v1/auth/login 🆕

일반 로그인 (이메일/비밀번호)

**Request:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123!"
}
```

**Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6...",
  "expiresIn": 3600,
  "user": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "투자도사",
    "provider": "LOCAL",
    "sajuElement": "FIRE",
    "avatarUrl": "/avatars/1.jpg"
  },
  "isNewUser": false
}
```

#### POST /api/v1/auth/oauth/google

Google OAuth 로그인 (기존)

**Request:**
```json
{
  "provider": "google",
  "idToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6..."
}
```

#### POST /api/v1/auth/oauth/kakao 🆕

Kakao OAuth 로그인

**Request:**
```json
{
  "provider": "kakao",
  "accessToken": "카카오_액세스_토큰"
}
```

**Response (200):** (공통)
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6...",
  "expiresIn": 3600,
  "user": {
    "userId": 1,
    "email": "user@gmail.com",
    "nickname": "투자도사",
    "provider": "GOOGLE",
    "sajuElement": "FIRE",
    "avatarUrl": "/avatars/1.jpg"
  },
  "isNewUser": false
}
```

### 5.2 사용자 API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/v1/user/me` | 내 정보 조회 | ✅ |
| PUT | `/api/v1/user/me` | 내 정보 수정 | ✅ |
| POST | `/api/v1/user/onboarding` | 온보딩 (생년월일 등록) | ✅ |
| GET | `/api/v1/user/wallet` | 지갑 정보 조회 | ✅ |

#### POST /api/v1/user/onboarding

**Request:**
```json
{
  "nickname": "투자도사",
  "birthDate": "1995-05-20"
}
```

**Response (200):**
```json
{
  "userId": 1,
  "nickname": "투자도사",
  "birthDate": "1995-05-20",
  "sajuElement": "FIRE",
  "sajuElementKor": "화(火)",
  "zodiacSign": "DOG",
  "zodiacSignKor": "개띠",
  "luckyColor": "Red",
  "luckyNumber": [3, 8],
  "wallet": {
    "cashBalance": 10000.0000,
    "realizedProfit": 0.0000,
    "gameCoin": 0
  }
}
```

### 5.3 거래 API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/v1/trade/order` | 매수/매도 주문 | ✅ |
| GET | `/api/v1/trade/portfolio` | 포트폴리오 조회 | ✅ |
| GET | `/api/v1/trade/history` | 거래 내역 조회 | ✅ |
| GET | `/api/v1/trade/portfolio/{ticker}` | 특정 종목 보유 현황 | ✅ |

#### POST /api/v1/trade/order

**Request:**
```json
{
  "ticker": "AAPL",
  "type": "BUY",
  "quantity": 10,
  "orderType": "MARKET"
}
```

**Response (200):**
```json
{
  "orderId": 12345,
  "ticker": "AAPL",
  "type": "BUY",
  "quantity": 10,
  "executedPrice": 198.45,
  "totalAmount": 1984.50,
  "fee": 0.00,
  "executedAt": "2026-01-16T10:30:00Z",
  "portfolio": {
    "ticker": "AAPL",
    "quantity": 20,
    "avgPrice": 195.25,
    "currentPrice": 198.45,
    "profitLoss": 64.00,
    "profitLossPercent": 1.64
  },
  "wallet": {
    "cashBalance": 8015.50,
    "totalAssets": 11985.50
  }
}
```

### 5.4 주식 API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/v1/stock/quote/{ticker}` | 종목 현재가 조회 | ✅ |
| GET | `/api/v1/stock/candles/{ticker}` | 캔들 데이터 조회 | ✅ |
| GET | `/api/v1/stock/search` | 종목 검색 | ✅ |
| GET | `/api/v1/stock/profile/{ticker}` | 종목 상세 정보 | ✅ |
| GET | `/api/v1/watchlist` | 관심 종목 조회 | ✅ |
| POST | `/api/v1/watchlist` | 관심 종목 추가 | ✅ |
| DELETE | `/api/v1/watchlist/{ticker}` | 관심 종목 삭제 | ✅ |

#### GET /api/v1/stock/candles/{ticker}

**Query Parameters:**
- `resolution`: 1, 5, 15, 30, 60, D, W, M
- `from`: Unix timestamp
- `to`: Unix timestamp

**Response (200):**
```json
{
  "ticker": "AAPL",
  "candles": [
    {
      "time": 1705392000,
      "open": 197.50,
      "high": 199.20,
      "low": 196.80,
      "close": 198.45,
      "volume": 58200000
    }
  ]
}
```

### 5.5 배당금 계산기 API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/v1/calc/dividend` | 배당금 계산 | ✅ |
| GET | `/api/v1/calc/tax` | 세금 계산 | ✅ |

#### GET /api/v1/calc/dividend

**Response (200):**
```json
{
  "portfolio": [
    {
      "ticker": "AAPL",
      "quantity": 20,
      "dividendPerShare": 0.96,
      "annualDividend": 19.20,
      "dividendYield": 0.48,
      "exDividendDate": "2026-02-09"
    }
  ],
  "summary": {
    "totalAnnualDividend": 156.40,
    "withholdingTax": 23.46,
    "netDividend": 132.94,
    "averageYield": 2.34
  }
}
```

### 5.6 게임/가챠 API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/v1/game/gacha` | 가챠 뽑기 | ✅ |
| GET | `/api/v1/game/items` | 아이템 목록 | ✅ |
| GET | `/api/v1/game/inventory` | 인벤토리 조회 | ✅ |
| PUT | `/api/v1/game/equip/{itemId}` | 아이템 장착/해제 | ✅ |
| GET | `/api/v1/game/ranking` | 랭킹 조회 | ✅ |
| POST | `/api/v1/game/convert-profit` | 수익 → 코인 변환 | ✅ |

#### POST /api/v1/game/gacha

**Request:**
```json
{
  "count": 1
}
```

**Response (200):**
```json
{
  "results": [
    {
      "itemId": 5,
      "name": "황금 왕관",
      "category": "ACCESSORY",
      "rarity": "LEGENDARY",
      "imageUrl": "/items/golden-crown.png",
      "description": "대박 투자자의 상징",
      "isNew": true
    }
  ],
  "wallet": {
    "gameCoin": 2000,
    "spent": 500
  }
}
```

### 5.7 AI 상담 API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/v1/chat/ask` | AI 상담 (SSE 스트리밍) | ✅ |
| GET | `/api/v1/chat/history` | 대화 내역 조회 | ✅ |
| DELETE | `/api/v1/chat/session/{sessionId}` | 세션 삭제 | ✅ |

#### POST /api/v1/chat/ask (SSE)

**Request:**
```json
{
  "message": "오늘 테슬라 사도 될까?",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "context": {
    "interestedTicker": "TSLA"
  }
}
```

**Response (SSE Stream):**
```
event: message
data: {"content": "허허, "}

event: message
data: {"content": "자네의 사주를 보니..."}

event: done
data: {"messageId": 123, "sentimentScore": 0.75}
```

### 5.8 알림 API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/v1/notifications` | 알림 목록 조회 | ✅ |
| PUT | `/api/v1/notifications/{id}/read` | 알림 읽음 처리 | ✅ |
| PUT | `/api/v1/notifications/read-all` | 전체 읽음 처리 | ✅ |

---

## 6. 프론트엔드 구조

### 6.1 현재 구현 상태

> **참고:** 아래 구조에서 ✅는 구현 완료, 🆕는 신규 구현 필요, ⚠️는 수정/업그레이드 필요를 의미합니다.

### 6.2 디렉토리 구조

```
📁 app/
├── layout.tsx                      # ✅ 루트 레이아웃 (Geist 폰트, Analytics)
├── globals.css                     # ✅ 글로벌 스타일 (oklch 컬러 시스템)
├── page.tsx                        # ✅ 대시보드 (드래그 가능 위젯 그리드)
├── 📁 gacha/
│   └── page.tsx                    # ✅ 가챠 (캡슐 토이 머신 UI)
├── 📁 mypage/
│   └── page.tsx                    # ✅ 마이페이지 (개요/인벤토리/아바타/설정 탭)
├── 📁 oracle/
│   └── page.tsx                    # ✅ AI 도사 상담 (사주/별자리/오하아사 탭)
├── 📁 ranking/
│   └── page.tsx                    # ✅ 랭킹 (포디움 + 전체 순위)
├── 📁 login/
│   └── page.tsx                    # 🆕 로그인 (일반 + Google + Kakao OAuth)
├── 📁 signup/
│   └── page.tsx                    # 🆕 회원가입 (이메일/비밀번호/닉네임)
├── 📁 onboarding/
│   └── page.tsx                    # 🆕 온보딩 (생년월일 입력 → 사주 계산)
├── 📁 calculator/
│   └── page.tsx                    # 🆕 배당금 계산기
├── loading.tsx                     # 🆕 글로벌 로딩
└── not-found.tsx                   # 🆕 404 페이지

📁 components/
├── 📁 auth/                        # 🆕 인증 컴포넌트
│   ├── login-form.tsx              # 🆕 일반 로그인 + OAuth 버튼
│   ├── signup-form.tsx             # 🆕 일반 회원가입 폼
│   ├── onboarding-form.tsx         # 🆕 온보딩 폼 (사주 계산)
│   └── protected-route.tsx         # 🆕 인증 보호 래퍼
│
├── 📁 dashboard/                   # 대시보드 컴포넌트
│   ├── header.tsx                  # ✅ 헤더 (검색, 알림, 프로필)
│   ├── sidebar.tsx                 # ✅ 사이드바 (네비게이션)
│   ├── stock-chart.tsx             # ✅ Area 차트 (Recharts)
│   ├── candle-chart.tsx            # 🆕 캔들스틱 차트 (lightweight-charts)
│   ├── trade-panel.tsx             # ⚠️ 매수/매도 (API 연동 필요)
│   ├── portfolio-summary.tsx       # ⚠️ 포트폴리오 (API 연동 필요)
│   ├── watchlist.tsx               # ⚠️ 관심 종목 (API 연동 필요)
│   ├── avatar-display.tsx          # ✅ 아바타 (수익률 기반 효과)
│   ├── ai-chatbot.tsx              # ⚠️ AI 챗봇 위젯 (SSE 연동 필요)
│   ├── mini-ranking.tsx            # ⚠️ 미니 랭킹 (API 연동 필요)
│   ├── stock-search.tsx            # 🆕 종목 검색 (자동완성)
│   ├── notification-dropdown.tsx   # 🆕 알림 드롭다운
│   └── dividend-calculator.tsx     # 🆕 배당금 계산기 위젯
│
├── 📁 layout/
│   └── app-layout.tsx              # ✅ 앱 레이아웃 (Header + Sidebar + Main)
│
├── 📁 common/                      # 🆕 공통 컴포넌트
│   ├── loading-spinner.tsx         # 🆕 로딩 스피너
│   ├── skeleton-card.tsx           # 🆕 스켈레톤 UI
│   ├── empty-state.tsx             # 🆕 빈 상태
│   ├── error-boundary.tsx          # 🆕 에러 바운더리
│   └── price-badge.tsx             # 🆕 가격 변동 배지
│
├── 📁 ui/                          # ✅ Shadcn/UI (New York 스타일)
│   ├── accordion.tsx               # ✅
│   ├── alert-dialog.tsx            # ✅
│   ├── avatar.tsx                  # ✅
│   ├── badge.tsx                   # ✅
│   ├── button.tsx                  # ✅
│   ├── card.tsx                    # ✅
│   ├── dialog.tsx                  # ✅
│   ├── dropdown-menu.tsx           # ✅
│   ├── input.tsx                   # ✅
│   ├── scroll-area.tsx             # ✅
│   ├── select.tsx                  # ✅
│   ├── separator.tsx               # ✅
│   ├── sheet.tsx                   # ✅
│   ├── skeleton.tsx                # ✅
│   ├── tabs.tsx                    # ✅
│   ├── toast.tsx                   # ✅
│   ├── tooltip.tsx                 # ✅
│   └── ... (기타 50+ 컴포넌트)
│
└── theme-provider.tsx              # ✅ 테마 프로바이더

📁 hooks/
├── use-mobile.ts                   # ✅ 모바일 감지
├── use-toast.ts                    # ✅ 토스트 알림
├── use-auth.ts                     # 🆕 인증 상태
├── use-websocket.ts                # 🆕 WebSocket 연결
├── use-stock-price.ts              # 🆕 실시간 가격 구독
├── use-ai-chat.ts                  # 🆕 AI SSE 스트리밍
├── use-notifications.ts            # 🆕 알림
└── use-portfolio.ts                # 🆕 포트폴리오

📁 stores/                          # 🆕 Zustand 스토어 (전체 신규)
├── auth-store.ts                   # 🆕 인증 상태
├── user-store.ts                   # 🆕 유저/지갑 상태
├── stock-store.ts                  # 🆕 주가 상태
├── portfolio-store.ts              # 🆕 포트폴리오 상태
├── notification-store.ts           # 🆕 알림 상태
└── ui-store.ts                     # 🆕 UI 상태 (모달, 사이드바)

📁 lib/
├── utils.ts                        # ✅ 유틸리티 (cn 함수)
├── mock-data.ts                    # ✅ Mock 데이터 (개발용)
├── 📁 api/                         # 🆕 API 클라이언트 (전체 신규)
│   ├── index.ts                    # 🆕 Axios 인스턴스 + 인터셉터
│   ├── auth.ts                     # 🆕 인증 API
│   ├── user.ts                     # 🆕 사용자 API
│   ├── trade.ts                    # 🆕 거래 API
│   ├── stock.ts                    # 🆕 주식 API
│   ├── game.ts                     # 🆕 게임/가챠 API
│   ├── chat.ts                     # 🆕 AI 상담 API
│   └── notification.ts             # 🆕 알림 API
├── stomp-client.ts                 # 🆕 STOMP WebSocket 클라이언트
├── saju-calculator.ts              # 🆕 사주/오행 계산 유틸
└── constants.ts                    # 🆕 상수 정의

📁 types/                           # 🆕 TypeScript 타입 (전체 신규)
├── user.ts                         # 🆕 사용자/인증 타입
├── stock.ts                        # 🆕 주식/시세 타입
├── trade.ts                        # 🆕 거래 타입
├── game.ts                         # 🆕 게임/가챠 타입
├── chat.ts                         # 🆕 AI 상담 타입
└── api.ts                          # 🆕 API 응답 타입

📁 providers/                       # 🆕 Context Providers (전체 신규)
├── auth-provider.tsx               # 🆕 인증 Provider
└── websocket-provider.tsx          # 🆕 WebSocket Provider

📁 public/                          # ✅ 정적 파일
├── anime-businessman-avatar.jpg    # ✅ 아바타 이미지
├── anime-golden-king-avatar.jpg    # ✅
├── anime-style-investor-avatar-character.jpg  # ✅
├── mystical-wizard-avatar.jpg      # ✅
├── golden-crown-pixel-art.jpg      # ✅ 가챠 아이템 이미지
├── fire-aura-effect.jpg            # ✅
├── high-tech-glasses-pixel-art.jpg # ✅
├── lucky-charm-pixel-art.jpg       # ✅
├── icon.svg                        # ✅ 파비콘
├── icon-dark-32x32.png             # ✅
├── icon-light-32x32.png            # ✅
└── apple-icon.png                  # ✅

📁 styles/
└── globals.css                     # ⚠️ app/globals.css와 통합 필요
```

### 6.3 페이지 라우팅

| 경로 | 파일 | 인증 | 상태 | 설명 |
|------|------|------|------|------|
| `/` | `app/page.tsx` | ✅ | ✅ 구현됨 | 대시보드 (드래그 위젯) |
| `/oracle` | `app/oracle/page.tsx` | ✅ | ✅ 구현됨 | AI 도사 (사주/별자리) |
| `/gacha` | `app/gacha/page.tsx` | ✅ | ✅ 구현됨 | 가챠 (캡슐 토이) |
| `/ranking` | `app/ranking/page.tsx` | ✅ | ✅ 구현됨 | 랭킹 (포디움) |
| `/mypage` | `app/mypage/page.tsx` | ✅ | ✅ 구현됨 | 마이페이지 (4탭) |
| `/login` | `app/login/page.tsx` | ❌ | 🆕 필요 | 일반 + Google + Kakao OAuth |
| `/signup` | `app/signup/page.tsx` | ❌ | 🆕 필요 | 일반 회원가입 |
| `/onboarding` | `app/onboarding/page.tsx` | ✅ | 🆕 필요 | 생년월일 입력 |
| `/calculator` | `app/calculator/page.tsx` | ✅ | 🆕 필요 | 배당금 계산기 |

### 6.4 현재 구현된 주요 기능

#### 대시보드 (`app/page.tsx`)
- ✅ 드래그 가능한 위젯 그리드 (4 cols)
- ✅ 편집 모드 토글 (길게 누르기)
- ✅ 위젯: 차트, 아바타, 매수/매도, 관심종목, 포트폴리오, AI챗봇, 랭킹

#### 가챠 (`app/gacha/page.tsx`)
- ✅ 캡슐 토이 머신 UI (유리 돔, 레버)
- ✅ 레버 돌리기 애니메이션
- ✅ 캡슐 색상 (등급별)
- ✅ 결과 표시 + 희귀도 효과
- ⚠️ 서버 API 연동 필요

#### AI 도사 (`app/oracle/page.tsx`)
- ✅ 채팅 UI (메시지 버블)
- ✅ 사주/별자리/오하아사 탭
- ✅ 오행 밸런스 표시
- ✅ 시간대별 투자 운세
- ⚠️ SSE 스트리밍 연동 필요

#### 마이페이지 (`app/mypage/page.tsx`)
- ✅ 프로필 헤더 (아바타, 통계)
- ✅ 개요 탭 (예수금, 평가금액, 실현수익)
- ✅ 인벤토리 탭 (아이템 그리드)
- ✅ 아바타 꾸미기 탭
- ✅ 설정 탭 (닉네임, 공개 설정)

#### 랭킹 (`app/ranking/page.tsx`)
- ✅ 포디움 (Top 3)
- ✅ 전체 순위 리스트
- ✅ 기간 탭 (월간/주간/전체)
- ⚠️ 실시간 데이터 연동 필요

### 6.5 컴포넌트 계층

```
<html lang="ko">
  <body>
    <RootLayout>                         <!-- app/layout.tsx -->
      <Analytics />                      <!-- Vercel Analytics -->
      
      <!-- 인증 필요 페이지 -->
      <AppLayout>                        <!-- components/layout/app-layout.tsx -->
        <DashboardHeader />              <!-- 상단 헤더 -->
        <div className="flex">
          <DashboardSidebar />           <!-- 좌측 사이드바 (w-64) -->
          <main className="flex-1">
            {children}                   <!-- 페이지 컨텐츠 -->
          </main>
        </div>
      </AppLayout>
      
    </RootLayout>
  </body>
</html>
```

### 6.6 추가 필요 Provider 구조 (구현 예정)

```
<RootLayout>
  <ThemeProvider>                        <!-- 🆕 다크/라이트 모드 -->
    <AuthProvider>                       <!-- 🆕 next-auth 세션 -->
      <WebSocketProvider>                <!-- 🆕 STOMP 연결 -->
        <AppLayout>
          <DashboardHeader />
          <DashboardSidebar />
          <main>{children}</main>
        </AppLayout>
      </WebSocketProvider>
    </AuthProvider>
  </ThemeProvider>
</RootLayout>
```

---

## 7. 핵심 기능 상세

### 7.1 실시간 주가 시스템

#### 데이터 흐름

```
1. [Finnhub WebSocket] → 실시간 시세 수신
2. [Spring Boot] → Redis에 캐시 & Pub/Sub 발행
3. [STOMP Broker] → 구독 클라이언트에 전달
4. [React] → UI 업데이트
```

#### 구독 전략 (Lazy Loading)

- 사용자가 **현재 화면에 띄운 종목**만 구독
- 관심 종목 (Watchlist) + 보유 종목 (Portfolio)
- 화면 이탈 시 구독 해제

#### Redis 키 구조

```
stock:{ticker}:price          # 현재가 (String)
stock:{ticker}:quote          # 상세 시세 (Hash)
stock:subscriptions           # 구독 중인 종목 (Set)
```

### 7.2 매수/매도 트랜잭션

#### 매수 흐름

```
1. 클라이언트 → POST /api/v1/trade/order (BUY)
2. 서버 → Redis에서 현재가 조회
3. 서버 → PostgreSQL wallet SELECT FOR UPDATE (Row Lock)
4. 잔고 확인: cash_balance >= price * quantity
5. wallet.cash_balance 차감
6. portfolio UPSERT (평단가 재계산)
7. trade_logs INSERT
8. wallet.total_assets 재계산
9. COMMIT
10. WebSocket → 클라이언트에 체결 알림
```

#### 매도 흐름

```
1. 클라이언트 → POST /api/v1/trade/order (SELL)
2. 서버 → Redis에서 현재가 조회
3. 서버 → PostgreSQL portfolio SELECT FOR UPDATE
4. 보유 수량 확인: quantity >= 요청 수량
5. 실현 손익 계산: (현재가 - 평단가) * 수량
6. portfolio 수량 차감 (0이면 DELETE)
7. wallet.cash_balance 증가
8. wallet.realized_profit 증가 (양수인 경우)
9. trade_logs INSERT (realized_pnl 포함)
10. COMMIT
11. WebSocket → 클라이언트에 체결 알림
```

#### 평단가 계산 공식

```
새 평단가 = (기존수량 × 기존평단가 + 신규수량 × 체결가) / (기존수량 + 신규수량)
```

### 7.3 사주 알고리즘

#### 오행 매핑

| 출생년도 끝자리 | 오행 | 한자 | 색상 | 투자 성향 |
|----------------|------|------|------|----------|
| 4, 5 | 목(Wood) | 木 | 파랑/초록 | 성장주, 장기투자 |
| 6, 7 | 화(Fire) | 火 | 빨강 | 열정적, 고위험/고수익 |
| 8, 9 | 토(Earth) | 土 | 노랑 | 안정적, 배당주 |
| 0, 1 | 금(Gold) | 金 | 흰색 | 결단력, 단기매매 |
| 2, 3 | 수(Water) | 水 | 검정 | 유연함, 분산투자 |

#### 띠 계산

```java
private static final String[] ZODIAC = {
    "RAT", "OX", "TIGER", "RABBIT", "DRAGON", "SNAKE",
    "HORSE", "GOAT", "MONKEY", "ROOSTER", "DOG", "PIG"
};

public String calculateZodiac(int birthYear) {
    return ZODIAC[(birthYear - 4) % 12];
}
```

### 7.4 배당금/세금 계산

#### 공식

```
예상 연간 배당금 = Σ(보유 수량 × 주당 연간 배당금)
배당 수익률 = (연간 배당금 / 현재 평가금액) × 100
원천징수세(미국) = 배당금 × 15%
실수령액 = 배당금 × 85%
```

---

## 8. 게이미피케이션 시스템

### 8.1 코인 시스템

#### 코인 획득 경로

| 경로 | 획득량 | 조건 |
|------|--------|------|
| 실현 수익 변환 | 수익금 × 10 | 매도 후 수동 변환 |
| 일일 출석 | 100 코인 | 매일 1회 |
| 첫 거래 보너스 | 500 코인 | 계정당 1회 |

#### 코인 사용처

| 용도 | 비용 |
|------|------|
| 일반 가챠 | 500 코인 |
| 프리미엄 가챠 | 1,000 코인 |
| 아바타 커스텀 슬롯 | 300 코인 |

### 8.2 가챠 시스템

#### 확률 테이블

| 등급 | 확률 | 색상 |
|------|------|------|
| Common | 60% | 회색 |
| Rare | 25% | 파랑 |
| Epic | 12% | 보라 |
| Legendary | 3% | 금색 |

#### 아이템 카테고리

| 카테고리 | 설명 | 예시 |
|----------|------|------|
| COSTUME | 의상 | 정장, 투자자 망토 |
| ACCESSORY | 액세서리 | 왕관, 안경, 모자 |
| AURA | 오라 효과 | 불꽃 오라, 얼음 오라 |
| BACKGROUND | 배경 | 월스트리트, 동양풍 |

### 8.3 랭킹 시스템

#### 랭킹 기준

- **주간 랭킹:** 해당 주 수익률
- **월간 랭킹:** 해당 월 수익률
- **전체 랭킹:** 가입 후 누적 수익률

#### 랭킹 보상 (월간 기준)

| 순위 | 보상 |
|------|------|
| 1위 | 5,000 코인 + Legendary 아이템 1개 |
| 2-3위 | 3,000 코인 + Epic 아이템 1개 |
| 4-10위 | 1,000 코인 + Rare 아이템 1개 |
| 11-50위 | 500 코인 |

### 8.4 아바타 리액션

| 수익률 | 아바타 상태 | 시각 효과 |
|--------|------------|----------|
| +10% 이상 | Ecstatic (환희) | 금빛 오라 + 빛 파티클 |
| +5% ~ +10% | Happy (행복) | 황금 테두리 |
| -5% ~ +5% | Neutral (평온) | 기본 상태 |
| -5% ~ -10% | Sad (슬픔) | 어두운 필터 |
| -10% 이하 | Crying (울음) | 비 이펙트 + 진동 |

---

## 9. AI 시스템

### 9.1 AI 도사 상담

#### System Prompt

```
당신은 50년 경력의 월스트리트 트레이더이자 동양 철학자 "주식 도사"입니다.

사용자 정보:
- 사주 오행: {saju_element}
- 띠: {zodiac_sign}
- 보유 종목: {portfolio}
- 관심 종목: {interested_ticker}

규칙:
1. 반드시 사주와 오행을 투자 조언에 연결하세요.
2. 신비롭고 은유적인 말투를 사용하세요 ("허허", "자네", "~하겠구먼").
3. 실제 재무 데이터(PER, PBR, RSI 등)를 언급하세요.
4. 투자는 본인 책임이라는 면책 조항을 암시하세요.
5. 답변은 300자 내외로 간결하게 하세요.

예시:
"화(火) 기운이 강한 자네에게 테슬라의 불꽃같은 변동성은 위험할 수 있네. 
현재 RSI가 72로 과매수 구간이니, 조급함을 다스리고 조정을 기다려보게나."
```

#### 컨텍스트 주입

```json
{
  "user": {
    "sajuElement": "FIRE",
    "zodiacSign": "DRAGON"
  },
  "portfolio": [
    {"ticker": "AAPL", "quantity": 10, "profitPercent": 5.2}
  ],
  "stockData": {
    "ticker": "TSLA",
    "price": 282.08,
    "change": -2.92,
    "rsi": 45.3,
    "per": 78.5
  }
}
```

### 9.2 아바타 생성 (Stable Diffusion)

#### 프롬프트 템플릿

```
A portrait of an anime-style investor character,
{element_style} theme,
professional attire,
confident expression,
stock charts in background,
high quality, detailed, 4k
```

#### 오행별 스타일

| 오행 | element_style |
|------|--------------|
| 목(Wood) | forest green, nature, growth |
| 화(Fire) | fiery red, flames, passion |
| 토(Earth) | golden yellow, stable, mountain |
| 금(Gold) | silver white, metallic, sharp |
| 수(Water) | deep blue, flowing, wisdom |

---

## 10. 실시간 통신

### 10.1 WebSocket 엔드포인트

```
ws://api.madcamp02.com/ws
```

### 10.2 STOMP 채널

| 채널 | 설명 | 메시지 형식 |
|------|------|------------|
| `/topic/stock.{ticker}` | 종목별 시세 | `{ticker, price, change, volume}` |
| `/user/queue/trade` | 개인 체결 알림 | `{orderId, ticker, status, ...}` |
| `/user/queue/notification` | 개인 알림 | `{type, title, message}` |
| `/topic/ranking` | 랭킹 변동 | `{rankings: [...]}` |

### 10.3 메시지 예시

#### 시세 업데이트

```json
{
  "ticker": "AAPL",
  "price": 198.45,
  "change": 3.21,
  "changePercent": 1.64,
  "volume": 58200000,
  "timestamp": "2026-01-16T10:30:00Z"
}
```

#### 체결 알림

```json
{
  "type": "TRADE_COMPLETE",
  "orderId": 12345,
  "ticker": "AAPL",
  "tradeType": "BUY",
  "quantity": 10,
  "price": 198.45,
  "message": "AAPL 10주 매수 체결"
}
```

---

## 11. 보안 및 인증

### 11.1 인증 방식 (3가지) 🆕

| 방식 | provider | 엔드포인트 | 설명 |
|------|----------|-----------|------|
| 일반 회원가입 | `LOCAL` | `/auth/signup` | 이메일 + 비밀번호 + 닉네임 |
| 일반 로그인 | `LOCAL` | `/auth/login` | 이메일 + 비밀번호 |
| Google OAuth | `GOOGLE` | `/auth/oauth/google` | Google ID Token |
| Kakao OAuth | `KAKAO` | `/auth/oauth/kakao` | Kakao Access Token |

### 11.2 인증 흐름

#### 일반 회원가입/로그인 🆕
```
1. 회원가입: POST /auth/signup {email, password, nickname}
2. 서버 → 이메일 중복 체크 → BCrypt 암호화 → User 생성
3. JWT Access Token (1h) + Refresh Token (7d) 발급
---
4. 로그인: POST /auth/login {email, password}
5. 서버 → BCrypt 비밀번호 검증 → JWT 발급
```

#### OAuth 로그인 (Google / Kakao)
```
1. 사용자 → Google/Kakao 로그인 → Token 획득
2. POST /auth/oauth/google (또는 /kakao) {provider, idToken/accessToken}
3. 서버 → OAuth Provider에 토큰 검증 요청
4. 서버 → 사용자 조회/생성 → JWT 발급
```

### 11.3 JWT 구조

```json
{
  "sub": "1",
  "email": "user@gmail.com",
  "nickname": "투자도사",
  "provider": "LOCAL",
  "role": "USER",
  "iat": 1705392000,
  "exp": 1705395600
}
```

### 11.4 보안 설정

| 항목 | 설정 |
|------|------|
| CORS | 프론트엔드 도메인만 허용 |
| Rate Limiting | 100 req/min per IP |
| SQL Injection | Prepared Statement 사용 |
| XSS | CSP 헤더 설정 |
| HTTPS | TLS 1.3 강제 |
| Password | 🆕 BCrypt 암호화 (일반 회원) |

---

## 12. UI/UX 가이드라인

### 12.1 컬러 팔레트

```css
:root {
  /* 기본 색상 */
  --background: oklch(0.13 0.01 260);      /* 다크 배경 */
  --foreground: oklch(0.95 0 0);           /* 텍스트 */
  --primary: oklch(0.75 0.18 85);          /* 금색 (메인) */
  --accent: oklch(0.65 0.2 145);           /* 초록 (수익) */
  
  /* 투자 색상 */
  --bull: oklch(0.65 0.2 145);             /* 상승 (초록) */
  --bear: oklch(0.55 0.22 25);             /* 하락 (빨강) */
  --gold: oklch(0.8 0.16 85);              /* 황금 */
  --oracle: oklch(0.6 0.18 280);           /* AI 도사 (보라) */
  
  /* 등급 색상 */
  --rarity-common: #6b7280;
  --rarity-rare: #3b82f6;
  --rarity-epic: #a855f7;
  --rarity-legendary: #f59e0b;
}
```

### 12.2 타이포그래피

```css
--font-sans: "Geist", system-ui, sans-serif;
--font-mono: "Geist Mono", monospace;
```

### 12.3 애니메이션

```css
/* 금빛 펄스 (수익 +5% 이상) */
@keyframes pulse-gold {
  0%, 100% { box-shadow: 0 0 20px oklch(0.8 0.16 85 / 0.3); }
  50% { box-shadow: 0 0 40px oklch(0.8 0.16 85 / 0.5); }
}

/* 플로팅 (아바타) */
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

/* 체결 성공 */
@keyframes trade-success {
  0% { transform: scale(1); }
  50% { transform: scale(1.1); background: var(--bull); }
  100% { transform: scale(1); }
}

/* 손실 떨림 */
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-5px); }
  75% { transform: translateX(5px); }
}
```

### 12.4 반응형 브레이크포인트

```css
/* Mobile */   @media (max-width: 640px)
/* Tablet */   @media (min-width: 641px) and (max-width: 1024px)
/* Desktop */  @media (min-width: 1025px)
```

---

## 13. 테스트 전략

### 13.1 테스트 레벨

| 레벨 | 도구 | 커버리지 목표 |
|------|------|-------------|
| 단위 테스트 | JUnit 5, Jest | 80% |
| 통합 테스트 | Spring Test, Playwright | 60% |
| E2E 테스트 | Playwright | 핵심 시나리오 |
| 성능 테스트 | k6, Artillery | 1000 동시 사용자 |

### 13.2 테스트 시나리오

#### 핵심 시나리오

1. **로그인 → 온보딩 → 대시보드**
2. **종목 검색 → 매수 → 포트폴리오 확인**
3. **매도 → 실현 수익 확인 → 코인 변환**
4. **가챠 뽑기 → 인벤토리 확인 → 아바타 장착**
5. **AI 상담 → 스트리밍 응답 확인**

---

## 14. 배포 전략

### 14.1 환경 구성

| 환경 | 용도 | URL |
|------|------|-----|
| Development | 개발 | localhost:3000 |
| Staging | QA 테스트 | staging.madcamp02.com |
| Production | 운영 | madcamp02.com |

### 14.2 인프라

```
[Vercel] ← Frontend (Next.js)
[AWS EC2 / ECS] ← Backend (Spring Boot, FastAPI)
[AWS RDS] ← PostgreSQL
[AWS ElastiCache] ← Redis
[Cloudflare] ← CDN, DDoS Protection
```

### 14.3 환경 변수

#### Frontend (.env.local)

```env
NEXT_PUBLIC_API_URL=https://api.madcamp02.com
NEXT_PUBLIC_WS_URL=wss://api.madcamp02.com/ws
NEXTAUTH_URL=https://madcamp02.com
NEXTAUTH_SECRET=xxx

# Google OAuth
GOOGLE_CLIENT_ID=xxx
GOOGLE_CLIENT_SECRET=xxx

# 🆕 Kakao OAuth
KAKAO_CLIENT_ID=xxx
KAKAO_CLIENT_SECRET=xxx
```

#### Backend (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/madcamp02-backend
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  redis:
    host: localhost
    port: 6379

finnhub:
  api-key: ${FINNHUB_API_KEY}
  websocket-url: wss://ws.finnhub.io

jwt:
  secret: ${JWT_SECRET}
  access-expiration: 3600000
  refresh-expiration: 604800000

# 🆕 OAuth 설정
oauth:
  google:
    client-id: ${GOOGLE_CLIENT_ID}
  kakao:
    client-id: ${KAKAO_CLIENT_ID}
    client-secret: ${KAKAO_CLIENT_SECRET}
```

---

## 15. 개발 로드맵

### Phase 1: 인프라 구축 (Week 1)

| 태스크 | 담당 | 상태 |
|--------|------|------|
| Docker Compose 환경 구성 | BE | ⬜ |
| PostgreSQL 스키마 생성 | BE | ⬜ |
| Spring Boot 프로젝트 세팅 | BE | ⬜ |
| Next.js 인증 시스템 | FE | ⬜ |
| Zustand 스토어 구축 | FE | ⬜ |

### Phase 2: 핵심 기능 (Week 2)

| 태스크 | 담당 | 상태 |
|--------|------|------|
| Finnhub WebSocket 연동 | BE | ⬜ |
| 매수/매도 API 구현 | BE | ⬜ |
| STOMP 클라이언트 구현 | FE | ⬜ |
| 캔들 차트 구현 | FE | ⬜ |
| 거래 UI 연동 | FE | ⬜ |

### Phase 3: AI 시스템 (Week 3)

| 태스크 | 담당 | 상태 |
|--------|------|------|
| FastAPI AI 서버 구축 | AI | ⬜ |
| 사주 알고리즘 구현 | BE | ⬜ |
| SSE 스트리밍 연동 | FE | ⬜ |
| 상담 UI 완성 | FE | ⬜ |

### Phase 4: 게이미피케이션 (Week 4)

| 태스크 | 담당 | 상태 |
|--------|------|------|
| 가챠 API 구현 | BE | ⬜ |
| 랭킹 시스템 구현 | BE | ⬜ |
| 인벤토리/아바타 연동 | FE | ⬜ |
| 가챠 UI 연동 | FE | ⬜ |

### Phase 5: 고도화 (Week 5)

| 태스크 | 담당 | 상태 |
|--------|------|------|
| 배당금 계산기 | BE/FE | ⬜ |
| 알림 시스템 | BE/FE | ⬜ |
| 성능 최적화 | BE/FE | ⬜ |
| E2E 테스트 | QA | ⬜ |
| 배포 및 모니터링 | DevOps | ⬜ |

---

## 📎 부록

### A. 용어 사전

| 용어 | 설명 |
|------|------|
| 오행 | 동양 철학의 다섯 가지 원소 (목, 화, 토, 금, 수) |
| 사주 | 생년월일시를 기반으로 한 동양 운명학 |
| 가챠 | 랜덤 아이템 뽑기 게임 |
| 평단가 | 평균 매수 단가 |
| 실현 손익 | 매도하여 확정된 손익 |
| 평가 손익 | 매도 전 현재가 기준 손익 |

### B. 에러 코드

> 에러 응답은 `ErrorResponse` DTO를 통해 일관된 형식으로 반환됩니다.
> 
> ```json
> {
>   "timestamp": "2026-01-17T12:00:00",
>   "status": 400,
>   "error": "TRADE_001",
>   "message": "잔고가 부족합니다."
> }
> ```

| 코드 | 설명 | HTTP 상태 |
|------|------|----------|
| **인증 (AUTH)** |||
| AUTH_001 | 토큰 만료 | 401 |
| AUTH_002 | 유효하지 않은 토큰 | 401 |
| AUTH_003 | 권한 없음 | 403 |
| AUTH_004 | 사용자 없음 | 404 |
| AUTH_005 | Google 토큰 검증 실패 | 401 |
| AUTH_006 | 🆕 Kakao 토큰 검증 실패 | 401 |
| AUTH_007 | 🆕 비밀번호 불일치 | 401 |
| AUTH_008 | 🆕 이메일 중복 (회원가입) | 409 |
| **거래 (TRADE)** |||
| TRADE_001 | 잔고 부족 | 400 |
| TRADE_002 | 보유 수량 부족 | 400 |
| TRADE_003 | 거래 시간 외 | 400 |
| TRADE_004 | 유효하지 않은 종목 | 400 |
| **게임 (GAME)** |||
| GAME_001 | 코인 부족 | 400 |
| GAME_002 | 이미 보유한 아이템 | 409 |
| GAME_003 | 아이템 없음 | 404 |
| **사용자 (USER)** |||
| USER_001 | 사용자 없음 | 404 |
| USER_002 | 이미 존재하는 사용자 | 409 |
| **서버 (SERVER)** |||
| SERVER_001 | 내부 서버 오류 | 500 |
| SERVER_002 | 외부 API 호출 실패 | 503 |
| **검증 (VALIDATION)** |||
| VALIDATION_ERROR | 입력값 검증 실패 | 400 |
| BIND_ERROR | 요청 데이터 바인딩 실패 | 400 |

### C. 예외 처리 구조

```
com.madcamp02.exception/
├── ErrorCode.java              # 에러 코드 enum (명세서 순서 기준)
├── ErrorResponse.java          # 에러 응답 DTO (팩토리 메서드 포함)
├── BusinessException.java      # 기본 비즈니스 예외 (부모 클래스)
├── AuthException.java          # 인증 예외 (AUTH_001~005)
├── TradeException.java         # 거래 예외 (TRADE_001~004)
├── GameException.java          # 게임 예외 (GAME_001~003)
├── UserException.java          # 사용자 예외 (USER_001~002)
└── GlobalExceptionHandler.java # 전역 예외 핸들러
```

---

**문서 버전:** 2.2 (🆕 카카오 OAuth, 일반 회원가입/로그인 추가)  
**최종 수정일:** 2026-01-17  
**작성자:** MadCamp02 개발팀
