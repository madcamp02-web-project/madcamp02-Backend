# 외부 API 사용 현황

이 문서는 MadCamp02 백엔드에서 사용하는 모든 외부 API를 정리한 문서입니다.

## 1. Finnhub API

**공식 문서**: https://finnhub.io/docs/api  
**Base URL**: `https://finnhub.io/api/v1`  
**인증**: API Key (Query Parameter: `token` 또는 Header: `X-Finnhub-Token`)

### 1.1 Quote API (현재가 조회)

**엔드포인트**: `GET /quote`  
**사용 위치**:
- `MarketService.getIndices()` - 주요 지수 조회
- `MarketService.getMovers()` - 급등/급락 종목 조회
- `StockService.getQuote()` - 개별 종목 현재가 조회

**요청 파라미터**:
- `symbol`: 종목 심볼 (예: `AAPL`, `SPY`, `QQQ`, `DIA`)
- `token`: API Key
- **주의**: 지수 심볼(`^DJI`, `^GSPC`, `^IXIC`)은 지원하지 않음. 지수 데이터는 ETF를 사용 (SPY=S&P 500, QQQ=NASDAQ-100, DIA=Dow Jones)

**응답 필드**:
- `c`: Current price (현재가)
- `d`: Change (변동액)
- `dp`: Percent change (변동률)
- `h`: High price (당일 최고가)
- `l`: Low price (당일 최저가)
- `o`: Open price (당일 시가)
- `pc`: Previous close (전일 종가)
- `t`: Timestamp (UNIX timestamp, 초)

**주의사항**:
- **지수 심볼 미지원**: `^DJI`, `^GSPC`, `^IXIC` 같은 지수 심볼은 지원하지 않음. US stocks만 지원
- **지수 데이터 대안**: 지수 데이터는 해당 지수를 추적하는 ETF를 사용 (SPY=S&P 500, QQQ=NASDAQ-100, DIA=Dow Jones)
- 장 마감 후 `c` (currentPrice)가 `0` 또는 `null`일 수 있음
- 이 경우 `pc` (previousClose)를 사용해야 함
- Free Tier: 60 calls/minute 제한

### 1.2 Search API (종목 검색)

**엔드포인트**: `GET /search`  
**사용 위치**:
- `StockService.searchStock()` - 종목 검색
- `MarketService.getMovers()` - 종목명 조회

**요청 파라미터**:
- `q`: 검색어 (종목명, 심볼, ISIN, CUSIP) (필수)
- `exchange`: 거래소 제한 (선택, 예: `US`)
- `token`: API Key

**응답 필드**:
- `count`: 검색 결과 개수
- `result[]`: 검색 결과 리스트
  - `description`: 종목 설명
  - `displaySymbol`: 표시 심볼
  - `symbol`: 심볼
  - `type`: 타입 (Common Stock, ETF 등)

### 1.3 News API (시장 뉴스)

**엔드포인트**: `GET /news`  
**사용 위치**:
- `MarketService.getNews()` - 시장 뉴스 조회

**요청 파라미터**:
- `category`: 카테고리 (`general`, `forex`, `crypto`, `merger`) (필수)
- `minId`: 이 ID 이후의 뉴스만 조회 (선택, 기본값: `0`)
- `token`: API Key

**응답 필드**:
- `category`: 카테고리
- `datetime`: UNIX timestamp (초)
- `headline`: 헤드라인
- `id`: 뉴스 ID
- `image`: 이미지 URL
- `related`: 관련 종목 심볼
- `source`: 출처
- `summary`: 요약
- `url`: 뉴스 URL

---

## 2. EODHD API

**공식 문서**: https://eodhd.com/docs/api  
**Base URL**: `https://eodhd.com/api`  
**인증**: API Key (Query Parameter: `api_token`)

### 2.1 Historical Data API (캔들 데이터)

**엔드포인트**: `GET /eod/{ticker}`  
**사용 위치**:
- `StockService.getCandles()` - 캔들 차트 데이터 조회

**요청 파라미터**:
- `ticker`: 종목 심볼 (예: `AAPL.US`, `AAPL` → 자동으로 `.US` 추가)
- `api_token`: API Key
- `fmt`: 응답 형식 (`json`)
- `from`: 시작 날짜 (YYYY-MM-DD)
- `to`: 종료 날짜 (YYYY-MM-DD)

**응답 필드**:
- `date`: 날짜 (YYYY-MM-DD)
- `open`: 시가
- `high`: 고가
- `low`: 저가
- `close`: 종가
- `adjusted_close`: 조정 종가
- `volume`: 거래량
- `warning`: 경고 메시지 (무료 구독 제한 등)

**주의사항**:
- **무료 구독 제한**: 최근 1년 데이터만 제공
- **일일 호출 제한**: 20회/일
- 1년 이전 데이터 요청 시 `warning` 필드에 경고 메시지 반환
- `warning` 필드가 있는 응답은 필터링하여 유효한 캔들 데이터만 저장

---

## 3. 한국천문연구원 API

**공식 문서**: https://www.data.go.kr/data/15012679/openapi.do  
**Base URL**: `https://apis.data.go.kr/B090041/openapi/service/LrsrCldInfoService`  
**인증**: Service Key (Query Parameter: `ServiceKey`)

### 3.1 양력일정보 조회 (음력 → 양력 변환)

**엔드포인트**: `GET /getSolCalInfo`  
**사용 위치**:
- `SajuCalculator.calculatePrecise()` - 정밀 사주 계산

**요청 파라미터**:
- `lunYear`: 연(음력), 4자리
- `lunMonth`: 월(음력), 2자리
- `lunDay`: 일(음력), 2자리
- `ServiceKey`: 서비스 키

**응답 필드**:
- `solYear`: 양력 연도
- `solMonth`: 양력 월
- `solDay`: 양력 일
- `lunLeapmonth`: 윤달구분 (평/윤)

### 3.2 음력일정보 조회 (양력 → 음력 변환)

**엔드포인트**: `GET /getLunCalInfo`  
**사용 위치**:
- `LunarCalendarClient.convertSolarToLunar()` - 양력 → 음력 변환

**요청 파라미터**:
- `solYear`: 연, 4자리
- `solMonth`: 월, 2자리
- `solDay`: 일, 2자리
- `ServiceKey`: 서비스 키

**응답 필드**:
- `lunYear`: 음력 연도
- `lunMonth`: 음력 월
- `lunDay`: 음력 일
- `lunLeapmonth`: 윤달구분 (평/윤)

---

## 4. API 사용 현황 요약

| 백엔드 API | 외부 API | 엔드포인트 | 용도 |
|-----------|---------|----------|------|
| `GET /api/v1/market/indices` | Finnhub | `/quote` | 주요 지수 조회 (ETF 사용: SPY, QQQ, DIA) |
| `GET /api/v1/market/news` | Finnhub | `/news` | 시장 뉴스 조회 |
| `GET /api/v1/market/movers` | Finnhub | `/quote`, `/search` | 급등/급락 종목 조회 |
| `GET /api/v1/stock/search` | Finnhub | `/search` | 종목 검색 |
| `GET /api/v1/stock/quote/{ticker}` | Finnhub | `/quote` | 개별 종목 현재가 조회 |
| `GET /api/v1/stock/candles/{ticker}` | EODHD | `/eod/{ticker}` | 캔들 차트 데이터 조회 |
| `POST /api/v1/user/onboarding` | 한국천문연구원 | `/getSolCalInfo` | 음력 → 양력 변환 (사주 계산) |

---

## 5. 환경 변수 설정

다음 환경 변수를 설정해야 합니다:

```bash
# Finnhub API
FINNHUB_API_KEY=your_finnhub_api_key

# EODHD API
EODHD_API_KEY=your_eodhd_api_key

# 한국천문연구원 API
KASI_SERVICE_KEY=your_kasi_service_key
```

---

**최종 수정일**: 2026-01-19

**변경 이력**:
- 2026-01-19: 지수 조회를 ETF로 변경 (Finnhub Quote API는 지수 심볼 미지원) - SPY, QQQ, DIA 사용
- 2026-01-19: Finnhub News API에 `minId` 파라미터 지원 추가, Finnhub Search API에 `exchange` 파라미터 지원 추가
- 2026-01-19: Market Movers Top 20 Market Cap DB 관리 구현 완료

---

## 6. 추가 참고사항

### 6.1 Market Movers 구현 현황

**구현 완료** (Phase 3.5):
- `GET /api/v1/market/movers`는 DB에서 관리되는 Top 20 Market Cap 종목 리스트를 사용합니다.
- **DB 테이블**: `market_cap_stocks` (Flyway V6)
  - `symbol`: 종목 심볼 (UNIQUE)
  - `company_name`: 회사명
  - `market_cap_rank`: 시가총액 순위 (1~20)
  - `is_active`: 활성화 여부
- **초기 데이터**: 20개 종목 (2026-01-19 기준)
  - 상위 20개: `AAPL`, `MSFT`, `GOOGL`, `AMZN`, `NVDA`, `META`, `TSLA`, `BRK.B`, `V`, `UNH`, `JNJ`, `WMT`, `JPM`, `MA`, `PG`, `HD`, `DIS`, `AVGO`, `PEP`, `COST`
- **동작 방식**:
  1. DB에서 활성화된 종목을 시가총액 순위 순으로 조회
  2. 각 종목의 Quote API를 호출하여 `changePercent` 계산
  3. `changePercent` 기준으로 정렬 후 상위 5개 반환
  4. 종목명은 DB의 `company_name` 우선 사용, 없으면 Search API로 조회
- **Fallback 로직**: DB에 데이터가 없으면 기존 하드코딩 리스트(10개 종목) 사용

---

## 7. 백엔드 API 테스트 방법

이 섹션에서는 MadCamp02 백엔드 API를 테스트하는 방법을 설명합니다.

### 7.1 Swagger UI를 사용한 테스트

**Swagger UI 접속**: `http://localhost:8080/swagger-ui.html` (또는 서버 주소)

#### 7.1.1 인증 설정

1. Swagger UI에서 "Authorize" 버튼 클릭
2. "bearer-key" 입력 필드에 JWT Access Token 입력
   - "Bearer " 접두사는 자동으로 추가되므로 토큰만 입력
   - 예: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
3. "Authorize" 클릭하여 저장

**토큰 발급 방법**:
- `POST /api/v1/auth/login` - 이메일/비밀번호 로그인
- `POST /api/v1/auth/signup` - 회원가입
- `POST /api/v1/auth/oauth/kakao` - 카카오 로그인
- `POST /api/v1/auth/oauth/google` - 구글 로그인

---

### 7.2 인증 API (`/api/v1/auth`)

#### 7.2.1 회원가입

**엔드포인트**: `POST /api/v1/auth/signup`  
**인증**: 불필요 (Public API)

**Swagger UI 테스트**:
1. Auth 컨트롤러의 `/signup` 엔드포인트 선택
2. "Try it out" 클릭
3. Request Body 입력:
   ```json
   {
     "email": "test@example.com",
     "password": "password123",
     "nickname": "테스트유저"
   }
   ```
4. "Execute" 클릭

**curl 명령어**:
```bash
curl -X POST "http://localhost:8080/api/v1/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "nickname": "테스트유저"
  }'
```

**응답 예시**:
```json
{
  "userId": 1,
  "email": "test@example.com",
  "nickname": "테스트유저",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "isNewUser": true
}
```

---

#### 7.2.2 이메일 로그인

**엔드포인트**: `POST /api/v1/auth/login`  
**인증**: 불필요 (Public API)

**Swagger UI 테스트**:
1. Auth 컨트롤러의 `/login` 엔드포인트 선택
2. "Try it out" 클릭
3. Request Body 입력:
   ```json
   {
     "email": "test@example.com",
     "password": "password123"
   }
   ```
4. "Execute" 클릭

**curl 명령어**:
```bash
curl -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

**응답 예시**: 회원가입과 동일한 형식

---

#### 7.2.3 카카오 OAuth 로그인

**엔드포인트**: `POST /api/v1/auth/oauth/kakao`  
**인증**: 불필요 (Public API)

**Swagger UI 테스트**:
1. Auth 컨트롤러의 `/oauth/kakao` 엔드포인트 선택
2. "Try it out" 클릭
3. Request Body 입력:
   ```json
   {
     "accessToken": "카카오에서_발급받은_Access_Token"
   }
   ```
4. "Execute" 클릭

**curl 명령어**:
```bash
curl -X POST "http://localhost:8080/api/v1/auth/oauth/kakao" \
  -H "Content-Type: application/json" \
  -d '{
    "accessToken": "카카오에서_발급받은_Access_Token"
  }'
```

---

#### 7.2.4 구글 OAuth 로그인

**엔드포인트**: `POST /api/v1/auth/oauth/google`  
**인증**: 불필요 (Public API)

**Swagger UI 테스트**:
1. Auth 컨트롤러의 `/oauth/google` 엔드포인트 선택
2. "Try it out" 클릭
3. Request Body 입력:
   ```json
   {
     "provider": "google",
     "idToken": "구글에서_발급받은_ID_Token"
   }
   ```
4. "Execute" 클릭

**curl 명령어**:
```bash
curl -X POST "http://localhost:8080/api/v1/auth/oauth/google" \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "google",
    "idToken": "구글에서_발급받은_ID_Token"
  }'
```

---

#### 7.2.5 토큰 갱신

**엔드포인트**: `POST /api/v1/auth/refresh`  
**인증**: 불필요 (Public API)

**Swagger UI 테스트**:
1. Auth 컨트롤러의 `/refresh` 엔드포인트 선택
2. "Try it out" 클릭
3. Request Body 입력:
   ```json
   {
     "refreshToken": "이전에_발급받은_Refresh_Token"
   }
   ```
4. "Execute" 클릭

**curl 명령어**:
```bash
curl -X POST "http://localhost:8080/api/v1/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "이전에_발급받은_Refresh_Token"
  }'
```

---

#### 7.2.6 현재 사용자 정보 조회

**엔드포인트**: `GET /api/v1/auth/me`  
**인증**: 필요 (Bearer Token)

**Swagger UI 테스트**:
1. Swagger UI에서 "Authorize" 버튼으로 토큰 설정
2. Auth 컨트롤러의 `/me` 엔드포인트 선택
3. "Try it out" 클릭
4. "Execute" 클릭

**curl 명령어**:
```bash
curl -X GET "http://localhost:8080/api/v1/auth/me" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

#### 7.2.7 로그아웃

**엔드포인트**: `POST /api/v1/auth/logout`  
**인증**: 필요 (Bearer Token)

**Swagger UI 테스트**:
1. Swagger UI에서 "Authorize" 버튼으로 토큰 설정
2. Auth 컨트롤러의 `/logout` 엔드포인트 선택
3. "Try it out" 클릭
4. "Execute" 클릭

**curl 명령어**:
```bash
curl -X POST "http://localhost:8080/api/v1/auth/logout" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

### 7.3 사용자 API (`/api/v1/user`)

#### 7.3.1 내 프로필 조회

**엔드포인트**: `GET /api/v1/user/me`  
**인증**: 필요 (Bearer Token)

**Swagger UI 테스트**:
1. Swagger UI에서 "Authorize" 버튼으로 토큰 설정
2. User 컨트롤러의 `/me` 엔드포인트 선택
3. "Try it out" 클릭
4. "Execute" 클릭

**curl 명령어**:
```bash
curl -X GET "http://localhost:8080/api/v1/user/me" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**응답 예시**:
```json
{
  "userId": 1,
  "email": "test@example.com",
  "nickname": "테스트유저",
  "sajuElement": "FIRE",
  "zodiacSign": "용",
  "avatarUrl": null,
  "isPublic": true,
  "isRankingJoined": true
}
```

---

#### 7.3.2 내 프로필 수정

**엔드포인트**: `PUT /api/v1/user/me`  
**인증**: 필요 (Bearer Token)

**Swagger UI 테스트**:
1. Swagger UI에서 "Authorize" 버튼으로 토큰 설정
2. User 컨트롤러의 `/me` (PUT) 엔드포인트 선택
3. "Try it out" 클릭
4. Request Body 입력:
   ```json
   {
     "nickname": "수정된닉네임",
     "isPublic": false,
     "isRankingJoined": true,
     "avatarUrl": "https://example.com/avatar.png"
   }
   ```
5. "Execute" 클릭

**curl 명령어**:
```bash
curl -X PUT "http://localhost:8080/api/v1/user/me" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nickname": "수정된닉네임",
    "isPublic": false,
    "isRankingJoined": true,
    "avatarUrl": "https://example.com/avatar.png"
  }'
```

---

#### 7.3.3 온보딩 (사주 계산)

**엔드포인트**: `POST /api/v1/user/onboarding`  
**인증**: 필요 (Bearer Token)

**Swagger UI 테스트**:
1. Swagger UI에서 "Authorize" 버튼으로 토큰 설정
2. User 컨트롤러의 `/onboarding` 엔드포인트 선택
3. "Try it out" 클릭
4. Request Body 입력:
   ```json
   {
     "birthDate": "2000-01-01",
     "birthTime": "13:05:00",
     "gender": "MALE",
     "calendarType": "SOLAR"
   }
   ```
5. "Execute" 클릭

**curl 명령어**:
```bash
curl -X POST "http://localhost:8080/api/v1/user/onboarding" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "birthDate": "2000-01-01",
    "birthTime": "13:05:00",
    "gender": "MALE",
    "calendarType": "SOLAR"
  }'
```

**참고사항**:
- `calendarType`: `SOLAR` (양력), `LUNAR` (음력), `LUNAR_LEAP` (음력 윤달)
- `gender`: `MALE`, `FEMALE`, `OTHER`
- 음력 변환은 한국천문연구원 API를 사용합니다

---

#### 7.3.4 지갑 정보 조회

**엔드포인트**: `GET /api/v1/user/wallet`  
**인증**: 필요 (Bearer Token)

**Swagger UI 테스트**:
1. Swagger UI에서 "Authorize" 버튼으로 토큰 설정
2. User 컨트롤러의 `/wallet` 엔드포인트 선택
3. "Try it out" 클릭
4. "Execute" 클릭

**curl 명령어**:
```bash
curl -X GET "http://localhost:8080/api/v1/user/wallet" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**응답 예시**:
```json
{
  "cashBalance": 10000.0,
  "coinBalance": 500,
  "currency": "USD"
}
```

---

### 7.4 시장 API (`/api/v1/market`)

#### 7.4.1 주요 지수 조회

**엔드포인트**: `GET /api/v1/market/indices`  
**인증**: 불필요 (Public API)

**Swagger UI 테스트**:
1. Market 컨트롤러의 `/indices` 엔드포인트 선택
2. "Try it out" 클릭
3. "Execute" 클릭

**curl 명령어**:
```bash
curl -X GET "http://localhost:8080/api/v1/market/indices"
```

**응답 예시**:
```json
{
  "asOf": "2026-01-19T12:00:00",
  "items": [
    {
      "code": "NASDAQ",
      "name": "NASDAQ",
      "value": 15000.12,
      "change": 123.45,
      "changePercent": 0.83,
      "currency": "USD"
    },
    {
      "code": "SP500",
      "name": "SP500",
      "value": 4800.56,
      "change": -12.34,
      "changePercent": -0.26,
      "currency": "USD"
    }
  ]
}
```

**참고사항**:
- Redis 캐싱: 60초 TTL
- Finnhub Quote API 사용

---

#### 7.4.2 시장 뉴스 조회

**엔드포인트**: `GET /api/v1/market/news`  
**인증**: 불필요 (Public API)

**Swagger UI 테스트**:
1. Market 컨트롤러의 `/news` 엔드포인트 선택
2. "Try it out" 클릭
3. "Execute" 클릭

**curl 명령어**:
```bash
curl -X GET "http://localhost:8080/api/v1/market/news"
```

**응답 예시**:
```json
{
  "asOf": "2026-01-19T12:00:00",
  "items": [
    {
      "id": "finnhub:123456",
      "headline": "Market headline",
      "summary": "Short summary",
      "source": "Reuters",
      "url": "https://example.com/news/123456",
      "imageUrl": "https://example.com/image.jpg",
      "publishedAt": "2026-01-19T11:50:00"
    }
  ]
}
```

**참고사항**:
- Redis 캐싱: 300초 TTL (5분)
- Finnhub News API 사용
- 최대 20개 뉴스 반환

---

#### 7.4.3 급등/급락 종목 조회

**엔드포인트**: `GET /api/v1/market/movers`  
**인증**: 불필요 (Public API)

**Swagger UI 테스트**:
1. Market 컨트롤러의 `/movers` 엔드포인트 선택
2. "Try it out" 클릭
3. "Execute" 클릭

**curl 명령어**:
```bash
curl -X GET "http://localhost:8080/api/v1/market/movers"
```

**응답 예시**:
```json
{
  "asOf": "2026-01-19T12:00:00",
  "items": [
    {
      "ticker": "AAPL",
      "name": "Apple Inc.",
      "price": 195.12,
      "changePercent": 2.34,
      "volume": 12345678,
      "direction": "UP"
    },
    {
      "ticker": "TSLA",
      "name": "Tesla, Inc.",
      "price": 250.50,
      "changePercent": -1.23,
      "volume": 9876543,
      "direction": "DOWN"
    }
  ]
}
```

**참고사항**:
- Redis 캐싱: 60초 TTL
- DB에서 Top 20 Market Cap 종목 조회 (`market_cap_stocks` 테이블)
- `changePercent` 기준으로 정렬 후 상위 5개 반환
- Fallback: DB에 데이터가 없으면 하드코딩 리스트(10개 종목) 사용

---

### 7.5 주식 API (`/api/v1/stock`)

#### 7.5.1 종목 검색

**엔드포인트**: `GET /api/v1/stock/search?keyword={keyword}`  
**인증**: 불필요 (Public API)

**Swagger UI 테스트**:
1. Stock 컨트롤러의 `/search` 엔드포인트 선택
2. "Try it out" 클릭
3. `keyword` 파라미터 입력 (예: `AAPL` 또는 `Apple`)
4. "Execute" 클릭

**curl 명령어**:
```bash
curl -X GET "http://localhost:8080/api/v1/stock/search?keyword=AAPL"
```

**응답 예시**:
```json
{
  "items": [
    {
      "symbol": "AAPL",
      "description": "Apple Inc.",
      "displaySymbol": "AAPL",
      "type": "Common Stock"
    }
  ]
}
```

**참고사항**:
- Finnhub Search API 사용
- 최대 20개 결과 반환
- 종목명, 심볼, ISIN, CUSIP으로 검색 가능

---

#### 7.5.2 현재가 조회

**엔드포인트**: `GET /api/v1/stock/quote/{ticker}`  
**인증**: 불필요 (Public API)

**Swagger UI 테스트**:
1. Stock 컨트롤러의 `/quote/{ticker}` 엔드포인트 선택
2. "Try it out" 클릭
3. `ticker` 파라미터 입력 (예: `AAPL`)
4. "Execute" 클릭

**curl 명령어**:
```bash
curl -X GET "http://localhost:8080/api/v1/stock/quote/AAPL"
```

**응답 예시**:
```json
{
  "ticker": "AAPL",
  "currentPrice": 195.12,
  "open": 193.50,
  "high": 196.00,
  "low": 192.80,
  "previousClose": 194.50,
  "change": 0.62,
  "changePercent": 0.32
}
```

**참고사항**:
- Finnhub Quote API 사용
- 장 마감 후 `currentPrice`가 `0` 또는 `null`일 수 있음 (이 경우 `previousClose` 사용)

---

#### 7.5.3 캔들 차트 데이터 조회

**엔드포인트**: `GET /api/v1/stock/candles/{ticker}?resolution={D}&from={timestamp}&to={timestamp}`  
**인증**: 불필요 (Public API)

**Swagger UI 테스트**:
1. Stock 컨트롤러의 `/candles/{ticker}` 엔드포인트 선택
2. "Try it out" 클릭
3. 파라미터 입력:
   - `ticker`: `AAPL`
   - `resolution`: `D` (일간), `W` (주간), `M` (월간), `1`, `5`, `15`, `30`, `60` (분봉)
   - `from`: `2026-01-01T00:00:00` (ISO-8601 형식)
   - `to`: `2026-01-19T23:59:59` (ISO-8601 형식)
4. "Execute" 클릭

**curl 명령어**:
```bash
curl -X GET "http://localhost:8080/api/v1/stock/candles/AAPL?resolution=D&from=2026-01-01T00:00:00&to=2026-01-19T23:59:59"
```

**응답 예시**:
```json
{
  "ticker": "AAPL",
  "resolution": "D",
  "items": [
    {
      "timestamp": 1704067200,
      "open": 193.50,
      "high": 196.00,
      "low": 192.80,
      "close": 195.12,
      "volume": 50000000
    }
  ],
  "stale": false
}
```

**참고사항**:
- **데이터 전략**: EODHD API + DB 캐싱
- **동작 흐름**:
  1. DB 조회 우선 (`stock_candles` 테이블)
  2. 데이터 최신성 체크 (오늘 데이터 존재 여부)
  3. Quota 체크 (일일 20회 제한)
  4. 필요 시 EODHD API 호출 → DB 저장
- **Quota 초과 시**:
  - 기존 데이터 있음: `stale: true`로 반환
  - 기존 데이터 없음: `429 Too Many Requests` 에러
- **무료 구독 제한**: 최근 1년 데이터만 제공
- **티커 형식**: `AAPL` → 자동으로 `AAPL.US`로 변환

---

### 7.6 테스트 시 주의사항

1. **인증이 필요한 API**:
   - 먼저 로그인 API로 토큰을 발급받아야 합니다
   - Swagger UI에서는 "Authorize" 버튼으로 토큰을 설정합니다
   - curl 명령어에서는 `-H "Authorization: Bearer YOUR_ACCESS_TOKEN"` 헤더를 추가합니다

2. **Public API**:
   - Market API와 Stock API는 인증 없이 접근 가능합니다
   - Redis 캐싱이 적용되어 있어 동일한 요청은 캐시에서 반환됩니다

3. **EODHD API 제한**:
   - 일일 20회 호출 제한이 있습니다
   - Quota 초과 시 기존 데이터를 `stale: true`로 반환하거나 429 에러를 반환합니다
   - 무료 구독은 최근 1년 데이터만 제공합니다

4. **에러 응답**:
   - 모든 에러는 `ErrorResponse` 형식으로 반환됩니다
   - 예: `{"timestamp": "...", "status": 400, "error": "TRADE_001", "message": "잔고가 부족합니다."}`

---

**최종 수정일**: 2026-01-19  
**변경 이력**:
- 2026-01-19: 각 API별 테스트 방법 추가 (Swagger UI + curl 명령어)
