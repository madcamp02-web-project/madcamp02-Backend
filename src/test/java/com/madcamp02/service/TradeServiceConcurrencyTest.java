package com.madcamp02.service;

import com.madcamp02.domain.portfolio.Portfolio;
import com.madcamp02.domain.portfolio.PortfolioRepository;
import com.madcamp02.domain.trade.TradeLog;
import com.madcamp02.domain.trade.TradeLogRepository;
import com.madcamp02.domain.user.User;
import com.madcamp02.domain.user.UserRepository;
import com.madcamp02.domain.wallet.Wallet;
import com.madcamp02.domain.wallet.WalletRepository;
import com.madcamp02.dto.request.TradeOrderRequest;
import com.madcamp02.dto.response.StockQuoteResponse;
import com.madcamp02.exception.ErrorCode;
import com.madcamp02.exception.TradeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * TradeService 동시성 테스트
 * Phase 4: Trade/Portfolio Engine
 * 
 * 비관적 락이 제대로 동작하는지 확인하는 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
class TradeServiceConcurrencyTest {

    @Autowired
    private TradeService tradeService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private TradeLogRepository tradeLogRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private StockService stockService;

    private User testUser;
    private Wallet testWallet;
    private String testTicker = "AAPL";

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        tradeLogRepository.deleteAll();
        portfolioRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = User.builder()
                .email("test@test.com")
                .nickname("testuser")
                .provider("LOCAL")
                .build();
        testUser = userRepository.save(testUser);

        // 테스트 지갑 생성 (잔고: 10000 USD)
        testWallet = Wallet.builder()
                .user(testUser)
                .build();
        testWallet = walletRepository.save(testWallet);

        // StockService 모킹
        when(stockService.getQuote(anyString()))
                .thenReturn(StockQuoteResponse.builder()
                        .currentPrice(100.0)
                        .build());
    }

    @Test
    @DisplayName("동시 매수 주문 시 잔고 부족으로 일부만 성공해야 함")
    void testConcurrentBuyOrders() throws InterruptedException {
        // Given: 잔고가 1개 주문만 가능한 상황 (현재가 100, 수량 10 = 1000 필요)
        // 잔고: 10000, 각 주문: 1000 필요 → 최대 10개 주문 가능
        // 하지만 동시에 15개 주문을 보내면 일부만 성공해야 함
        
        int threadCount = 15;
        int orderQuantity = 10;
        BigDecimal currentPrice = new BigDecimal("100.00");
        
        // StockService 모킹 대신 실제 가격 사용 (테스트 환경에서는 실제 API 호출 안 함)
        // 실제로는 MockBean을 사용하거나 테스트 프로파일에서 StockService를 모킹해야 함
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // When: 동시에 15개 매수 주문 실행
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    TradeOrderRequest request = new TradeOrderRequest();
                    request.setTicker(testTicker);
                    request.setType(TradeLog.TradeType.BUY);
                    request.setQuantity(orderQuantity);

                    tradeService.executeOrder(testUser.getUserId(), request);
                    successCount.incrementAndGet();
                } catch (TradeException e) {
                    if (e.getErrorCode() == ErrorCode.TRADE_INSUFFICIENT_BALANCE) {
                        failureCount.incrementAndGet();
                    } else {
                        exceptions.add(e);
                    }
                } catch (Throwable e) {
                    if (e instanceof Exception) {
                        exceptions.add((Exception) e);
                    } else {
                        System.out.println("CRITICAL ERROR: " + e.getMessage());
                        e.printStackTrace();
                        exceptions.add(new RuntimeException(e));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Then: 성공한 주문 수와 실패한 주문 수의 합이 15개여야 함
        assertThat(successCount.get() + failureCount.get()).isEqualTo(threadCount);
        
        // 잔고 확인: 최대 10개 주문만 성공 가능 (10000 / 1000 = 10)
        Wallet wallet = walletRepository.findByUserUserId(testUser.getUserId()).orElseThrow();
        BigDecimal expectedBalance = new BigDecimal("10000.00")
                .subtract(new BigDecimal("1000.00").multiply(BigDecimal.valueOf(successCount.get())));
        
        // 허용 오차 범위 내에서 잔고 확인
        assertThat(wallet.getCashBalance().compareTo(expectedBalance)).isLessThanOrEqualTo(0);
        
        // 예외가 발생하지 않았어야 함 (락 타임아웃 등)
        assertThat(exceptions).isEmpty();
    }

    @Test
    @DisplayName("동시 매도 주문 시 보유 수량 부족으로 일부만 성공해야 함")
    void testConcurrentSellOrders() throws InterruptedException {
        // Given: 보유 수량이 10개인 상황에서 동시에 15개 매도 주문
        Portfolio portfolio = Portfolio.builder()
                .user(testUser)
                .ticker(testTicker)
                .quantity(10)
                .avgPrice(new BigDecimal("100.00"))
                .build();
        portfolioRepository.save(portfolio);

        int threadCount = 15;
        int orderQuantity = 1; // 각 주문당 1개씩 매도

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // When: 동시에 15개 매도 주문 실행
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    TradeOrderRequest request = new TradeOrderRequest();
                    request.setTicker(testTicker);
                    request.setType(TradeLog.TradeType.SELL);
                    request.setQuantity(orderQuantity);

                    tradeService.executeOrder(testUser.getUserId(), request);
                    successCount.incrementAndGet();
                } catch (TradeException e) {
                    if (e.getErrorCode() == ErrorCode.TRADE_INSUFFICIENT_QUANTITY) {
                        failureCount.incrementAndGet();
                    } else {
                        exceptions.add(e);
                    }
                } catch (Throwable e) {
                    if (e instanceof Exception) {
                        exceptions.add((Exception) e);
                    } else {
                        System.out.println("CRITICAL ERROR: " + e.getMessage());
                        e.printStackTrace();
                        exceptions.add(new RuntimeException(e));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Then: 최대 10개 주문만 성공 가능 (보유 수량이 10개)
        assertThat(successCount.get()).isLessThanOrEqualTo(10);
        assertThat(successCount.get() + failureCount.get()).isEqualTo(threadCount);
        
        // 예외가 발생하지 않았어야 함
        assertThat(exceptions).isEmpty();
    }
}
