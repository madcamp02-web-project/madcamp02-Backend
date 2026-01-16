# 🏗️ Spring MVC 구조 가이드

**MadCamp02 프로젝트 - Spring Boot MVC 아키텍처 설명서**

---

## 📋 목차

1. [Spring MVC란?](#1-spring-mvc란)
2. [MVC 패턴 기본 개념](#2-mvc-패턴-기본-개념)
3. [레이어별 역할](#3-레이어별-역할)
4. [데이터 흐름](#4-데이터-흐름)
5. [프로젝트 구조 매핑](#5-프로젝트-구조-매핑)
6. [실제 코드 예시](#6-실제-코드-예시)
7. [핵심 원칙](#7-핵심-원칙)

---

## 1. Spring MVC란?

**Spring MVC**는 웹 애플리케이션을 개발하기 위한 Spring 프레임워크의 모듈입니다.

### 간단한 비유

```
🍕 피자 주문 시스템으로 이해하기

1. 손님 (Client) → "페퍼로니 피자 주문할게요!"
   ↓
2. 웨이터 (Controller) → 주문을 받고 주방에 전달
   ↓
3. 주방장 (Service) → 피자 만드는 방법 결정, 재료 확인
   ↓
4. 재료 창고 (Repository) → 필요한 재료 가져오기
   ↓
5. 냉장고 (Database) → 실제 재료 저장소
```

### Spring MVC의 역할

- **Controller**: HTTP 요청을 받아서 처리
- **Service**: 비즈니스 로직 (실제 업무 처리)
- **Repository**: 데이터베이스 접근
- **Entity**: 데이터베이스 테이블과 매핑되는 객체

---

## 2. MVC 패턴 기본 개념

### MVC는 무엇의 약자?

- **M**odel (모델) = 데이터와 비즈니스 로직
- **V**iew (뷰) = 화면 표시 (REST API에서는 JSON 응답)
- **C**ontroller (컨트롤러) = 요청 처리 및 흐름 제어

### Spring MVC 구조도

```
┌─────────────────────────────────────────────────────────┐
│                    클라이언트 (브라우저/앱)                 │
└────────────────────┬────────────────────────────────────┘
                     │ HTTP Request
                     ▼
┌─────────────────────────────────────────────────────────┐
│              Controller (컨트롤러)                        │
│  • HTTP 요청 받기                                        │
│  • 요청 데이터 검증                                      │
│  • Service 호출                                          │
│  • 응답 데이터 변환                                      │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              Service (서비스)                            │
│  • 비즈니스 로직 처리                                    │
│  • 여러 Repository 조합                                 │
│  • 트랜잭션 관리                                         │
│  • 예외 처리                                            │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│           Repository (레포지토리)                        │
│  • 데이터베이스 접근                                     │
│  • CRUD 작업 (Create, Read, Update, Delete)            │
│  • 쿼리 실행                                            │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              Entity (엔티티)                             │
│  • 데이터베이스 테이블과 매핑                            │
│  • 실제 데이터 저장                                     │
└─────────────────────────────────────────────────────────┘
```

---

## 3. 레이어별 역할

### 3.1 Controller (컨트롤러) - 프레젠테이션 레이어

**역할**: HTTP 요청을 받고 응답을 반환하는 입구/출구

**책임**:
- ✅ HTTP 요청 받기 (`GET`, `POST`, `PUT`, `DELETE`)
- ✅ 요청 데이터를 DTO로 변환
- ✅ 데이터 유효성 검증 (`@Valid`)
- ✅ Service 메서드 호출
- ✅ Service 결과를 Response DTO로 변환
- ✅ HTTP 응답 반환 (JSON)

**하지 않는 것**:
- ❌ 비즈니스 로직 작성
- ❌ 데이터베이스 직접 접근
- ❌ 복잡한 계산 로직

**예시 위치**: `src/main/java/com/madcamp02/controller/`

```java
@RestController  // REST API 컨트롤러
@RequestMapping("/api/v1/trade")  // 기본 경로
public class TradeController {
    
    private final TradeService tradeService;  // Service 주입
    
    // 매수 주문 API
    @PostMapping("/order")  // POST /api/v1/trade/order
    public ResponseEntity<TradeResponse> submitOrder(
            @RequestBody TradeOrderRequest request,  // 요청 데이터
            @AuthenticationPrincipal UserDetails user  // 인증된 사용자
    ) {
        // 1. Service 호출 (비즈니스 로직은 Service에 위임)
        TradeResponse response = tradeService.executeOrder(
            user.getUserId(), 
            request
        );
        
        // 2. 응답 반환
        return ResponseEntity.ok(response);
    }
}
```

### 3.2 Service (서비스) - 비즈니스 로직 레이어

**역할**: 실제 업무 로직을 처리하는 핵심

**책임**:
- ✅ 비즈니스 규칙 구현
- ✅ 여러 Repository 조합하여 복잡한 작업 수행
- ✅ 트랜잭션 관리 (`@Transactional`)
- ✅ 예외 처리 및 변환
- ✅ 외부 API 호출 (Finnhub, AI 서버 등)

**하지 않는 것**:
- ❌ HTTP 요청/응답 직접 처리
- ❌ 데이터베이스 쿼리 직접 작성 (Repository 사용)

**예시 위치**: `src/main/java/com/madcamp02/service/`

```java
@Service  // 서비스 빈 등록
@Transactional  // 트랜잭션 관리
public class TradeService {
    
    private final TradeRepository tradeRepository;
    private final WalletRepository walletRepository;
    private final PortfolioRepository portfolioRepository;
    private final StockService stockService;
    
    public TradeResponse executeOrder(Long userId, TradeOrderRequest request) {
        // 1. 지갑 조회
        Wallet wallet = walletRepository.findByUserId(userId)
            .orElseThrow(() -> new BusinessException("지갑을 찾을 수 없습니다"));
        
        // 2. 현재가 조회 (외부 API)
        StockPrice currentPrice = stockService.getCurrentPrice(
            request.getTicker()
        );
        
        // 3. 잔고 확인 (비즈니스 로직)
        BigDecimal requiredAmount = currentPrice.getPrice()
            .multiply(BigDecimal.valueOf(request.getQuantity()));
        
        if (wallet.getCashBalance().compareTo(requiredAmount) < 0) {
            throw new TradeException("잔고가 부족합니다");
        }
        
        // 4. 거래 실행
        // ... (복잡한 비즈니스 로직)
        
        // 5. 결과 반환
        return TradeResponse.builder()
            .orderId(orderId)
            .ticker(request.getTicker())
            .executedPrice(currentPrice.getPrice())
            .build();
    }
}
```

### 3.3 Repository (레포지토리) - 데이터 접근 레이어

**역할**: 데이터베이스와 소통하는 창구

**책임**:
- ✅ 데이터베이스 CRUD 작업
- ✅ 쿼리 실행
- ✅ Entity 저장/조회/수정/삭제

**하지 않는 것**:
- ❌ 비즈니스 로직 작성
- ❌ 트랜잭션 관리 (Service에서 처리)

**예시 위치**: `src/main/java/com/madcamp02/domain/*/`

```java
@Repository  // 레포지토리 빈 등록
public interface UserRepository extends JpaRepository<User, Long> {
    
    // JPA가 자동으로 구현해주는 메서드들
    // - save(), findById(), findAll(), delete() 등
    
    // 커스텀 메서드 (JPA가 자동으로 쿼리 생성)
    Optional<User> findByEmail(String email);
    
    List<User> findBySajuElement(String sajuElement);
    
    // 직접 쿼리 작성
    @Query("SELECT u FROM User u WHERE u.createdAt > :date")
    List<User> findRecentUsers(@Param("date") LocalDateTime date);
}
```

### 3.4 Entity (엔티티) - 도메인 모델

**역할**: 데이터베이스 테이블과 1:1로 매핑되는 객체

**책임**:
- ✅ 테이블 구조 정의
- ✅ 컬럼 매핑
- ✅ 관계 설정 (OneToMany, ManyToOne 등)

**예시 위치**: `src/main/java/com/madcamp02/domain/*/`

```java
@Entity  // JPA 엔티티
@Table(name = "users")  // 테이블 이름
public class User {
    
    @Id  // Primary Key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // 자동 증가
    private Long userId;
    
    @Column(nullable = false, unique = true)  // NOT NULL, UNIQUE
    private String email;
    
    @Column(nullable = false)
    private String nickname;
    
    // 관계 설정
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Wallet wallet;  // 1:1 관계
    
    @OneToMany(mappedBy = "user")
    private List<Portfolio> portfolios;  // 1:N 관계
}
```

---

## 4. 데이터 흐름

### 4.1 전체 흐름도

```
[클라이언트] 
    │
    │ POST /api/v1/trade/order
    │ { "ticker": "AAPL", "quantity": 10 }
    ▼
[Controller] TradeController.submitOrder()
    │
    │ 1. 요청 데이터 검증
    │ 2. Service 호출
    ▼
[Service] TradeService.executeOrder()
    │
    │ 1. Wallet 조회 (WalletRepository)
    │ 2. 현재가 조회 (StockService)
    │ 3. 잔고 확인 (비즈니스 로직)
    │ 4. Portfolio 업데이트 (PortfolioRepository)
    │ 5. TradeLog 저장 (TradeLogRepository)
    │ 6. Wallet 업데이트 (WalletRepository)
    │ 7. 트랜잭션 커밋
    ▼
[Repository] JPA 작업
    │
    │ SQL 실행
    ▼
[Database] PostgreSQL
    │
    │ 데이터 저장
    ▼
[Repository] Entity 반환
    │
    ▼
[Service] TradeResponse 생성
    │
    ▼
[Controller] ResponseEntity 반환
    │
    │ JSON 응답
    ▼
[클라이언트] 
    {
      "orderId": 12345,
      "ticker": "AAPL",
      "executedPrice": 198.45
    }
```

### 4.2 단계별 상세 설명

#### Step 1: 클라이언트 요청
```http
POST /api/v1/trade/order
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

{
  "ticker": "AAPL",
  "type": "BUY",
  "quantity": 10
}
```

#### Step 2: Controller 처리
```java
@PostMapping("/order")
public ResponseEntity<TradeResponse> submitOrder(
        @RequestBody TradeOrderRequest request,  // JSON → DTO 변환
        @AuthenticationPrincipal UserDetails user
) {
    // 요청 데이터 자동 검증 (@Valid)
    // Service 호출
    TradeResponse response = tradeService.executeOrder(
        user.getUserId(), 
        request
    );
    return ResponseEntity.ok(response);
}
```

#### Step 3: Service 비즈니스 로직
```java
@Transactional  // 모든 작업이 하나의 트랜잭션
public TradeResponse executeOrder(Long userId, TradeOrderRequest request) {
    // 1. 지갑 조회
    Wallet wallet = walletRepository.findByUserId(userId)
        .orElseThrow(() -> new BusinessException("지갑 없음"));
    
    // 2. 현재가 조회
    StockPrice price = stockService.getCurrentPrice(request.getTicker());
    
    // 3. 잔고 확인
    if (잔고 부족) {
        throw new TradeException("잔고 부족");
    }
    
    // 4. 거래 실행
    // 5. 결과 반환
}
```

#### Step 4: Repository 데이터 접근
```java
// JPA가 자동으로 SQL 생성 및 실행
Wallet wallet = walletRepository.findByUserId(userId);
// → SELECT * FROM wallet WHERE user_id = ?

portfolioRepository.save(portfolio);
// → INSERT INTO portfolio ...

walletRepository.save(wallet);
// → UPDATE wallet SET cash_balance = ? WHERE wallet_id = ?
```

---

## 5. 프로젝트 구조 매핑

### 5.1 현재 프로젝트 구조

```
📁 src/main/java/com/madcamp02/
│
├── 📁 controller/          ← Controller Layer
│   ├── AuthController.java
│   ├── TradeController.java
│   ├── UserController.java
│   └── ...
│
├── 📁 service/             ← Service Layer
│   ├── AuthService.java
│   ├── TradeService.java
│   ├── UserService.java
│   └── ...
│
├── 📁 domain/              ← Repository + Entity Layer
│   ├── 📁 user/
│   │   ├── User.java              (Entity)
│   │   └── UserRepository.java   (Repository)
│   ├── 📁 wallet/
│   │   ├── Wallet.java
│   │   └── WalletRepository.java
│   └── ...
│
└── 📁 dto/                 ← DTO Layer
    ├── request/            (요청 DTO)
    └── response/           (응답 DTO)
```

### 5.2 레이어별 파일 예시

| 레이어 | 파일 예시 | 역할 |
|--------|----------|------|
| **Controller** | `TradeController.java` | HTTP 요청 처리 |
| **Service** | `TradeService.java` | 비즈니스 로직 |
| **Repository** | `TradeLogRepository.java` | 데이터 접근 |
| **Entity** | `TradeLog.java` | 데이터 모델 |
| **DTO** | `TradeOrderRequest.java` | 데이터 전송 객체 |

---

## 6. 실제 코드 예시

### 6.1 매수 주문 전체 흐름

#### 1. Controller
```java
@RestController
@RequestMapping("/api/v1/trade")
@RequiredArgsConstructor  // 생성자 주입
public class TradeController {
    
    private final TradeService tradeService;
    
    @PostMapping("/order")
    public ResponseEntity<TradeResponse> submitOrder(
            @Valid @RequestBody TradeOrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        TradeResponse response = tradeService.executeOrder(userId, request);
        return ResponseEntity.ok(response);
    }
}
```

#### 2. Service
```java
@Service
@RequiredArgsConstructor
@Transactional
public class TradeService {
    
    private final TradeRepository tradeRepository;
    private final WalletRepository walletRepository;
    private final PortfolioRepository portfolioRepository;
    private final StockService stockService;
    
    public TradeResponse executeOrder(Long userId, TradeOrderRequest request) {
        // 1. 지갑 조회
        Wallet wallet = walletRepository.findByUserId(userId)
            .orElseThrow(() -> new BusinessException("지갑을 찾을 수 없습니다"));
        
        // 2. 현재가 조회
        StockPrice currentPrice = stockService.getCurrentPrice(
            request.getTicker()
        );
        
        // 3. 잔고 확인
        BigDecimal totalAmount = currentPrice.getPrice()
            .multiply(BigDecimal.valueOf(request.getQuantity()));
        
        if (wallet.getCashBalance().compareTo(totalAmount) < 0) {
            throw new TradeException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        
        // 4. 지갑에서 돈 차감
        wallet.deductCash(totalAmount);
        
        // 5. 포트폴리오 업데이트
        Portfolio portfolio = portfolioRepository
            .findByUserIdAndTicker(userId, request.getTicker())
            .orElse(new Portfolio(userId, request.getTicker()));
        
        portfolio.addQuantity(request.getQuantity(), currentPrice.getPrice());
        portfolioRepository.save(portfolio);
        
        // 6. 거래 기록 저장
        TradeLog tradeLog = TradeLog.builder()
            .userId(userId)
            .ticker(request.getTicker())
            .tradeType(request.getType())
            .price(currentPrice.getPrice())
            .quantity(request.getQuantity())
            .totalAmount(totalAmount)
            .build();
        tradeRepository.save(tradeLog);
        
        // 7. 지갑 업데이트
        walletRepository.save(wallet);
        
        // 8. 응답 생성
        return TradeResponse.builder()
            .orderId(tradeLog.getLogId())
            .ticker(request.getTicker())
            .executedPrice(currentPrice.getPrice())
            .quantity(request.getQuantity())
            .build();
    }
}
```

#### 3. Repository
```java
@Repository
public interface TradeRepository extends JpaRepository<TradeLog, Long> {
    
    List<TradeLog> findByUserIdOrderByTradeDateDesc(Long userId);
    
    @Query("SELECT t FROM TradeLog t WHERE t.userId = :userId " +
           "AND t.tradeDate >= :startDate")
    List<TradeLog> findRecentTrades(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate
    );
}
```

#### 4. Entity
```java
@Entity
@Table(name = "trade_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false, length = 10)
    private String ticker;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private TradeType tradeType;
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime tradeDate;
    
    @Builder
    public TradeLog(Long userId, String ticker, TradeType tradeType,
                   BigDecimal price, Integer quantity, BigDecimal totalAmount) {
        this.userId = userId;
        this.ticker = ticker;
        this.tradeType = tradeType;
        this.price = price;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.tradeDate = LocalDateTime.now();
    }
}
```

---

## 7. 핵심 원칙

### 7.1 단방향 의존성 (의존성 방향)

```
Controller → Service → Repository → Entity
```

**규칙**:
- ✅ Controller는 Service만 의존
- ✅ Service는 Repository만 의존
- ✅ Repository는 Entity만 의존
- ❌ 역방향 의존 금지 (Service가 Controller를 참조하면 안됨)

### 7.2 DTO 사용 원칙

**왜 DTO를 사용하나요?**

1. **보안**: Entity의 모든 필드를 노출하지 않음
2. **유연성**: API 스펙 변경 시 Entity 수정 불필요
3. **성능**: 필요한 데이터만 전송

**사용 규칙**:
- Controller ↔ Service: **DTO 사용**
- Service ↔ Repository: **Entity 사용**
- Controller에서 Entity 직접 반환 ❌

```
[Controller] ←→ [Service] ←→ [Repository] ←→ [Entity]
    DTO           DTO/Entity     Entity         Entity
```

### 7.3 트랜잭션 관리

**규칙**:
- ✅ Service 레이어에서만 `@Transactional` 사용
- ❌ Controller에서는 사용하지 않음
- ✅ 읽기 전용 작업은 `@Transactional(readOnly = true)`

**예시**:
```java
@Service
public class TradeService {
    
    @Transactional  // 쓰기 작업
    public TradeResponse executeOrder(...) {
        // 여러 Repository 작업이 하나의 트랜잭션으로 처리됨
    }
    
    @Transactional(readOnly = true)  // 읽기 전용
    public List<TradeLog> getTradeHistory(Long userId) {
        // 읽기만 하므로 성능 최적화
    }
}
```

### 7.4 예외 처리

**계층별 예외 처리**:

```
[Controller]
    │
    │ 예외 발생 시
    ▼
[GlobalExceptionHandler]  ← 전역 예외 처리
    │
    │ 예외 타입별 처리
    ▼
[Client] 에러 응답
```

**예시**:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(TradeException.class)
    public ResponseEntity<ErrorResponse> handleTradeException(
            TradeException e) {
        return ResponseEntity
            .status(e.getErrorCode().getHttpStatus())
            .body(ErrorResponse.of(e.getErrorCode()));
    }
}
```

---

## 8. 자주 묻는 질문 (FAQ)

### Q1: Controller에서 Repository를 직접 사용해도 되나요?

**A**: ❌ 안됩니다. 비즈니스 로직이 Controller에 들어가면 안 되고, Service를 거쳐야 합니다.

```java
// ❌ 나쁜 예
@RestController
public class TradeController {
    private final TradeRepository tradeRepository;  // 직접 사용
    
    @PostMapping("/order")
    public void submitOrder(...) {
        // 비즈니스 로직이 Controller에...
        tradeRepository.save(...);  // ❌
    }
}

// ✅ 좋은 예
@RestController
public class TradeController {
    private final TradeService tradeService;  // Service 사용
    
    @PostMapping("/order")
    public ResponseEntity<TradeResponse> submitOrder(...) {
        return tradeService.executeOrder(...);  // ✅
    }
}
```

### Q2: Service에서 여러 Repository를 사용해도 되나요?

**A**: ✅ 네, 가능합니다. Service는 여러 Repository를 조합하여 복잡한 비즈니스 로직을 처리합니다.

```java
@Service
public class TradeService {
    private final WalletRepository walletRepository;
    private final PortfolioRepository portfolioRepository;
    private final TradeRepository tradeRepository;
    
    @Transactional
    public TradeResponse executeOrder(...) {
        // 여러 Repository 조합 사용 ✅
        Wallet wallet = walletRepository.findByUserId(userId);
        Portfolio portfolio = portfolioRepository.findByUserIdAndTicker(...);
        tradeRepository.save(tradeLog);
    }
}
```

### Q3: Entity를 Controller에서 직접 반환해도 되나요?

**A**: ❌ 안됩니다. DTO를 사용해야 합니다.

```java
// ❌ 나쁜 예
@GetMapping("/user")
public User getUser() {  // Entity 직접 반환
    return userRepository.findById(userId);
}

// ✅ 좋은 예
@GetMapping("/user")
public UserResponse getUser() {  // DTO 반환
    User user = userService.getUser(userId);
    return UserResponse.from(user);
}
```

---

## 9. 요약

### Spring MVC의 핵심

1. **Controller**: HTTP 요청/응답 처리
2. **Service**: 비즈니스 로직 처리
3. **Repository**: 데이터베이스 접근
4. **Entity**: 데이터 모델

### 데이터 흐름

```
Client → Controller → Service → Repository → Database
         (DTO)       (DTO/Entity) (Entity)   (Table)
```

### 핵심 원칙

- ✅ 단방향 의존성 유지
- ✅ DTO 사용 (Controller ↔ Service)
- ✅ 트랜잭션은 Service에서 관리
- ✅ 예외는 전역 핸들러에서 처리

---

**문서 버전:** 1.0  
**최종 수정일:** 2026-01-16  
**작성자:** MadCamp02 개발팀
