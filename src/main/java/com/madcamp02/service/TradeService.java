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
import com.madcamp02.dto.response.TradeHistoryResponse;
import com.madcamp02.dto.response.TradeNotificationDto;
import com.madcamp02.dto.response.TradeResponse;
import com.madcamp02.exception.BusinessException;
import com.madcamp02.exception.ErrorCode;
import com.madcamp02.exception.TradeException;
import com.madcamp02.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 거래 관련 비즈니스 로직 서비스
 * Phase 4: Trade/Portfolio Engine
 * 
 * 트랜잭션 및 비관적 락을 사용하여 동시성 문제를 해결합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeService {

    private final WalletRepository walletRepository;
    private final PortfolioRepository portfolioRepository;
    private final TradeLogRepository tradeLogRepository;
    private final UserRepository userRepository;
    private final StockService stockService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    @Lazy
    private TradeService self;

    /**
     * 거래 주문 실행
     * POST /api/v1/trade/order
     * 
     * 비관적 락을 사용하여 동시성 문제를 방지합니다.
     * 외부 API 호출은 트랜잭션 외부에서 수행하여 트랜잭션 유지 시간을 최소화합니다.
     */
    public TradeResponse executeOrder(Long userId, TradeOrderRequest request) {
        log.debug("거래 주문 실행: userId={}, ticker={}, type={}, quantity={}",
                userId, request.getTicker(), request.getType(), request.getQuantity());

        // 1. 현재가 조회 (외부 API, 트랜잭션 외부에서 호출)
        // 외부 API 지연 시 트랜잭션 유지 시간을 최소화하기 위해 트랜잭션 전에 호출
        StockQuoteResponse quote = stockService.getQuote(request.getTicker());
        BigDecimal currentPrice = BigDecimal.valueOf(quote.getCurrentPrice());

        // 2. 트랜잭션 내부에서 거래 실행
        // self-invocation 문제 해결을 위해 자기 자신을 주입받아 호출
        return self.executeOrderInTransaction(userId, request, currentPrice);
    }

    /**
     * 트랜잭션 내부에서 거래 주문 실행
     * 비관적 락을 사용하여 동시성 문제를 방지합니다.
     * public으로 변경하여 프록시 호출이 가능하도록 함
     */
    @Transactional
    public TradeResponse executeOrderInTransaction(
            Long userId,
            TradeOrderRequest request,
            BigDecimal currentPrice
    ) {
        // 1. Wallet 조회 (비관적 락)
        Wallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. 거래 타입별 분기
        if (request.getType() == TradeLog.TradeType.BUY) {
            return executeBuyOrder(userId, request, wallet, currentPrice);
        } else {
            return executeSellOrder(userId, request, wallet, currentPrice);
        }
    }

    /**
     * 매수 주문 실행
     * 비관적 락을 사용하여 동시성 문제를 방지합니다.
     */
    private TradeResponse executeBuyOrder(
            Long userId,
            TradeOrderRequest request,
            Wallet wallet,
            BigDecimal currentPrice
    ) {
        log.debug("매수 주문 실행: userId={}, ticker={}, quantity={}, price={}",
                userId, request.getTicker(), request.getQuantity(), currentPrice);

        // 1. 총 필요 금액 계산
        BigDecimal totalAmount = currentPrice
                .multiply(BigDecimal.valueOf(request.getQuantity()));

        // 2. 잔고 확인
        if (wallet.getCashBalance().compareTo(totalAmount) < 0) {
            throw new TradeException(ErrorCode.TRADE_INSUFFICIENT_BALANCE);
        }

        // 3. Portfolio 조회 또는 생성 (비관적 락)
        Portfolio portfolio = portfolioRepository
                .findByUserIdAndTickerWithLock(userId, request.getTicker())
                .orElse(null);

        if (portfolio == null) {
            // 신규 보유 종목 생성
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
            portfolio = Portfolio.builder()
                    .user(user)
                    .ticker(request.getTicker())
                    .quantity(request.getQuantity())
                    .avgPrice(currentPrice)
                    .build();
        } else {
            // 기존 보유 종목 평단가 재계산
            portfolio.addQuantity(request.getQuantity(), currentPrice);
        }

        // 4. Wallet 차감
        wallet.deductCash(totalAmount);

        // 5. TradeLog 저장
        TradeLog tradeLog = TradeLog.builder()
                .user(portfolio.getUser())
                .ticker(request.getTicker())
                .tradeType(TradeLog.TradeType.BUY)
                .price(currentPrice)
                .quantity(request.getQuantity())
                .totalAmount(totalAmount)
                .fee(BigDecimal.ZERO) // 수수료는 향후 추가
                .realizedPnl(null) // 매수 시 실현 손익 없음
                .build();

        tradeLogRepository.save(tradeLog);
        portfolioRepository.save(portfolio);
        walletRepository.save(wallet);

        log.info("매수 주문 완료: userId={}, ticker={}, quantity={}, totalAmount={}",
                userId, request.getTicker(), request.getQuantity(), totalAmount);

        // 6. STOMP 브로드캐스트 (체결 알림)
        broadcastTradeNotification(userId, tradeLog, currentPrice, totalAmount, null);

        // 7. 응답 생성
        return TradeResponse.builder()
                .orderId(tradeLog.getLogId())
                .ticker(request.getTicker())
                .type(TradeLog.TradeType.BUY)
                .quantity(request.getQuantity())
                .executedPrice(currentPrice.doubleValue())
                .totalAmount(totalAmount.doubleValue())
                .executedAt(tradeLog.getTradeDate())
                .build();
    }

    /**
     * 매도 주문 실행
     * 비관적 락을 사용하여 동시성 문제를 방지합니다.
     */
    private TradeResponse executeSellOrder(
            Long userId,
            TradeOrderRequest request,
            Wallet wallet,
            BigDecimal currentPrice
    ) {
        log.debug("매도 주문 실행: userId={}, ticker={}, quantity={}, price={}",
                userId, request.getTicker(), request.getQuantity(), currentPrice);

        // 1. Portfolio 조회 (비관적 락)
        Portfolio portfolio = portfolioRepository
                .findByUserIdAndTickerWithLock(userId, request.getTicker())
                .orElseThrow(() -> new TradeException(
                        ErrorCode.TRADE_INSUFFICIENT_QUANTITY,
                        "보유 종목이 없습니다."
                ));

        // 2. 보유 수량 확인
        if (portfolio.getQuantity() < request.getQuantity()) {
            throw new TradeException(ErrorCode.TRADE_INSUFFICIENT_QUANTITY);
        }

        // 3. 실현 손익 계산
        BigDecimal avgPrice = portfolio.getAvgPrice();
        BigDecimal pnlPerShare = currentPrice.subtract(avgPrice);
        BigDecimal realizedPnl = pnlPerShare
                .multiply(BigDecimal.valueOf(request.getQuantity()));

        // 4. 총 매도 금액 계산
        BigDecimal totalAmount = currentPrice
                .multiply(BigDecimal.valueOf(request.getQuantity()));

        // 5. Portfolio 수량 차감
        portfolio.subtractQuantity(request.getQuantity());

        // 6. Wallet 업데이트
        wallet.addCash(totalAmount);
        wallet.addRealizedProfit(realizedPnl);

        // 7. TradeLog 저장
        TradeLog tradeLog = TradeLog.builder()
                .user(portfolio.getUser())
                .ticker(request.getTicker())
                .tradeType(TradeLog.TradeType.SELL)
                .price(currentPrice)
                .quantity(request.getQuantity())
                .totalAmount(totalAmount)
                .fee(BigDecimal.ZERO)
                .realizedPnl(realizedPnl)
                .build();

        tradeLogRepository.save(tradeLog);

        // 8. Portfolio가 비어있으면 삭제
        if (portfolio.isEmpty()) {
            portfolioRepository.delete(portfolio);
        } else {
            portfolioRepository.save(portfolio);
        }

        walletRepository.save(wallet);

        log.info("매도 주문 완료: userId={}, ticker={}, quantity={}, totalAmount={}, realizedPnl={}",
                userId, request.getTicker(), request.getQuantity(), totalAmount, realizedPnl);

        // 9. STOMP 브로드캐스트 (체결 알림)
        broadcastTradeNotification(userId, tradeLog, currentPrice, totalAmount, realizedPnl);

        // 10. 응답 생성
        return TradeResponse.builder()
                .orderId(tradeLog.getLogId())
                .ticker(request.getTicker())
                .type(TradeLog.TradeType.SELL)
                .quantity(request.getQuantity())
                .executedPrice(currentPrice.doubleValue())
                .totalAmount(totalAmount.doubleValue())
                .executedAt(tradeLog.getTradeDate())
                .build();
    }

    /**
     * 거래 내역 조회
     * GET /api/v1/trade/history
     */
    @Transactional(readOnly = true)
    public TradeHistoryResponse getTradeHistory(
            Long userId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        log.debug("거래 내역 조회: userId={}, startDate={}, endDate={}",
                userId, startDate, endDate);

        List<TradeLog> tradeLogs;

        if (startDate != null && endDate != null) {
            tradeLogs = tradeLogRepository.findByUserIdAndDateRange(
                    userId, startDate, endDate);
        } else {
            Page<TradeLog> page = tradeLogRepository.findByUserUserId(userId, pageable);
            tradeLogs = page.getContent();
        }

        List<TradeHistoryResponse.Item> items = tradeLogs.stream()
                .map(log -> TradeHistoryResponse.Item.builder()
                        .logId(log.getLogId())
                        .ticker(log.getTicker())
                        .type(log.getTradeType())
                        .quantity(log.getQuantity())
                        .price(log.getPrice().doubleValue())
                        .totalAmount(log.getTotalAmount().doubleValue())
                        .realizedPnl(log.getRealizedPnl() != null
                                ? log.getRealizedPnl().doubleValue()
                                : null)
                        .tradeDate(log.getTradeDate())
                        .build())
                .collect(Collectors.toList());

        return TradeHistoryResponse.builder()
                .asOf(LocalDateTime.now().toString())
                .items(items)
                .build();
    }

    /**
     * 거래 체결 알림 STOMP 브로드캐스트
     * 
     * 트랜잭션 커밋 후 `/user/queue/trade` 토픽으로 사용자에게 체결 알림을 전송합니다.
     * 
     * @param userId 사용자 ID
     * @param tradeLog 거래 로그
     * @param executedPrice 체결 가격
     * @param totalAmount 총 거래 금액
     * @param realizedPnl 실현 손익 (매도 시만, 매수 시는 null)
     */
    private void broadcastTradeNotification(
            Long userId,
            TradeLog tradeLog,
            BigDecimal executedPrice,
            BigDecimal totalAmount,
            BigDecimal realizedPnl
    ) {
        try {
            TradeNotificationDto notification = TradeNotificationDto.builder()
                    .orderId(tradeLog.getLogId())
                    .ticker(tradeLog.getTicker())
                    .type(tradeLog.getTradeType().name()) // "BUY" | "SELL"
                    .quantity(tradeLog.getQuantity())
                    .executedPrice(executedPrice.doubleValue())
                    .totalAmount(totalAmount.doubleValue())
                    .realizedPnl(realizedPnl != null ? realizedPnl.doubleValue() : null)
                    .executedAt(tradeLog.getTradeDate().toString()) // ISO-8601
                    .status("FILLED") // 향후 확장: PARTIALLY_FILLED 등
                    .build();

            // 사용자별 큐로 전송: /user/{userId}/queue/trade
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/trade",
                    notification
            );

            log.debug("거래 체결 알림 발행: userId={}, ticker={}, type={}",
                    userId, tradeLog.getTicker(), tradeLog.getTradeType());
        } catch (Exception e) {
            log.error("거래 체결 알림 발행 실패: userId={}, ticker={}",
                    userId, tradeLog.getTicker(), e);
            // STOMP 발행 실패해도 REST 응답은 정상 반환
        }
    }
}
