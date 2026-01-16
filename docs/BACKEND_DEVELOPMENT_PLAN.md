# ⚙️ Stock-Persona: 백엔드 개발 계획서

**Ver 1.0 - Backend Development Blueprint**

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
11. [AI 서버 설계](#11-ai-서버-설계)
12. [캐싱 전략](#12-캐싱-전략)
13. [테스트 전략](#13-테스트-전략)
14. [배포 및 인프라](#14-배포-및-인프라)
15. [개발 로드맵](#15-개발-로드맵)

---

## 1. 시스템 개요

### 1.1 백엔드 역할

Stock-Persona 백엔드는 다음 핵심 기능을 담당합니다:

1. **사용자 관리**: OAuth2 인증, JWT 토큰, 프로필 관리
2. **모의투자 엔진**: 매수/매도 트랜잭션, 포트폴리오 관리
3. **실시간 데이터**: Finnhub WebSocket → Redis → 클라이언트 중계
4. **게이미피케이션**: 가챠, 랭킹, 인벤토리
5. **AI 상담**: 사주 기반 투자 조언 (FastAPI 연동)

### 1.2 서버 구성

```
┌─────────────────────────────────────────────────────────────────┐
│                       BACKEND SERVERS                            │
│                                                                  │
│  ┌──────────────────────────────┐   ┌────────────────────────┐  │
│  │     Spring Boot 3.2          │   │    FastAPI (Python)    │  │
│  │     (Core Server)            │   │    (AI Server)         │  │
│  │                              │   │                        │  │
│  │  • REST API                  │   │  • LLM Inference       │  │
│  │  • WebSocket (STOMP)         │   │  • Stable Diffusion    │  │
│  │  • OAuth2 + JWT              │   │  • SSE Streaming       │  │
│  │  • Transaction Management    │   │                        │  │
│  │  • Finnhub Integration       │   │                        │  │
│  └──────────────────────────────┘   └────────────────────────┘  │
│               │                               │                  │
│               ▼                               ▼                  │
│  ┌──────────────────┐  ┌──────────────────┐  ┌───────────────┐  │
│  │  PostgreSQL 16   │  │    Redis 7       │  │  Local GPU    │  │
│  │  (Main DB)       │  │  (Cache/Pub-Sub) │  │  (AI 처리)    │  │
│  └──────────────────┘  └──────────────────┘  └───────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 아키텍처 설계

### 2.1 레이어드 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                     PRESENTATION LAYER                           │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  Controllers (REST API)                                     │ │
│  │  ├── AuthController                                         │ │
│  │  ├── UserController                                         │ │
│  │  ├── TradeController                                        │ │
│  │  ├── StockController                                        │ │
│  │  ├── GameController                                         │ │
│  │  ├── ChatController                                         │ │
│  │  └── NotificationController                                 │ │
│  └─────────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  WebSocket Handlers (STOMP)                                 │ │
│  │  ├── StockWebSocketHandler                                  │ │
│  │  ├── TradeWebSocketHandler                                  │ │
│  │  └── NotificationWebSocketHandler                           │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      SERVICE LAYER                               │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  Services (Business Logic)                                  │ │
│  │  ├── AuthService                                            │ │
│  │  ├── UserService                                            │ │
│  │  ├── TradeService                                           │ │
│  │  ├── PortfolioService                                       │ │
│  │  ├── StockService                                           │ │
│  │  ├── GachaService                                           │ │
│  │  ├── RankingService                                         │ │
│  │  ├── ChatService                                            │ │
│  │  ├── NotificationService                                    │ │
│  │  └── SajuService                                            │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   INFRASTRUCTURE LAYER                           │
│  ┌───────────────────┐ ┌──────────────────┐ ┌────────────────┐  │
│  │  Repositories     │ │  External APIs   │ │  Message Queue │  │
│  │  (JPA)            │ │  (Finnhub, AI)   │ │  (Redis)       │  │
│  └───────────────────┘ └──────────────────┘ └────────────────┘  │
│  ┌───────────────────┐ ┌──────────────────┐                     │
│  │  Cache Manager    │ │  WebSocket Broker│                     │
│  │  (Redis)          │ │  (STOMP)         │                     │
│  └───────────────────┘ └──────────────────┘                     │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 도메인 모델

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│    User      │────►│   Wallet     │     │  Portfolio   │
│              │     │              │     │  (Holdings)  │
│ - userId     │     │ - cashBalance│     │ - ticker     │
│ - email      │     │ - realizedPnL│     │ - quantity   │
│ - nickname   │     │ - gameCoin   │     │ - avgPrice   │
│ - birthDate  │     │ - totalAssets│     │              │
│ - sajuElement│     └──────────────┘     └──────────────┘
│ - zodiacSign │            │                    │
│ - avatarUrl  │            ▼                    ▼
└──────────────┘     ┌──────────────┐     ┌──────────────┐
       │             │  TradeLogs   │     │  Watchlist   │
       │             │              │     │              │
       │             │ - tradeType  │     │ - ticker     │
       │             │ - price      │     │ - addedAt    │
       │             │ - quantity   │     └──────────────┘
       │             │ - realizedPnL│
       │             └──────────────┘
       │
       ├─────────────────────────────────────────────┐
       │                                             │
       ▼                                             ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Inventory   │────►│    Item      │     │ ChatHistory  │
│              │     │              │     │              │
│ - isEquipped │     │ - name       │     │ - sessionId  │
│ - acquiredAt │     │ - category   │     │ - messages   │
│              │     │ - rarity     │     │ - sentiment  │
└──────────────┘     │ - probability│     └──────────────┘
                     └──────────────┘
                            │
                            ▼
                     ┌──────────────┐
                     │ Notification │
                     │              │
                     │ - type       │
                     │ - title      │
                     │ - isRead     │
                     └──────────────┘
```

---

## 3. 기술 스택

### 3.1 Core Server (Spring Boot)

| 기술 | 버전 | 용도 |
|------|------|------|
| Java | 21 LTS | 언어 |
| Spring Boot | 3.2.x | 프레임워크 |
| Spring Security | 6.x | 인증/인가 |
| Spring Data JPA | 3.x | ORM |
| Spring WebSocket | 6.x | 실시간 통신 |
| Spring Validation | 3.x | 입력 검증 |
| Lombok | Latest | 보일러플레이트 제거 |
| MapStruct | Latest | DTO 매핑 |
| jjwt | 0.12.x | JWT 처리 |
| Lettuce | 6.x | Redis 클라이언트 |

### 3.2 AI Server (FastAPI)

| 기술 | 버전 | 용도 |
|------|------|------|
| Python | 3.11+ | 언어 |
| FastAPI | 0.100+ | API 프레임워크 |
| Uvicorn | Latest | ASGI 서버 |
| PyTorch | 2.x | AI 프레임워크 |
| Transformers | Latest | LLM |
| Diffusers | Latest | Stable Diffusion |
| SSE-Starlette | Latest | SSE 스트리밍 |

### 3.3 Database & Cache

| 기술 | 버전 | 용도 |
|------|------|------|
| PostgreSQL | 16 | 메인 DB |
| Redis | 7.x | 캐시/Pub-Sub |

### 3.4 외부 API

| API | 용도 |
|-----|------|
| Finnhub | 미국 주식 실시간 데이터 |
| Google OAuth2 | 소셜 로그인 |

---

## 4. 프로젝트 구조

### 4.1 Spring Boot 프로젝트 구조

```
📁 stock-persona-backend/
├── 📁 src/main/java/com/stockpersona/
│   │
│   ├── 📁 config/                        # 설정 클래스
│   │   ├── SecurityConfig.java           # Spring Security 설정
│   │   ├── WebSocketConfig.java          # WebSocket/STOMP 설정
│   │   ├── RedisConfig.java              # Redis 설정
│   │   ├── JpaConfig.java                # JPA 설정
│   │   ├── CorsConfig.java               # CORS 설정
│   │   └── SwaggerConfig.java            # API 문서 설정
│   │
│   ├── 📁 domain/                        # 도메인 엔티티
│   │   ├── 📁 user/
│   │   │   ├── User.java                 # 사용자 엔티티
│   │   │   └── UserRepository.java       # 사용자 레포지토리
│   │   ├── 📁 wallet/
│   │   │   ├── Wallet.java               # 지갑 엔티티
│   │   │   └── WalletRepository.java
│   │   ├── 📁 portfolio/
│   │   │   ├── Portfolio.java            # 포트폴리오 엔티티
│   │   │   └── PortfolioRepository.java
│   │   ├── 📁 trade/
│   │   │   ├── TradeLog.java             # 거래 기록 엔티티
│   │   │   └── TradeLogRepository.java
│   │   ├── 📁 item/
│   │   │   ├── Item.java                 # 아이템 엔티티
│   │   │   ├── ItemRepository.java
│   │   │   ├── Inventory.java            # 인벤토리 엔티티
│   │   │   └── InventoryRepository.java
│   │   ├── 📁 chat/
│   │   │   ├── ChatHistory.java          # 채팅 기록 엔티티
│   │   │   └── ChatHistoryRepository.java
│   │   ├── 📁 watchlist/
│   │   │   ├── Watchlist.java            # 관심종목 엔티티
│   │   │   └── WatchlistRepository.java
│   │   └── 📁 notification/
│   │       ├── Notification.java         # 알림 엔티티
│   │       └── NotificationRepository.java
│   │
│   ├── 📁 dto/                           # Data Transfer Objects
│   │   ├── 📁 request/
│   │   │   ├── LoginRequest.java
│   │   │   ├── OnboardingRequest.java
│   │   │   ├── TradeOrderRequest.java
│   │   │   ├── GachaRequest.java
│   │   │   └── ChatRequest.java
│   │   ├── 📁 response/
│   │   │   ├── AuthResponse.java
│   │   │   ├── UserResponse.java
│   │   │   ├── WalletResponse.java
│   │   │   ├── PortfolioResponse.java
│   │   │   ├── TradeResponse.java
│   │   │   ├── StockQuoteResponse.java
│   │   │   ├── CandleResponse.java
│   │   │   ├── GachaResultResponse.java
│   │   │   ├── RankingResponse.java
│   │   │   ├── DividendResponse.java
│   │   │   └── NotificationResponse.java
│   │   └── 📁 websocket/
│   │       ├── StockPriceMessage.java
│   │       ├── TradeNotification.java
│   │       └── RankingUpdate.java
│   │
│   ├── 📁 service/                       # 비즈니스 로직
│   │   ├── AuthService.java              # 인증 서비스
│   │   ├── UserService.java              # 사용자 서비스
│   │   ├── WalletService.java            # 지갑 서비스
│   │   ├── TradeService.java             # 거래 서비스
│   │   ├── PortfolioService.java         # 포트폴리오 서비스
│   │   ├── StockService.java             # 주식 서비스
│   │   ├── GachaService.java             # 가챠 서비스
│   │   ├── InventoryService.java         # 인벤토리 서비스
│   │   ├── RankingService.java           # 랭킹 서비스
│   │   ├── ChatService.java              # AI 상담 서비스
│   │   ├── NotificationService.java      # 알림 서비스
│   │   ├── SajuService.java              # 사주 계산 서비스
│   │   └── DividendService.java          # 배당금 계산 서비스
│   │
│   ├── 📁 controller/                    # REST 컨트롤러
│   │   ├── AuthController.java           # /api/v1/auth/*
│   │   ├── UserController.java           # /api/v1/user/*
│   │   ├── TradeController.java          # /api/v1/trade/*
│   │   ├── StockController.java          # /api/v1/stock/*
│   │   ├── WatchlistController.java      # /api/v1/watchlist/*
│   │   ├── GameController.java           # /api/v1/game/*
│   │   ├── ChatController.java           # /api/v1/chat/*
│   │   ├── CalcController.java           # /api/v1/calc/*
│   │   └── NotificationController.java   # /api/v1/notifications/*
│   │
│   ├── 📁 websocket/                     # WebSocket 핸들러
│   │   ├── StockPriceHandler.java        # 주가 브로드캐스트
│   │   ├── TradeHandler.java             # 체결 알림
│   │   └── NotificationHandler.java      # 개인 알림
│   │
│   ├── 📁 external/                      # 외부 API 연동
│   │   ├── 📁 finnhub/
│   │   │   ├── FinnhubClient.java        # REST 클라이언트
│   │   │   ├── FinnhubWebSocketClient.java # WebSocket 클라이언트
│   │   │   └── FinnhubProperties.java    # 설정
│   │   └── 📁 ai/
│   │       ├── AIClient.java             # FastAPI 연동
│   │       └── AIProperties.java         # 설정
│   │
│   ├── 📁 security/                      # 보안 관련
│   │   ├── JwtTokenProvider.java         # JWT 생성/검증
│   │   ├── JwtAuthenticationFilter.java  # JWT 필터
│   │   ├── OAuth2SuccessHandler.java     # OAuth 성공 핸들러
│   │   ├── CustomUserDetails.java        # 사용자 상세
│   │   └── CustomUserDetailsService.java # 사용자 로드
│   │
│   ├── 📁 exception/                     # 예외 처리
│   │   ├── GlobalExceptionHandler.java   # 전역 예외 핸들러
│   │   ├── BusinessException.java        # 비즈니스 예외
│   │   ├── AuthException.java            # 인증 예외
│   │   ├── TradeException.java           # 거래 예외
│   │   └── ErrorCode.java                # 에러 코드 enum
│   │
│   ├── 📁 util/                          # 유틸리티
│   │   ├── SajuCalculator.java           # 사주 계산
│   │   └── DateUtils.java                # 날짜 유틸
│   │
│   └── StockPersonaApplication.java      # 메인 클래스
│
├── 📁 src/main/resources/
│   ├── application.yml                   # 메인 설정
│   ├── application-dev.yml               # 개발 설정
│   ├── application-prod.yml              # 프로덕션 설정
│   └── 📁 db/migration/                  # Flyway 마이그레이션
│       ├── V1__init_schema.sql
│       ├── V2__add_items.sql
│       └── V3__add_notifications.sql
│
├── 📁 src/test/java/com/stockpersona/
│   ├── 📁 service/
│   │   ├── TradeServiceTest.java
│   │   ├── GachaServiceTest.java
│   │   └── SajuServiceTest.java
│   ├── 📁 controller/
│   │   ├── AuthControllerTest.java
│   │   └── TradeControllerTest.java
│   └── 📁 integration/
│       ├── TradeIntegrationTest.java
│       └── WebSocketIntegrationTest.java
│
├── build.gradle                          # Gradle 빌드
├── settings.gradle
├── Dockerfile                            # Docker 이미지
└── docker-compose.yml                    # 개발 환경
```

### 4.2 FastAPI 프로젝트 구조

```
📁 stock-persona-ai/
├── 📁 app/
│   ├── main.py                           # FastAPI 앱
│   ├── 📁 api/
│   │   ├── __init__.py
│   │   ├── chat.py                       # /chat/* 라우터
│   │   └── avatar.py                     # /avatar/* 라우터
│   │
│   ├── 📁 services/
│   │   ├── __init__.py
│   │   ├── llm_service.py                # LLM 추론
│   │   ├── avatar_service.py             # 아바타 생성
│   │   └── prompt_builder.py             # 프롬프트 생성
│   │
│   ├── 📁 models/
│   │   ├── __init__.py
│   │   ├── chat_request.py               # 요청 모델
│   │   └── chat_response.py              # 응답 모델
│   │
│   └── 📁 core/
│       ├── __init__.py
│       ├── config.py                     # 설정
│       └── gpu_manager.py                # GPU 관리
│
├── requirements.txt                      # 의존성
├── Dockerfile                            # Docker 이미지
└── docker-compose.yml                    # 개발 환경
```

---

## 5. 데이터베이스 설계

### 5.1 ERD

```
┌──────────────────────────────────────────────────────────────────────┐
│                              users                                    │
├──────────────────────────────────────────────────────────────────────┤
│ PK │ user_id        │ BIGSERIAL                                      │
│    │ email          │ VARCHAR(255) NOT NULL UNIQUE                   │
│    │ nickname       │ VARCHAR(50) NOT NULL                           │
│    │ provider       │ VARCHAR(20) DEFAULT 'GOOGLE'                   │
│    │ birth_date     │ DATE NOT NULL                                  │
│    │ saju_element   │ VARCHAR(10)                                    │
│    │ zodiac_sign    │ VARCHAR(20)                                    │
│    │ avatar_url     │ TEXT                                           │
│    │ created_at     │ TIMESTAMP DEFAULT CURRENT_TIMESTAMP            │
│    │ updated_at     │ TIMESTAMP DEFAULT CURRENT_TIMESTAMP            │
└────┴────────────────┴────────────────────────────────────────────────┘
           │
           │ 1:1
           ▼
┌──────────────────────────────────────────────────────────────────────┐
│                              wallet                                   │
├──────────────────────────────────────────────────────────────────────┤
│ PK │ wallet_id      │ BIGSERIAL                                      │
│ FK │ user_id        │ BIGINT NOT NULL UNIQUE → users(user_id)        │
│    │ cash_balance   │ NUMERIC(19,4) DEFAULT 10000.0000               │
│    │ realized_profit│ NUMERIC(19,4) DEFAULT 0.0000                   │
│    │ total_assets   │ NUMERIC(19,4) DEFAULT 10000.0000               │
│    │ game_coin      │ INT DEFAULT 0                                  │
│    │ updated_at     │ TIMESTAMP DEFAULT CURRENT_TIMESTAMP            │
└────┴────────────────┴────────────────────────────────────────────────┘
           │
           │ 1:N
           ▼
┌──────────────────────────────────────────────────────────────────────┐
│                            portfolio                                  │
├──────────────────────────────────────────────────────────────────────┤
│ PK │ pf_id          │ BIGSERIAL                                      │
│ FK │ user_id        │ BIGINT NOT NULL → users(user_id)               │
│    │ ticker         │ VARCHAR(10) NOT NULL                           │
│    │ quantity       │ INT NOT NULL CHECK (quantity >= 0)             │
│    │ avg_price      │ NUMERIC(19,4) NOT NULL                         │
│ UQ │ (user_id, ticker)                                               │
└────┴────────────────┴────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                           trade_logs                                  │
├──────────────────────────────────────────────────────────────────────┤
│ PK │ log_id         │ BIGSERIAL                                      │
│ FK │ user_id        │ BIGINT NOT NULL → users(user_id)               │
│    │ ticker         │ VARCHAR(10) NOT NULL                           │
│    │ trade_type     │ VARCHAR(4) NOT NULL ('BUY' or 'SELL')          │
│    │ price          │ NUMERIC(19,4) NOT NULL                         │
│    │ quantity       │ INT NOT NULL                                   │
│    │ total_amount   │ NUMERIC(19,4) NOT NULL                         │
│    │ fee            │ NUMERIC(19,4) DEFAULT 0                        │
│    │ realized_pnl   │ NUMERIC(19,4)                                  │
│    │ trade_date     │ TIMESTAMP DEFAULT CURRENT_TIMESTAMP            │
└────┴────────────────┴────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                             items                                     │
├──────────────────────────────────────────────────────────────────────┤
│ PK │ item_id        │ BIGSERIAL                                      │
│    │ name           │ VARCHAR(100) NOT NULL                          │
│    │ description    │ TEXT                                           │
│    │ category       │ VARCHAR(20) NOT NULL                           │
│    │ rarity         │ VARCHAR(20) NOT NULL                           │
│    │ probability    │ FLOAT NOT NULL                                 │
│    │ image_url      │ TEXT                                           │
│    │ created_at     │ TIMESTAMP DEFAULT CURRENT_TIMESTAMP            │
└────┴────────────────┴────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                           inventory                                   │
├──────────────────────────────────────────────────────────────────────┤
│ PK │ inv_id         │ BIGSERIAL                                      │
│ FK │ user_id        │ BIGINT NOT NULL → users(user_id)               │
│ FK │ item_id        │ BIGINT NOT NULL → items(item_id)               │
│    │ is_equipped    │ BOOLEAN DEFAULT FALSE                          │
│    │ acquired_at    │ TIMESTAMP DEFAULT CURRENT_TIMESTAMP            │
│ UQ │ (user_id, item_id)                                              │
└────┴────────────────┴────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                          chat_history                                 │
├──────────────────────────────────────────────────────────────────────┤
│ PK │ chat_id        │ BIGSERIAL                                      │
│ FK │ user_id        │ BIGINT NOT NULL → users(user_id)               │
│    │ session_id     │ UUID NOT NULL                                  │
│    │ messages       │ JSONB NOT NULL                                 │
│    │ sentiment_score│ FLOAT                                          │
│    │ created_at     │ TIMESTAMP DEFAULT CURRENT_TIMESTAMP            │
└────┴────────────────┴────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                           watchlist                                   │
├──────────────────────────────────────────────────────────────────────┤
│ PK │ watchlist_id   │ BIGSERIAL                                      │
│ FK │ user_id        │ BIGINT NOT NULL → users(user_id)               │
│    │ ticker         │ VARCHAR(10) NOT NULL                           │
│    │ added_at       │ TIMESTAMP DEFAULT CURRENT_TIMESTAMP            │
│ UQ │ (user_id, ticker)                                               │
└────┴────────────────┴────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                         notifications                                 │
├──────────────────────────────────────────────────────────────────────┤
│ PK │ notif_id       │ BIGSERIAL                                      │
│ FK │ user_id        │ BIGINT NOT NULL → users(user_id)               │
│    │ type           │ VARCHAR(30) NOT NULL                           │
│    │ title          │ VARCHAR(200) NOT NULL                          │
│    │ message        │ TEXT                                           │
│    │ is_read        │ BOOLEAN DEFAULT FALSE                          │
│    │ created_at     │ TIMESTAMP DEFAULT CURRENT_TIMESTAMP            │
└────┴────────────────┴────────────────────────────────────────────────┘
```

### 5.2 인덱스 전략

| 테이블 | 인덱스 | 용도 |
|--------|--------|------|
| users | `idx_users_email` | 로그인 조회 |
| wallet | `idx_wallet_user` | 사용자별 지갑 |
| portfolio | `idx_portfolio_user` | 포트폴리오 조회 |
| portfolio | `idx_portfolio_ticker` | 종목별 조회 |
| trade_logs | `idx_trade_user` | 거래 내역 |
| trade_logs | `idx_trade_date` | 날짜별 조회 |
| chat_history | `idx_chat_gin` | JSONB 검색 |
| notifications | `idx_notif_unread` | 안읽은 알림 |

---

## 6. API 상세 설계

### 6.1 API 컨트롤러 설계

#### AuthController (`/api/v1/auth`)

| 메서드 | 경로 | 설명 | 요청 DTO | 응답 DTO |
|--------|------|------|---------|---------|
| POST | `/login` | OAuth 로그인 | `LoginRequest` | `AuthResponse` |
| POST | `/refresh` | 토큰 갱신 | `RefreshRequest` | `AuthResponse` |
| POST | `/logout` | 로그아웃 | - | - |
| GET | `/me` | 현재 사용자 | - | `UserResponse` |

#### UserController (`/api/v1/user`)

| 메서드 | 경로 | 설명 | 요청 DTO | 응답 DTO |
|--------|------|------|---------|---------|
| GET | `/me` | 내 정보 | - | `UserDetailResponse` |
| PUT | `/me` | 정보 수정 | `UpdateUserRequest` | `UserResponse` |
| POST | `/onboarding` | 온보딩 | `OnboardingRequest` | `OnboardingResponse` |
| GET | `/wallet` | 지갑 조회 | - | `WalletResponse` |

#### TradeController (`/api/v1/trade`)

| 메서드 | 경로 | 설명 | 요청 DTO | 응답 DTO |
|--------|------|------|---------|---------|
| POST | `/order` | 주문 | `TradeOrderRequest` | `TradeResponse` |
| GET | `/portfolio` | 포트폴리오 | - | `PortfolioListResponse` |
| GET | `/portfolio/{ticker}` | 특정 종목 | - | `PortfolioItemResponse` |
| GET | `/history` | 거래 내역 | Query Params | `TradeHistoryResponse` |

#### StockController (`/api/v1/stock`)

| 메서드 | 경로 | 설명 | 요청 DTO | 응답 DTO |
|--------|------|------|---------|---------|
| GET | `/quote/{ticker}` | 현재가 | - | `StockQuoteResponse` |
| GET | `/candles/{ticker}` | 캔들 데이터 | Query Params | `CandleResponse` |
| GET | `/search` | 종목 검색 | Query Params | `SearchResponse` |
| GET | `/profile/{ticker}` | 종목 정보 | - | `StockProfileResponse` |

#### WatchlistController (`/api/v1/watchlist`)

| 메서드 | 경로 | 설명 | 요청 DTO | 응답 DTO |
|--------|------|------|---------|---------|
| GET | `/` | 관심종목 조회 | - | `WatchlistResponse` |
| POST | `/` | 추가 | `AddWatchlistRequest` | `WatchlistItemResponse` |
| DELETE | `/{ticker}` | 삭제 | - | - |

#### GameController (`/api/v1/game`)

| 메서드 | 경로 | 설명 | 요청 DTO | 응답 DTO |
|--------|------|------|---------|---------|
| POST | `/gacha` | 가챠 뽑기 | `GachaRequest` | `GachaResultResponse` |
| GET | `/items` | 아이템 목록 | - | `ItemListResponse` |
| GET | `/inventory` | 인벤토리 | - | `InventoryResponse` |
| PUT | `/equip/{itemId}` | 장착/해제 | - | `EquipResponse` |
| GET | `/ranking` | 랭킹 | Query Params | `RankingResponse` |
| POST | `/convert-profit` | 수익→코인 | `ConvertRequest` | `ConvertResponse` |

#### CalcController (`/api/v1/calc`)

| 메서드 | 경로 | 설명 | 요청 DTO | 응답 DTO |
|--------|------|------|---------|---------|
| GET | `/dividend` | 배당금 계산 | - | `DividendResponse` |
| GET | `/tax` | 세금 계산 | Query Params | `TaxResponse` |

#### ChatController (`/api/v1/chat`)

| 메서드 | 경로 | 설명 | 요청 DTO | 응답 DTO |
|--------|------|------|---------|---------|
| POST | `/ask` | AI 상담 (SSE) | `ChatRequest` | SSE Stream |
| GET | `/history` | 대화 내역 | Query Params | `ChatHistoryResponse` |
| DELETE | `/session/{sessionId}` | 세션 삭제 | - | - |

#### NotificationController (`/api/v1/notifications`)

| 메서드 | 경로 | 설명 | 요청 DTO | 응답 DTO |
|--------|------|------|---------|---------|
| GET | `/` | 알림 목록 | Query Params | `NotificationListResponse` |
| PUT | `/{id}/read` | 읽음 처리 | - | - |
| PUT | `/read-all` | 전체 읽음 | - | - |

### 6.2 DTO 설계

#### Request DTOs

```
📁 dto/request/
├── LoginRequest
│   └── provider: String, idToken: String
│
├── OnboardingRequest
│   └── nickname: String, birthDate: LocalDate
│
├── TradeOrderRequest
│   └── ticker: String, type: TradeType, quantity: Integer, orderType: OrderType
│
├── GachaRequest
│   └── count: Integer
│
├── ChatRequest
│   └── message: String, sessionId: UUID, context: ChatContext
│
└── AddWatchlistRequest
    └── ticker: String
```

#### Response DTOs

```
📁 dto/response/
├── AuthResponse
│   └── accessToken, refreshToken, expiresIn, user, isNewUser
│
├── UserResponse
│   └── userId, email, nickname, sajuElement, zodiacSign, avatarUrl
│
├── OnboardingResponse
│   └── userId, nickname, birthDate, sajuElement, sajuElementKor, 
│       zodiacSign, zodiacSignKor, luckyColor, luckyNumber[], wallet
│
├── WalletResponse
│   └── cashBalance, realizedProfit, totalAssets, gameCoin
│
├── PortfolioResponse
│   └── holdings[], totalValue, totalProfit, totalProfitPercent
│
├── TradeResponse
│   └── orderId, ticker, type, quantity, executedPrice, 
│       totalAmount, fee, executedAt, portfolio, wallet
│
├── StockQuoteResponse
│   └── ticker, price, change, changePercent, volume, timestamp
│
├── CandleResponse
│   └── ticker, candles[{time, open, high, low, close, volume}]
│
├── GachaResultResponse
│   └── results[{itemId, name, category, rarity, imageUrl, isNew}], wallet
│
├── RankingResponse
│   └── rankings[{rank, userId, nickname, avatarUrl, profitPercent, items[]}], 
│       totalParticipants
│
├── DividendResponse
│   └── portfolio[{ticker, quantity, dividendPerShare, annualDividend, 
│                   dividendYield, exDividendDate}],
│       summary{totalAnnualDividend, withholdingTax, netDividend, avgYield}
│
└── NotificationResponse
    └── notifications[{id, type, title, message, isRead, createdAt}], unreadCount
```

---

## 7. 인증 및 보안

### 7.1 인증 흐름

```
┌─────────┐         ┌─────────┐         ┌─────────┐         ┌─────────┐
│ Client  │         │ Google  │         │ Backend │         │   DB    │
└────┬────┘         └────┬────┘         └────┬────┘         └────┬────┘
     │                   │                   │                   │
     │ Google Login      │                   │                   │
     │──────────────────▶│                   │                   │
     │                   │                   │                   │
     │   ID Token        │                   │                   │
     │◀──────────────────│                   │                   │
     │                   │                   │                   │
     │      POST /auth/login (ID Token)      │                   │
     │──────────────────────────────────────▶│                   │
     │                   │                   │                   │
     │                   │                   │ Verify Token      │
     │                   │◀──────────────────│                   │
     │                   │                   │                   │
     │                   │   Token Valid     │                   │
     │                   │──────────────────▶│                   │
     │                   │                   │                   │
     │                   │                   │ Find/Create User  │
     │                   │                   │──────────────────▶│
     │                   │                   │                   │
     │                   │                   │     User Data     │
     │                   │                   │◀──────────────────│
     │                   │                   │                   │
     │                   │                   │ Generate JWT      │
     │                   │                   │ (Access+Refresh)  │
     │                   │                   │                   │
     │     AuthResponse (Access, Refresh)    │                   │
     │◀──────────────────────────────────────│                   │
     │                   │                   │                   │
```

### 7.2 JWT 구조

```java
// Access Token Payload
{
  "sub": "1",                    // userId
  "email": "user@gmail.com",
  "nickname": "투자도사",
  "role": "USER",
  "iat": 1705392000,             // issued at
  "exp": 1705395600              // expires (1h)
}

// Refresh Token Payload
{
  "sub": "1",
  "type": "REFRESH",
  "iat": 1705392000,
  "exp": 1705996800              // expires (7d)
}
```

### 7.3 Security Config

```
SecurityFilterChain 설정:
├── CSRF 비활성화 (REST API)
├── 세션 Stateless
├── CORS 설정
├── 공개 엔드포인트: /auth/login, /auth/refresh
├── 보호 엔드포인트: 나머지 전체
└── JWT 필터 추가
```

### 7.4 보안 체크리스트

| 항목 | 구현 방법 |
|------|----------|
| SQL Injection | JPA Prepared Statement |
| XSS | CSP 헤더, 입력 검증 |
| CSRF | Stateless (비활성화) |
| Rate Limiting | Bucket4j 또는 Nginx |
| HTTPS | TLS 1.3 강제 |
| Password | OAuth2만 사용 (저장 없음) |

---

## 8. 실시간 통신

### 8.1 WebSocket 설정

```
WebSocketConfig:
├── STOMP 엔드포인트: /ws
├── SockJS 폴백 활성화
├── Message Broker:
│   ├── /topic/* (브로드캐스트)
│   └── /user/queue/* (개인)
└── Application Destination Prefix: /app
```

### 8.2 채널 설계

| 채널 | 타입 | 설명 | 메시지 형식 |
|------|------|------|------------|
| `/topic/stock.{ticker}` | Broadcast | 종목별 실시간 시세 | `StockPriceMessage` |
| `/user/queue/trade` | User-specific | 체결 알림 | `TradeNotification` |
| `/user/queue/notification` | User-specific | 개인 알림 | `NotificationMessage` |
| `/topic/ranking` | Broadcast | 랭킹 변동 | `RankingUpdate` |

### 8.3 Finnhub 연동

```
Finnhub WebSocket Client:
├── 연결: wss://ws.finnhub.io
├── 구독: {"type":"subscribe","symbol":"AAPL"}
├── 수신: {"data":[{"s":"AAPL","p":198.45,"v":100}],"type":"trade"}
├── 처리:
│   ├── Redis 캐시 업데이트
│   └── STOMP 브로커로 전달
└── 재연결: 자동 재연결 로직
```

### 8.4 메시지 흐름

```
[Finnhub] ─── WebSocket ───▶ [FinnhubWebSocketClient]
                                      │
                                      ├──▶ [Redis Cache] stock:{ticker}:price
                                      │
                                      └──▶ [Redis Pub/Sub] channel: stock.update
                                                  │
                                                  ▼
                                      [StockPriceHandler]
                                                  │
                                                  ▼
                                      [STOMP Broker] /topic/stock.{ticker}
                                                  │
                                                  ▼
                                      [Subscribed Clients]
```

---

## 9. 외부 API 연동

### 9.1 Finnhub API

#### REST API 사용

| 엔드포인트 | 용도 |
|-----------|------|
| `GET /quote` | 현재가 조회 |
| `GET /stock/candle` | 캔들 데이터 |
| `GET /search` | 종목 검색 |
| `GET /stock/profile2` | 회사 정보 |
| `GET /stock/metric` | 재무 지표 |

#### WebSocket 사용

```
구독: {"type":"subscribe","symbol":"AAPL"}
수신: {"data":[{"s":"AAPL","p":198.45,"t":1705392000000,"v":100}],"type":"trade"}
해제: {"type":"unsubscribe","symbol":"AAPL"}
```

### 9.2 AI Server (FastAPI) 연동

#### 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/chat/ask` | AI 상담 (SSE) |
| POST | `/avatar/generate` | 아바타 생성 |

#### 요청 형식

```json
// POST /chat/ask
{
  "message": "테슬라 지금 사도 될까?",
  "userContext": {
    "sajuElement": "FIRE",
    "zodiacSign": "DRAGON",
    "portfolio": [
      {"ticker": "AAPL", "profitPercent": 5.2}
    ]
  },
  "stockContext": {
    "ticker": "TSLA",
    "price": 282.08,
    "change": -2.92,
    "rsi": 45.3,
    "per": 78.5
  }
}
```

---

## 10. 비즈니스 로직

### 10.1 매수 트랜잭션

```
TradeService.buy():
1. @Transactional 시작
2. Redis에서 현재가 조회
3. wallet SELECT FOR UPDATE (Row Lock)
4. 잔고 확인: cashBalance >= price * quantity
5. wallet.cashBalance 차감
6. portfolio UPSERT:
   - 기존 보유 시: 수량 합산, 평단가 재계산
   - 신규 보유 시: INSERT
7. trade_logs INSERT
8. wallet.totalAssets 재계산
9. COMMIT
10. WebSocket으로 체결 알림 전송
11. 알림 저장
```

### 10.2 매도 트랜잭션

```
TradeService.sell():
1. @Transactional 시작
2. Redis에서 현재가 조회
3. portfolio SELECT FOR UPDATE
4. 보유 수량 확인: quantity >= 요청 수량
5. 실현 손익 계산: (현재가 - 평단가) * 수량
6. portfolio 수량 차감 (0이면 DELETE)
7. wallet.cashBalance 증가
8. wallet.realizedProfit 증가 (양수인 경우)
9. trade_logs INSERT (realized_pnl 포함)
10. wallet.totalAssets 재계산
11. COMMIT
12. WebSocket으로 체결 알림 전송
13. 알림 저장
```

### 10.3 가챠 로직

```
GachaService.spin():
1. wallet 조회 및 코인 확인
2. 확률 기반 아이템 선택:
   - Random 0~1 생성
   - 누적 확률로 등급 결정
   - 해당 등급 내 랜덤 아이템 선택
3. wallet.gameCoin 차감
4. inventory에 아이템 추가 (중복 시 무시)
5. 결과 반환
```

### 10.4 사주 계산

```
SajuService.calculate():
1. 생년월일에서 연도 추출
2. 오행 계산:
   - 끝자리 4,5 → 목(Wood)
   - 끝자리 6,7 → 화(Fire)
   - 끝자리 8,9 → 토(Earth)
   - 끝자리 0,1 → 금(Gold)
   - 끝자리 2,3 → 수(Water)
3. 띠 계산:
   - (연도 - 4) % 12 → 12간지
4. 행운의 색/숫자 매핑
5. 상생/상극 원소 계산
```

### 10.5 배당금 계산

```
DividendService.calculate():
1. 포트폴리오 조회
2. 각 종목에 대해:
   - Finnhub에서 배당 정보 조회
   - 연간 배당금 = 수량 × 주당 배당금
   - 배당 수익률 = 배당금 / 현재 평가액
3. 합계:
   - 총 연간 배당금
   - 원천징수세 (15%)
   - 실수령액
   - 평균 수익률
```

### 10.6 랭킹 계산

```
RankingService.getRanking():
1. 모든 사용자의 wallet 조회
2. 총 수익률 계산:
   - (total_assets - 초기자금) / 초기자금 × 100
3. 수익률 기준 정렬
4. 순위 부여
5. Redis 캐시 저장 (TTL: 1분)
6. 상위 N명 반환
```

---

## 11. AI 서버 설계

### 11.1 FastAPI 구조

```
main.py:
├── CORS 설정
├── 라우터 등록
└── GPU 초기화

api/chat.py:
├── POST /chat/ask (SSE)
│   ├── 사용자 컨텍스트 파싱
│   ├── 프롬프트 생성
│   ├── LLM 추론 (스트리밍)
│   └── SSE 응답
└── POST /chat/analyze
    └── 감정 분석

api/avatar.py:
└── POST /avatar/generate
    ├── 프롬프트 생성
    ├── Stable Diffusion 추론
    └── 이미지 반환
```

### 11.2 System Prompt

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
```

### 11.3 GPU 관리

```
GPU Manager:
├── 모델 로딩 (앱 시작 시)
│   ├── LLM 모델
│   └── Stable Diffusion 모델
├── 추론 큐 관리
│   └── 동시 요청 제한
├── 메모리 관리
│   └── 모델 언로드/로드
└── 헬스 체크
```

---

## 12. 캐싱 전략

### 12.1 Redis 키 구조

```
# 실시간 주가
stock:{ticker}:price          → "198.45" (String, TTL: 5s)
stock:{ticker}:quote          → {price, change, volume} (Hash, TTL: 5s)

# 사용자 세션
session:{userId}              → JWT Refresh Token (String, TTL: 7d)

# 랭킹
ranking:monthly               → [{userId, profit}...] (Sorted Set)
ranking:weekly                → [{userId, profit}...] (Sorted Set)
ranking:cache                 → JSON (String, TTL: 1m)

# 검색 캐시
search:{query}                → [results] (String, TTL: 10m)

# Rate Limiting
ratelimit:{ip}                → count (String, TTL: 1m)
```

### 12.2 캐싱 대상

| 대상 | 캐시 전략 | TTL |
|------|----------|-----|
| 실시간 주가 | Write-through | 5초 |
| 랭킹 | Read-through | 1분 |
| 종목 검색 | Read-through | 10분 |
| 회사 정보 | Read-through | 1시간 |
| 배당 정보 | Read-through | 24시간 |

### 12.3 캐시 무효화

```
패턴:
1. 거래 체결 시 → 랭킹 캐시 무효화
2. 가챠 결과 시 → 인벤토리 캐시 무효화
3. 주가 업데이트 시 → 해당 종목 캐시 갱신
```

---

## 13. 테스트 전략

### 13.1 테스트 레벨

| 레벨 | 도구 | 커버리지 목표 |
|------|------|-------------|
| 단위 테스트 | JUnit 5, Mockito | 80% |
| 통합 테스트 | Spring Test, Testcontainers | 60% |
| E2E 테스트 | RestAssured | 핵심 시나리오 |
| 부하 테스트 | k6, Gatling | 1000 동시 사용자 |

### 13.2 테스트 시나리오

#### 단위 테스트

```
TradeServiceTest:
├── 매수 성공
├── 매수 실패 (잔고 부족)
├── 매도 성공
├── 매도 실패 (수량 부족)
├── 평단가 계산 정확성
└── 실현 손익 계산 정확성

GachaServiceTest:
├── 확률 분포 검증
├── 코인 차감 정확성
├── 중복 아이템 처리
└── 코인 부족 시 예외

SajuServiceTest:
├── 각 연도별 오행 계산
├── 띠 계산
└── 행운 정보 매핑
```

#### 통합 테스트

```
TradeIntegrationTest:
├── 매수 → 포트폴리오 확인 → 매도 → 실현 손익 확인
├── 동시 매수 요청 (동시성)
└── 트랜잭션 롤백 확인

WebSocketIntegrationTest:
├── 연결 → 구독 → 메시지 수신
├── 인증 없이 연결 시도
└── 재연결 로직
```

### 13.3 테스트 데이터

```
@TestConfiguration:
├── 테스트용 H2 데이터베이스
├── 테스트용 Redis (Embedded)
├── Mock Finnhub 서버
└── 테스트 사용자/데이터 Fixture
```

---

## 14. 배포 및 인프라

### 14.1 Docker Compose

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: stockpersona
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  backend:
    build: ./stock-persona-backend
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:postgresql://postgres:5432/stockpersona
      REDIS_HOST: redis
    ports:
      - "8080:8080"
    depends_on:
      - postgres
      - redis

  ai-server:
    build: ./stock-persona-ai
    runtime: nvidia
    environment:
      CUDA_VISIBLE_DEVICES: "0"
    ports:
      - "8000:8000"

  nginx:
    image: nginx:alpine
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    ports:
      - "80:80"
      - "443:443"
    depends_on:
      - backend
      - ai-server
```

### 14.2 환경 변수

```yaml
# application.yml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/stockpersona}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:password}
  
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}

finnhub:
  api-key: ${FINNHUB_API_KEY}
  websocket-url: wss://ws.finnhub.io

jwt:
  secret: ${JWT_SECRET}
  access-expiration: 3600000    # 1시간
  refresh-expiration: 604800000 # 7일

ai:
  server-url: ${AI_SERVER_URL:http://localhost:8000}
```

### 14.3 CI/CD 파이프라인

```
GitHub Actions:
├── PR 생성 시
│   ├── 코드 린트
│   ├── 단위 테스트
│   └── 통합 테스트
│
├── main 브랜치 병합 시
│   ├── Docker 이미지 빌드
│   ├── ECR 푸시
│   └── ECS 배포
│
└── 태그 생성 시 (릴리스)
    ├── 프로덕션 배포
    └── 릴리스 노트 생성
```

### 14.4 모니터링

| 도구 | 용도 |
|------|------|
| Prometheus | 메트릭 수집 |
| Grafana | 대시보드 |
| ELK Stack | 로그 분석 |
| Sentry | 에러 트래킹 |
| PagerDuty | 알림 |

---

## 15. 개발 로드맵

### Phase 1: 프로젝트 셋업 (Week 1)

| 태스크 | 파일/모듈 | 설명 |
|--------|---------|------|
| 프로젝트 생성 | Spring Initializr | 의존성 설정 |
| Docker 환경 | `docker-compose.yml` | Postgres, Redis |
| DB 스키마 | `V1__init.sql` | 테이블 생성 |
| Entity 정의 | `domain/*.java` | JPA 엔티티 |
| Repository 생성 | `*Repository.java` | 데이터 접근 |

### Phase 2: 인증 시스템 (Week 1)

| 태스크 | 파일/모듈 | 설명 |
|--------|---------|------|
| Security 설정 | `SecurityConfig.java` | JWT 설정 |
| JWT Provider | `JwtTokenProvider.java` | 토큰 생성/검증 |
| Auth Controller | `AuthController.java` | 로그인 API |
| OAuth2 연동 | `OAuth2SuccessHandler.java` | Google 연동 |
| 온보딩 API | `UserController.java` | 사주 계산 |

### Phase 3: 거래 시스템 (Week 2)

| 태스크 | 파일/모듈 | 설명 |
|--------|---------|------|
| Trade Service | `TradeService.java` | 매수/매도 로직 |
| Portfolio Service | `PortfolioService.java` | 포트폴리오 관리 |
| Trade Controller | `TradeController.java` | 거래 API |
| 트랜잭션 테스트 | `TradeServiceTest.java` | 단위 테스트 |

### Phase 4: 실시간 시스템 (Week 2)

| 태스크 | 파일/모듈 | 설명 |
|--------|---------|------|
| WebSocket 설정 | `WebSocketConfig.java` | STOMP 설정 |
| Finnhub Client | `FinnhubWebSocketClient.java` | 시세 수신 |
| Stock Handler | `StockPriceHandler.java` | 브로드캐스트 |
| Redis Pub/Sub | `RedisConfig.java` | 캐시/메시지 |

### Phase 5: 주식 API (Week 3)

| 태스크 | 파일/모듈 | 설명 |
|--------|---------|------|
| Stock Service | `StockService.java` | 시세/캔들 조회 |
| Finnhub REST | `FinnhubClient.java` | REST API 연동 |
| Stock Controller | `StockController.java` | 주식 API |
| Watchlist | `WatchlistController.java` | 관심종목 |

### Phase 6: 게이미피케이션 (Week 3-4)

| 태스크 | 파일/모듈 | 설명 |
|--------|---------|------|
| Gacha Service | `GachaService.java` | 가챠 로직 |
| Inventory Service | `InventoryService.java` | 인벤토리 관리 |
| Ranking Service | `RankingService.java` | 랭킹 계산 |
| Game Controller | `GameController.java` | 게임 API |
| 아이템 데이터 | `V2__add_items.sql` | 초기 데이터 |

### Phase 7: AI 서버 (Week 4)

| 태스크 | 파일/모듈 | 설명 |
|--------|---------|------|
| FastAPI 셋업 | `main.py` | 프로젝트 생성 |
| LLM Service | `llm_service.py` | 추론 로직 |
| Chat API | `chat.py` | SSE 스트리밍 |
| Chat Controller | `ChatController.java` | Spring 연동 |

### Phase 8: 부가 기능 (Week 4-5)

| 태스크 | 파일/모듈 | 설명 |
|--------|---------|------|
| Dividend Service | `DividendService.java` | 배당 계산 |
| Notification | `NotificationService.java` | 알림 시스템 |
| Calc Controller | `CalcController.java` | 계산기 API |
| Notification Controller | `NotificationController.java` | 알림 API |

### Phase 9: 테스트 및 최적화 (Week 5)

| 태스크 | 파일/모듈 | 설명 |
|--------|---------|------|
| 통합 테스트 | `*IntegrationTest.java` | 시나리오 테스트 |
| 부하 테스트 | k6 스크립트 | 성능 검증 |
| 캐시 최적화 | Redis 튜닝 | 성능 개선 |
| API 문서 | Swagger/OpenAPI | 문서화 |

### Phase 10: 배포 (Week 5)

| 태스크 | 파일/모듈 | 설명 |
|--------|---------|------|
| Dockerfile | `Dockerfile` | 이미지 빌드 |
| CI/CD | GitHub Actions | 자동 배포 |
| 모니터링 | Prometheus/Grafana | 관측성 |
| 프로덕션 배포 | AWS ECS/EC2 | 최종 배포 |

---

## 📎 부록

### A. 에러 코드

| 코드 | 설명 | HTTP 상태 |
|------|------|----------|
| AUTH_001 | 토큰 만료 | 401 |
| AUTH_002 | 유효하지 않은 토큰 | 401 |
| AUTH_003 | 권한 없음 | 403 |
| TRADE_001 | 잔고 부족 | 400 |
| TRADE_002 | 보유 수량 부족 | 400 |
| TRADE_003 | 거래 시간 외 | 400 |
| TRADE_004 | 유효하지 않은 종목 | 400 |
| GAME_001 | 코인 부족 | 400 |
| GAME_002 | 이미 보유한 아이템 | 400 |
| USER_001 | 사용자 없음 | 404 |
| SERVER_001 | 내부 서버 오류 | 500 |

### B. 의존성 (build.gradle)

```groovy
dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-websocket'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    
    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.5'
    
    // Database
    runtimeOnly 'org.postgresql:postgresql'
    implementation 'org.flywaydb:flyway-core'
    
    // Utility
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    implementation 'org.mapstruct:mapstruct:1.5.5.Final'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.5.Final'
    
    // API Documentation
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
    
    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'org.testcontainers:postgresql'
    testImplementation 'org.testcontainers:junit-jupiter'
}
```

---

**문서 버전:** 1.0  
**최종 수정일:** 2026-01-16  
**작성자:** Stock-Persona 개발팀
