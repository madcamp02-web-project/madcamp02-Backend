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
import com.madcamp02.dto.response.TradeHistoryResponse;
import com.madcamp02.dto.response.TradeResponse;
import com.madcamp02.exception.ErrorCode;
import com.madcamp02.exception.TradeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TradeService 통합 테스트
 * Phase 4: Trade/Portfolio Engine
 * 
 * 전체 거래 흐름을 검증하는 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TradeServiceIntegrationTest {

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

    private User testUser;
    private Wallet testWallet;
    private String testTicker = "AAPL";

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
    }

    @Test
    @DisplayName("매수 주문 성공 시 잔고 차감 및 포트폴리오 생성")
    void testBuyOrderSuccess() {
        // Given
        TradeOrderRequest request = new TradeOrderRequest();
        request.setTicker(testTicker);
        request.setType(TradeLog.TradeType.BUY);
        request.setQuantity(10);

        BigDecimal initialBalance = testWallet.getCashBalance();

        // When
        TradeResponse response = tradeService.executeOrder(testUser.getUserId(), request);

        // Then
        assertThat(response.getTicker()).isEqualTo(testTicker);
        assertThat(response.getType()).isEqualTo(TradeLog.TradeType.BUY);
        assertThat(response.getQuantity()).isEqualTo(10);

        // 잔고 확인
        Wallet wallet = walletRepository.findByUserUserId(testUser.getUserId()).orElseThrow();
        assertThat(wallet.getCashBalance()).isLessThan(initialBalance);

        // 포트폴리오 확인
        Portfolio portfolio = portfolioRepository
                .findByUserUserIdAndTicker(testUser.getUserId(), testTicker)
                .orElseThrow();
        assertThat(portfolio.getQuantity()).isEqualTo(10);

        // 거래 내역 확인
        TradeLog tradeLog = tradeLogRepository.findById(response.getOrderId()).orElseThrow();
        assertThat(tradeLog.getTradeType()).isEqualTo(TradeLog.TradeType.BUY);
        assertThat(tradeLog.getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("매수 주문 시 잔고 부족하면 예외 발생")
    void testBuyOrderInsufficientBalance() {
        // Given: 잔고보다 큰 주문
        TradeOrderRequest request = new TradeOrderRequest();
        request.setTicker(testTicker);
        request.setType(TradeLog.TradeType.BUY);
        request.setQuantity(100000); // 매우 큰 수량

        // When & Then
        assertThatThrownBy(() -> tradeService.executeOrder(testUser.getUserId(), request))
                .isInstanceOf(TradeException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TRADE_INSUFFICIENT_BALANCE);
    }

    @Test
    @DisplayName("매도 주문 성공 시 실현 손익 계산 및 포트폴리오 업데이트")
    void testSellOrderSuccess() {
        // Given: 보유 종목 생성
        Portfolio portfolio = Portfolio.builder()
                .user(testUser)
                .ticker(testTicker)
                .quantity(10)
                .avgPrice(new BigDecimal("100.00")) // 평단가 100
                .build();
        portfolioRepository.save(portfolio);

        BigDecimal initialBalance = testWallet.getCashBalance();
        BigDecimal initialRealizedProfit = testWallet.getRealizedProfit();

        TradeOrderRequest request = new TradeOrderRequest();
        request.setTicker(testTicker);
        request.setType(TradeLog.TradeType.SELL);
        request.setQuantity(5);

        // When
        TradeResponse response = tradeService.executeOrder(testUser.getUserId(), request);

        // Then
        assertThat(response.getTicker()).isEqualTo(testTicker);
        assertThat(response.getType()).isEqualTo(TradeLog.TradeType.SELL);
        assertThat(response.getQuantity()).isEqualTo(5);

        // 잔고 증가 확인
        Wallet wallet = walletRepository.findByUserUserId(testUser.getUserId()).orElseThrow();
        assertThat(wallet.getCashBalance()).isGreaterThan(initialBalance);

        // 실현 손익 확인 (현재가가 평단가보다 높으면 수익)
        TradeLog tradeLog = tradeLogRepository.findById(response.getOrderId()).orElseThrow();
        assertThat(tradeLog.getRealizedPnl()).isNotNull();

        // 포트폴리오 수량 감소 확인
        Portfolio updatedPortfolio = portfolioRepository
                .findByUserUserIdAndTicker(testUser.getUserId(), testTicker)
                .orElseThrow();
        assertThat(updatedPortfolio.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("매도 주문 시 보유 수량 부족하면 예외 발생")
    void testSellOrderInsufficientQuantity() {
        // Given: 보유 종목 없음
        TradeOrderRequest request = new TradeOrderRequest();
        request.setTicker(testTicker);
        request.setType(TradeLog.TradeType.SELL);
        request.setQuantity(10);

        // When & Then
        assertThatThrownBy(() -> tradeService.executeOrder(testUser.getUserId(), request))
                .isInstanceOf(TradeException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TRADE_INSUFFICIENT_QUANTITY);
    }

    @Test
    @DisplayName("매도 주문으로 포트폴리오가 비어지면 삭제됨")
    void testSellOrderEmptyPortfolio() {
        // Given: 보유 종목 1개
        Portfolio portfolio = Portfolio.builder()
                .user(testUser)
                .ticker(testTicker)
                .quantity(1)
                .avgPrice(new BigDecimal("100.00"))
                .build();
        portfolioRepository.save(portfolio);

        TradeOrderRequest request = new TradeOrderRequest();
        request.setTicker(testTicker);
        request.setType(TradeLog.TradeType.SELL);
        request.setQuantity(1);

        // When
        tradeService.executeOrder(testUser.getUserId(), request);

        // Then: 포트폴리오가 삭제되어야 함
        boolean exists = portfolioRepository
                .findByUserUserIdAndTicker(testUser.getUserId(), testTicker)
                .isPresent();
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("거래 내역 조회 성공")
    void testGetTradeHistory() {
        // Given: 거래 내역 생성
        TradeLog tradeLog1 = TradeLog.builder()
                .user(testUser)
                .ticker(testTicker)
                .tradeType(TradeLog.TradeType.BUY)
                .price(new BigDecimal("100.00"))
                .quantity(10)
                .totalAmount(new BigDecimal("1000.00"))
                .build();
        tradeLogRepository.save(tradeLog1);

        TradeLog tradeLog2 = TradeLog.builder()
                .user(testUser)
                .ticker(testTicker)
                .tradeType(TradeLog.TradeType.SELL)
                .price(new BigDecimal("110.00"))
                .quantity(5)
                .totalAmount(new BigDecimal("550.00"))
                .realizedPnl(new BigDecimal("50.00"))
                .build();
        tradeLogRepository.save(tradeLog2);

        // When
        Pageable pageable = PageRequest.of(0, 20);
        TradeHistoryResponse response = tradeService.getTradeHistory(
                testUser.getUserId(),
                null,
                null,
                pageable
        );

        // Then
        assertThat(response.getItems()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(response.getAsOf()).isNotNull();
    }
}
