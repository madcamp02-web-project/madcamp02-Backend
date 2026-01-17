package com.madcamp02.domain.trade;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TradeLogRepository extends JpaRepository<TradeLog, Long> {

    // 사용자의 전체 거래 내역 (최신순)
    List<TradeLog> findByUserUserIdOrderByTradeDateDesc(Long userId);

    // 사용자의 거래 내역 (페이징)
    Page<TradeLog> findByUserUserId(Long userId, Pageable pageable);

    // 특정 기간 거래 내역
    @Query("SELECT t FROM TradeLog t WHERE t.user.userId = :userId " +
            "AND t.tradeDate BETWEEN :startDate AND :endDate " +
            "ORDER BY t.tradeDate DESC")
    List<TradeLog> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // 특정 종목 거래 내역
    List<TradeLog> findByUserUserIdAndTickerOrderByTradeDateDesc(Long userId, String ticker);

    // 거래 유형별 내역 (매수/매도)
    List<TradeLog> findByUserUserIdAndTradeTypeOrderByTradeDateDesc(
            Long userId,
            TradeLog.TradeType tradeType
    );

    // 사용자의 총 실현 손익 합계
    @Query("SELECT COALESCE(SUM(t.realizedPnl), 0) FROM TradeLog t " +
            "WHERE t.user.userId = :userId AND t.realizedPnl IS NOT NULL")
    BigDecimal sumRealizedPnlByUserId(@Param("userId") Long userId);

    // 최근 N개 거래 내역
    @Query("SELECT t FROM TradeLog t WHERE t.user.userId = :userId " +
            "ORDER BY t.tradeDate DESC LIMIT :limit")
    List<TradeLog> findRecentTrades(
            @Param("userId") Long userId,
            @Param("limit") int limit
    );

    // 특정 종목의 전체 거래량 (통계용)
    @Query("SELECT SUM(t.quantity) FROM TradeLog t WHERE t.ticker = :ticker")
    Long sumQuantityByTicker(@Param("ticker") String ticker);
}