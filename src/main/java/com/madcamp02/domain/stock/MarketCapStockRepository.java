package com.madcamp02.domain.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Market Movers를 위한 Top 20 Market Cap 종목 Repository
 */
@Repository
public interface MarketCapStockRepository extends JpaRepository<MarketCapStock, Long> {
    
    /**
     * 활성화된 종목을 시가총액 순위 순으로 조회
     * @return 활성화된 종목 리스트 (순위 오름차순)
     */
    List<MarketCapStock> findByIsActiveTrueOrderByMarketCapRankAsc();
    
    /**
     * 심볼로 종목 조회
     * @param symbol 종목 심볼
     * @return 종목 정보 (Optional)
     */
    Optional<MarketCapStock> findBySymbol(String symbol);
}
