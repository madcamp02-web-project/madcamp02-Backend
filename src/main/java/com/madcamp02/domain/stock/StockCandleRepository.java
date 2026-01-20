package com.madcamp02.domain.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockCandleRepository extends JpaRepository<StockCandle, StockCandleId> {

    // 특정 종목의 모든 캔들 조회 (날짜 내림차순)
    List<StockCandle> findAllBySymbolOrderByDateDesc(String symbol);

    // 날짜 범위 조회 (기존 메서드, 호환성 유지)
    List<StockCandle> findAllBySymbolAndDateBetweenOrderByDateAsc(String symbol, LocalDate startDate,
            LocalDate endDate);

    // period 조건 추가된 조회 메서드들
    List<StockCandle> findAllBySymbolAndPeriodAndDateBetweenOrderByDateAsc(
            String symbol, String period, LocalDate startDate, LocalDate endDate);

    // 배치 로드 판단용: 특정 종목의 특정 period 데이터 존재 여부 확인
    boolean existsBySymbolAndPeriod(String symbol, String period);
}
