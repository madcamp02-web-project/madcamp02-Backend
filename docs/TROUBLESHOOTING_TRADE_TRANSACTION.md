# 🛠️ 트러블슈팅: 거래 트랜잭션 및 동시성 테스트 문제 해결

**작성일**: 2026-01-19  
**관련 모듈**: `TradeService`, `TradeServiceConcurrencyTest`  
**관련 Phase**: Phase 4 (Trade/Portfolio Engine)

---

## 1. 문제 상황 (Background)

매수/매도 주문의 동시성 제어(Concurrency Control)를 검증하기 위한 테스트(`TradeServiceConcurrencyTest`) 수행 중 다음과 같은 실패가 반복됨.

- **증상**: 동시 요청 시 기대했던 `성공 + 실패` 합계가 전체 스레드 수와 일치하지 않음.
- **오류 메시지**: `AssertionFailedError: expected: 15 but was: 0` (스레드는 돌았으나 결과 집계가 안 됨)
- **로그 분석**: `InvalidDataAccessApiUsageException: Query requires transaction be in progress`

## 2. 원인 분석 (Root Cause Analysis)

### 2.1 트랜잭션 미적용 (Self-invocation 문제)
`TradeService`의 설계는 외부 API 호출 지연이 DB 트랜잭션을 잡고 있는 것을 방지하기 위해 두 단계로 분리되어 있었음.

1. `executeOrder()`: 외부 API 호출 (트랜잭션 없음)
2. `executeOrderInTransaction()`: DB 작업 (트랜잭션 있음, `@Transactional`)

**문제점**: `executeOrder()` 내부에서 `this.executeOrderInTransaction()`을 호출함.
- Spring의 AOP 프록시는 **외부에서 객체를 호출할 때**만 동작함.
- 객체 내부에서 자신의 메서드를 호출(`this.method`)하면 프록시를 거치지 않고 원본 객체의 메서드가 바로 실행됨.
- 결과적으로 `@Transactional`이 무시되어, DB 락(`findByUserIdWithLock`)을 걸려고 할 때 "트랜잭션이 없다"는 예외가 발생함.

### 2.2 테스트 코드의 스레드 안전성 문제
- 테스트에서 예외를 수집하기 위해 사용한 `ArrayList`는 스레드 안전(Thread-safe)하지 않음.
- 멀티 스레드 환경에서 동시에 예외가 발생하여 리스트에 `add()` 할 때 경합 조건(Race Condition) 발생으로 일부 예외가 유실됨.
- 이로 인해 테스트 결과 검증 시 카운트가 맞지 않는 현상 발생.

### 2.3 테스트 환경의 트랜잭션 격리 문제
- 테스트 클래스 레벨에 `@Transactional`을 붙임.
- 메인 스레드가 시작한 트랜잭션(테스트 데이터 생성)이 커밋되지 않은 상태에서, 별도의 스레드(`ExecutorService`)가 DB에 접근하려 함.
- 격리 수준에 의해 별도 스레드는 테스트 데이터를 볼 수 없어 `UserNotFound` 등의 에러 발생.

## 3. 해결 조치 (Solution)

### 3.1 Self-injection을 통한 트랜잭션 적용
Spring의 자기 주입 패턴을 사용하여 프록시를 경유하도록 수정함.

```java
@Service
public class TradeService {
    
    @Autowired
    @Lazy // 순환 참조 방지
    private TradeService self;

    public TradeResponse executeOrder(...) {
        // 1. 외부 API 호출 (Transaction-less)
        StockQuoteResponse quote = stockService.getQuote(...);
        
        // 2. 프록시를 통한 내부 호출 (Transaction applied)
        // this.executeOrderInTransaction(...) -> (X) 트랜잭션 미적용
        return self.executeOrderInTransaction(...); // (O) 트랜잭션 적용
    }

    @Transactional
    public TradeResponse executeOrderInTransaction(...) { // private -> public 변경
        // ... 비즈니스 로직 (Lock 획득) ...
    }
}
```

- **보안 검토**: `executeOrderInTransaction`을 `public`으로 열었으나, Controller에 매핑되지 않아 외부 HTTP 요청으로는 접근 불가능함. 내부 오용 방지를 위해 네이밍으로 명시함.

### 3.2 테스트 코드 리팩토링
1. **트랜잭션 제거**: 테스트 클래스의 `@Transactional` 제거.
2. **수동 데이터 정리**: `@AfterEach`에서 `repository.deleteAll()` 호출로 데이터 격리 보장.
3. **스레드 안전한 리스트 사용**: `Collections.synchronizedList(new ArrayList<>())` 적용.
4. **Mocking 적용**: `StockService`를 `@MockBean`으로 대체하여 외부 API(Finnhub) 호출로 인한 지연/실패 변수 제거.

## 4. 검증 결과 (Verification)

- **동시성 테스트 통과**: 
  - 동시 매수 주문 15개 요청 (잔고는 10개분) → **정확히 10개 성공, 5개 실패 확인.**
  - 동시 매도 주문 15개 요청 (보유는 10개) → **정확히 10개 성공, 5개 실패 확인.**
- **트랜잭션 분리 확인**: 외부 API 호출 구간과 DB 트랜잭션 구간이 정확히 분리되어 동작함.
- **계획 정합성**: `BACKEND_DEVELOPMENT_PLAN.md`의 Phase 4 설계 요구사항을 100% 충족함.

---
**Status**: ✅ Resolved
