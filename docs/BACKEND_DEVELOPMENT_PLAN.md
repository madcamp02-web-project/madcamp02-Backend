# ⚙️ MadCamp02: 백엔드 개발 계획서

**Ver 2.7 - Backend Development Blueprint (Spec-Driven Alignment)**

---

## 📝 변경 이력

| 버전 | 날짜 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| 1.0 | 2026-01-15 | 초기 명세서 작성 | MadCamp02 |
| 2.0 | 2026-01-16 | Exception 구조 정리, ErrorResponse DTO 추가 | MadCamp02 |
| 2.1 | 2026-01-17 | 카카오 OAuth, 일반 회원가입/로그인 추가 | MadCamp02 |
| 2.2 | 2026-01-17 | OAuth2 백엔드 주도 방식으로 변경 | MadCamp02 |
| 2.3 | 2026-01-18 | 프론트엔드 구현 현황 반영 (Market, Shop, Trade 페이지 API 추가) | MadCamp02 |
| 2.4 | 2026-01-18 | 통합 명세서 및 프론트 계획 완전 동기화 | MadCamp02 |
| 2.5 | 2026-01-18 | 통합 명세서(FULL_SPECIFICATION)와 버전 동기화 및 최종 점검 | MadCamp02 |
| **2.6** | **2026-01-18** | **하이브리드 인증 방식(Frontend/Backend Driven) 명세 반영 및 구현 현황 점검** | **MadCamp02** |
| **2.7** | **2026-01-18** | **3개 문서 버전 동기화 및 엔드포인트/용어 문구 정리(프론트 연동 기준)** | **MadCamp02** |

### Ver 2.6 주요 변경 사항

1.  **인증 아키텍처 현실화**: 실제 코드(`SecurityConfig`, `AuthController`)에 구현된 **하이브리드 인증(Hybrid Auth)** 방식을 명세에 반영. 백엔드 주도(Redirect)와 프론트엔드 주도(Token API) 방식을 모두 지원함.
2.  **구현 현황 추적**: 현재 코드베이스 상태와 명세서 간의 갭(Gap)을 분석하여 '구현 현황' 섹션 추가.

### Ver 2.7 주요 변경 사항

1.  **정합성 기준 고정**: `FULL_SPECIFICATION` 및 프론트/백엔드 개발 계획서(2.7)를 기준으로 API/실시간/인증 인터페이스를 고정하고, 구현을 그 기준에 맞춰 진행.
2.  **프론트 연동 우선순위 반영**: (1) Hybrid Auth Callback/토큰 저장 → (2) `lib/api` 모듈화 및 401 Refresh/Retry → (3) 페이지 실데이터 치환 → (4) STOMP/SSE 실시간 순으로 연동.

---

## 📋 목차

1. [시스템 개요](#1-시스템-개요)
2. [아키텍처 설계](#2-아키텍처-설계)
3. [기술 스택](#3-기술-스택)
4. [프로젝트 구조](#4-프로젝트-구조)
5. [데이터베이스 설계](#5-데이터베이스-설계)
6. [API 상세 설계](#6-api-상세-설계)
7. [인증 및 보안](#7-인증-및-보안)
8. [실시간 통신](#8-실시간-통신)
9. [외부 API 연동](#9-외부-api-연동)
10. [비즈니스 로직](#10-비즈니스-로직)
11. [구현 현황 (Status)](#11-구현-현황-status)

---

## 1. 시스템 개요

### 1.1 백엔드 역할

MadCamp02 백엔드는 다음 핵심 기능을 담당합니다:

1. **사용자 관리**: OAuth2 인증(Hybrid), JWT 토큰 발급, 프로필 및 사주 데이터 관리
2. **모의투자 엔진**: 실시간 호가/체결 처리, 포트폴리오 평가, 매수/매도 트랜잭션 관리
3. **실시간 데이터**: Finnhub WebSocket → Redis → 클라이언트 중계 (주가, 호가)
4. **게이미피케이션**: 가챠(확률형 아이템), 아이템 인벤토리, 랭킹 산정
5. **AI 상담**: 사주 기반 투자 조언 (FastAPI 연동)

---

## 2. 아키텍처 설계

### 2.1 레이어드 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                     PRESENTATION LAYER                           │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  Controllers (REST API)                                     │ │
│  │  ├── AuthController, UserController, MarketController       │ │
│  │  ├── TradeController, StockController, GameController       │ │
│  │  └── ChatController, NotificationController, CalcController │ │
│  └─────────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  WebSocket Handlers (STOMP)                                 │ │
│  │  ├── StockPriceHandler (주가/호가)                          │ │
│  │  └── TradeNotificationHandler (체결 알림)                   │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      SERVICE LAYER                               │
│  ├── AuthService, UserService, MarketService                     │
│  ├── TradeService, StockService, PortfolioService                │
│  ├── GachaService, InventoryService, RankingService              │
│  └── ChatService, SajuService, DividendService                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. 기술 스택

### 3.1 Core Server (Spring Boot)

| 기술 | 버전 | 용도 |
|------|------|------|
| Java | 21 LTS | 언어 |
| Spring Boot | 3.2.x | 프레임워크 |
| Spring Security | 6.x | 인증/인가 (OAuth2 Client) |
| Spring Data JPA | 3.x | ORM |
| Spring WebSocket | 6.x | 실시간 통신 (STOMP) |
| QueryDSL | Latest | 동적 쿼리 (랭킹/검색) |

### 3.2 AI Server (FastAPI)

| 기술 | 버전 | 용도 |
|------|------|------|
| Python | 3.11+ | 언어 |
| FastAPI | 0.100+ | API 프레임워크 |
| LangChain | Latest | LLM 오케스트레이션 |

---

## 4. 프로젝트 구조 (패키지)

```
📦 src/main/java/com/madcamp02
 ┣ 📂 config            # Security, Swagger, WebSocket, Redis 설정
 ┣ 📂 controller        # REST API 컨트롤러
 ┣ 📂 domain            # Entity 및 Repository
 ┃ ┣ 📂 user
 ┃ ┣ 📂 stock
 ┃ ┣ 📂 trade
 ┃ ┣ 📂 item
 ┃ ┗ 📂 chat
 ┣ 📂 dto               # Request/Response DTO
 ┣ 📂 service           # 비즈니스 로직
 ┣ 📂 security          # JWT, OAuth2 핸들러
 ┣ 📂 exception         # Global Exception Handler
 ┗ 📂 external          # Finnhub, AI Server 연동 클라이언트
```

---

## 5. 데이터베이스 설계

### 5.1 ERD

```mermaid
erDiagram
    Users ||--|| Wallet : has
    Users ||--o{ Portfolio : owns
    Users ||--o{ TradeLogs : executes
    Users ||--o{ Inventory : has
    Users ||--o{ ChatHistory : chats
    Inventory }|--|| Items : contains

    Users {
        Long user_id PK
        String email
        String nickname
        String provider
        Date birth_date
        String saju_element
        Boolean is_public "🆕 프로필 공개 여부"
        Boolean is_ranking_joined "🆕 랭킹 참여 여부"
    }

    Items {
        Long item_id PK
        String name
        String category "NAMEPLATE, AVATAR, THEME"
        String rarity
        Double probability
    }
```

### 5.2 주요 테이블 변경사항

1.  **Users 테이블**
    *   `is_public` (BOOLEAN, Default TRUE): 마이페이지 설정 반영
    *   `is_ranking_joined` (BOOLEAN, Default TRUE): 랭킹 참여 여부

2.  **Items 테이블**
    *   `category` 컬럼 ENUM: `NAMEPLATE` (이름표/테두리), `AVATAR` (아바타 장식), `THEME` (앱 테마)
    *   프론트엔드 `Shop` 페이지 탭 구성과 정확히 일치

---

## 6. API 상세 설계

### 6.1 Market API (신규)

프론트엔드 `/market` 페이지 지원

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/v1/market/indices` | 주요 지수 (KOSPI, NASDAQ 등) 조회 |
| GET | `/api/v1/market/news` | 최신 시장 뉴스 조회 |
| GET | `/api/v1/market/movers` | 급등/급락/거래량 상위 종목 조회 |

### 6.2 Game/Shop API (확장)

프론트엔드 `/shop`, `/mypage` 페이지 지원

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/v1/game/items` | 아이템 목록 조회 (Query Param: `category` 지원) |
| POST | `/api/v1/game/gacha` | 가챠 뽑기 (코인 차감 및 인벤토리 지급) |
| GET | `/api/v1/game/inventory` | 내 인벤토리 조회 |
| PUT | `/api/v1/game/equip/{itemId}` | 아이템 장착/해제 |

### 6.3 Trade API (확장)

프론트엔드 `/trade` 및 `/portfolio` 페이지 지원

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/v1/stock/orderbook/{ticker}` | 호가(Orderbook) 데이터 조회 |
| GET | `/api/v1/trade/available-balance` | 현재 매수 가능 금액 조회 |
| GET | `/api/v1/trade/portfolio` | 상세 포트폴리오 (평가손익 포함) |

### 6.4 Calculation API (신규) 🆕

프론트엔드 `/calculator` 페이지 지원

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/v1/calc/dividend` | 보유 종목 기반 예상 배당금 및 세금 계산 |
| GET | `/api/v1/calc/tax` | 실현 수익 기반 예상 양도소득세 계산 |

### 6.5 Auth API (기존 유지)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/v1/auth/signup` | 일반 회원가입 |
| POST | `/api/v1/auth/login` | 일반 로그인 |
| GET | `/api/v1/auth/me` | 현재 사용자 정보 (사주 정보 포함) |

---

## 7. 인증 및 보안

### 7.1 Hybrid OAuth2 Architecture

MadCamp02는 다양한 클라이언트 환경(Web, Mobile, External)을 지원하기 위해 **두 가지 인증 방식을 모두 지원**합니다.

#### A. Backend-Driven (표준 웹 방식)
*   **흐름**: `GET /oauth2/authorization/kakao` → Provider Login → Redirect to Backend → Redirect to Frontend with Token.
*   **장점**: 보안성이 높음, 클라이언트 구현 간단.
*   **구현**: `SecurityConfig`의 `oauth2Login()` 설정 및 `OAuth2SuccessHandler`.

#### B. Frontend-Driven (모바일/SPA 친화적)
*   **흐름**: Frontend에서 Provider SDK로 토큰 획득 → `POST /api/v1/auth/oauth/kakao` (Body: accessToken) → Backend 검증 및 JWT 발급.
*   **장점**: 모바일 네이티브 SDK 활용 용이, 유연한 UI 제어.
*   **구현**: `AuthController`의 `kakaoLogin`, `googleLogin` 엔드포인트.

---

## 8. 실시간 통신 (WebSocket)

### 8.1 토픽 설계

*   **Endpoint**: `/ws-stomp` *(문서 정합성 기준, 프론트 연동 시 동일 엔드포인트 사용)*
*   `/topic/stock.indices`: 시장 지수 업데이트 (10초 주기)
*   `/topic/stock.ticker.{ticker}`: 개별 종목 체결가/호가 (실시간)
*   `/user/queue/trade`: 사용자 개인 주문 체결 알림

---

## 9. 외부 API 연동

### 9.1 Finnhub (주식 데이터)
*   WebSocket으로 실시간 미국 주식 데이터 수신
*   Redis에 최신가 캐싱 (`stock:price:{ticker}`)

### 9.2 FastAPI (AI 서버)
*   **Endpoint**: `POST /chat/ask` (SSE Streaming)
    *   사용자의 포트폴리오 및 사주 정보를 컨텍스트로 포함하여 LLM에 질의
*   SSE(Server-Sent Events)를 통해 스트리밍 응답 제공

---

## 10. 비즈니스 로직 (핵심)

### 10.1 가챠(Gacha) 로직
1.  사용자 지갑에서 코인 차감 (`WalletService`)
2.  확률 테이블(`Items`의 `probability`)에 따라 아이템 추첨 (`RandomUtils`)
3.  인벤토리에 아이템 추가 (중복 시 대체 보상 또는 레벨업 고려)
4.  결과 DTO 반환

### 10.2 사주 분석 로직 (`SajuCalculator`)
1.  생년월일 기반 천간(Heavenly Stem) 지지(Earthly Branch) 계산
2.  오행(Wood, Fire, Earth, Metal, Water) 도출
3.  오행별 투자 성향 매핑 (DB 또는 Enum 관리)

---

## 11. 구현 현황 (Status)

**Current Date:** 2026-01-18

| 모듈 | 진행률 | 상태 | 비고 |
|------|--------|------|------|
| **Auth** | 100% | ✅ Complete | Hybrid 인증 인터페이스(Backend/Frontend Driven) 확정. 프론트 `/oauth/callback` 및 토큰 저장/갱신 연동은 Phase 1에서 진행. |
| **User** | 80% | ⚠️ Update Req | 기본 엔티티 존재하나 `is_public` 등 신규 필드 누락됨. |
| **Market** | 0% | ⬜ Pending | Controller/Service 미구현. |
| **Trade** | 10% | 🚧 In Progress | 엔티티(`TradeLog`) 존재, 로직 미구현. |
| **Game** | 20% | ⚠️ Update Req | `Item` 엔티티 존재하나 Category Enum(`NAMEPLATE` 등) 업데이트 필요. |
| **AI** | 0% | ⬜ Pending | FastAPI 연동 미구현. |

---

**문서 버전:** 2.7 (Spec-Driven Alignment)
**최종 수정일:** 2026-01-18
