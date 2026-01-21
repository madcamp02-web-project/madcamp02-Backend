package com.madcamp02.service;

import com.madcamp02.domain.portfolio.Portfolio;
import com.madcamp02.domain.portfolio.PortfolioRepository;
import com.madcamp02.domain.wallet.Wallet;
import com.madcamp02.domain.wallet.WalletRepository;
import com.madcamp02.dto.response.PortfolioResponse;
import com.madcamp02.dto.response.StockQuoteResponse;
import com.madcamp02.exception.BusinessException;
import com.madcamp02.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 포트폴리오 관련 비즈니스 로직 서비스
 * Phase 4: Trade/Portfolio Engine
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

        private final WalletRepository walletRepository;
        private final PortfolioRepository portfolioRepository;
        private final StockService stockService;

        /**
         * 포트폴리오 조회 및 평가
         * GET /api/v1/trade/portfolio
         */
        /**
         * 포트폴리오 조회 및 평가
         * GET /api/v1/trade/portfolio
         */
        @Transactional
        public PortfolioResponse getPortfolio(Long userId) {
                log.debug("포트폴리오 조회: userId={}", userId);

                // 1. Wallet 조회
                Wallet wallet = walletRepository.findByUserUserId(userId)
                                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

                // 2. Portfolio 조회
                List<Portfolio> portfolios = portfolioRepository.findByUserUserId(userId);

                // 3. 현재가 조회 및 평가
                List<PortfolioResponse.Position> positions = new ArrayList<>();
                BigDecimal totalMarketValue = BigDecimal.ZERO;

                for (Portfolio portfolio : portfolios) {
                        try {
                                StockQuoteResponse quote = stockService.getQuote(portfolio.getTicker());
                                BigDecimal currentPrice = BigDecimal.valueOf(quote.getCurrentPrice());
                                BigDecimal marketValue = currentPrice
                                                .multiply(BigDecimal.valueOf(portfolio.getQuantity()));
                                BigDecimal pnl = currentPrice.subtract(portfolio.getAvgPrice())
                                                .multiply(BigDecimal.valueOf(portfolio.getQuantity()));
                                BigDecimal pnlPercent = portfolio.getAvgPrice().compareTo(BigDecimal.ZERO) > 0
                                                ? pnl.divide(portfolio.getAvgPrice()
                                                                .multiply(BigDecimal.valueOf(portfolio.getQuantity())),
                                                                4, RoundingMode.HALF_UP)
                                                                .multiply(BigDecimal.valueOf(100))
                                                : BigDecimal.ZERO;

                                totalMarketValue = totalMarketValue.add(marketValue);

                                positions.add(PortfolioResponse.Position.builder()
                                                .ticker(portfolio.getTicker())
                                                .quantity((long) portfolio.getQuantity())
                                                .avgPrice(portfolio.getAvgPrice().doubleValue())
                                                .currentPrice(currentPrice.doubleValue())
                                                .marketValue(marketValue.doubleValue())
                                                .pnl(pnl.doubleValue())
                                                .pnlPercent(pnlPercent.doubleValue())
                                                .build());
                        } catch (Exception e) {
                                log.warn("종목 현재가 조회 실패: ticker={}, error={}",
                                                portfolio.getTicker(), e.getMessage());
                                // 현재가 조회 실패 시에도 기본 정보는 포함
                                BigDecimal marketValue = portfolio.getAvgPrice()
                                                .multiply(BigDecimal.valueOf(portfolio.getQuantity()));
                                positions.add(PortfolioResponse.Position.builder()
                                                .ticker(portfolio.getTicker())
                                                .quantity((long) portfolio.getQuantity())
                                                .avgPrice(portfolio.getAvgPrice().doubleValue())
                                                .currentPrice(portfolio.getAvgPrice().doubleValue())
                                                .marketValue(marketValue.doubleValue())
                                                .pnl(0.0)
                                                .pnlPercent(0.0)
                                                .build());
                                totalMarketValue = totalMarketValue.add(marketValue);
                        }
                }

                // 4. Summary 계산
                BigDecimal totalEquity = wallet.getCashBalance().add(totalMarketValue);
                BigDecimal totalPnl = totalEquity.subtract(wallet.getTotalAssets()); // 주의: 기존 자산 기준이 아님. 총 자산 - (예수금 +
                                                                                     // 투자원금) 이어야 정확한 PnL이지만, 요구사항상
                                                                                     // totalEquity - totalInvested 를
                                                                                     // PnL로 잡아야 함.
                // 하지만 여기서 wallet.getTotalAssets()는 "이전" 값임.
                // PnL = (Current Valuation + Cash) - Original Principal.
                // Original Principal 추적이 어려움.
                // 그러나 일반적으로 PnL = Sum(Position PnL).

                // 정확한 Total PnL 계산: 각 포지션의 PnL 합계
                BigDecimal sumPositionPnl = positions.stream()
                                .map(p -> BigDecimal.valueOf(p.getPnl()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Total PnL Percent = (Total PnL / Total Invested) * 100
                BigDecimal totalInvested = positions.stream()
                                .map(p -> BigDecimal.valueOf(p.getAvgPrice())
                                                .multiply(BigDecimal.valueOf(p.getQuantity())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalPnlPercent = totalInvested.compareTo(BigDecimal.ZERO) > 0
                                ? sumPositionPnl.divide(totalInvested, 4, RoundingMode.HALF_UP)
                                                .multiply(BigDecimal.valueOf(100))
                                : BigDecimal.ZERO;

                // 5. Wallet TotalAssets 업데이트 (Lazy Update)
                // 사용자가 포트폴리오를 조회할 때마다 최신 평가액으로 지갑 정보를 갱신하여
                // 랭킹 및 마이페이지에 반영되도록 함.
                wallet.updateTotalAssets(totalEquity);
                walletRepository.save(wallet);

                return PortfolioResponse.builder()
                                .asOf(LocalDateTime.now().toString())
                                .summary(PortfolioResponse.Summary.builder()
                                                .totalEquity(totalEquity.doubleValue())
                                                .cashBalance(wallet.getCashBalance().doubleValue())
                                                .totalPnl(sumPositionPnl.doubleValue())
                                                .totalPnlPercent(totalPnlPercent.doubleValue())
                                                .currency("USD")
                                                .build())
                                .positions(positions)
                                .build();
        }
}
