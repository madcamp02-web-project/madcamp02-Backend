package com.madcamp02.domain.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockCandleRepository extends JpaRepository<StockCandle, StockCandleId> {

    // 특정 종목의 모든 캔들 조회 (날짜 내림차순)
    List<StockCandle> findAllBySymbolOrderByDateDesc(String symbol);

    // 날짜 범위 조회
    List<StockCandle> findAllBySymbolAndDateBetweenOrderByDateAsc(String symbol, LocalDate startDate,
            LocalDate endDate);
}
