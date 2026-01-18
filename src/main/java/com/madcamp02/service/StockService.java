package com.madcamp02.service;

//======================================
// StockService - 주식 데이터 비즈니스 로직
//======================================
// Phase 3에서 구현하는 Stock API들의 "실제 일"을 담당하는 Service
//
// 구현 대상 엔드포인트:
// - GET /api/v1/stock/search
// - GET /api/v1/stock/quote/{ticker}
// - GET /api/v1/stock/candles/{ticker}
//======================================

import com.madcamp02.dto.response.StockCandlesResponse;
import com.madcamp02.dto.response.StockQuoteResponse;
import com.madcamp02.dto.response.StockSearchResponse;
import com.madcamp02.external.FinnhubClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final FinnhubClient finnhubClient;

    //------------------------------------------
    // 종목 검색 (GET /api/v1/stock/search)
    //------------------------------------------
    @Transactional(readOnly = true)
    public StockSearchResponse searchStock(String keyword) {
        log.debug("종목 검색 시작: keyword={}", keyword);

        FinnhubClient.SearchResponse searchResponse = finnhubClient.searchSymbol(keyword);

        List<StockSearchResponse.Item> items = searchResponse.getResult().stream()
                .limit(20) // 최대 20개만 반환
                .map(result -> StockSearchResponse.Item.builder()
                        .symbol(result.getSymbol())
                        .description(result.getDescription())
                        .displaySymbol(result.getDisplaySymbol())
                        .type(result.getType())
                        .build())
                .collect(Collectors.toList());

        return StockSearchResponse.builder()
                .items(items)
                .build();
    }

    //------------------------------------------
    // 현재가 조회 (GET /api/v1/stock/quote/{ticker})
    //------------------------------------------
    @Transactional(readOnly = true)
    public StockQuoteResponse getQuote(String ticker) {
        log.debug("현재가 조회 시작: ticker={}", ticker);

        FinnhubClient.QuoteResponse quote = finnhubClient.getQuote(ticker);

        double change = quote.getPreviousClose() != null && quote.getCurrentPrice() != null
                ? quote.getCurrentPrice() - quote.getPreviousClose()
                : 0.0;

        double changePercent = quote.getPreviousClose() != null && quote.getPreviousClose() != 0
                ? (change / quote.getPreviousClose()) * 100
                : 0.0;

        return StockQuoteResponse.builder()
                .ticker(ticker)
                .currentPrice(quote.getCurrentPrice())
                .open(quote.getOpen())
                .high(quote.getHigh())
                .low(quote.getLow())
                .previousClose(quote.getPreviousClose())
                .change(change)
                .changePercent(changePercent)
                .timestamp(quote.getTimestamp())
                .build();
    }

    //------------------------------------------
    // 캔들 차트 데이터 조회 (GET /api/v1/stock/candles/{ticker})
    //------------------------------------------
    // 주의: Finnhub의 Candles (OHLCV) API는 Premium이므로 사용 불가
    // 현재는 구현하지 않음. 향후 대체 API(Alpha Vantage 등) 또는 Phase 6 WebSocket으로 실시간 데이터 제공 예정
    //------------------------------------------
    @Transactional(readOnly = true)
    public StockCandlesResponse getCandles(String ticker, String resolution, LocalDateTime from, LocalDateTime to) {
        log.warn("캔들 차트 데이터 조회 요청: ticker={}, resolution={}, from={}, to={} - Premium API이므로 미구현", 
                ticker, resolution, from, to);
        
        // Premium API이므로 빈 응답 반환
        // 향후 대체 API 연동 또는 WebSocket 실시간 데이터로 대체 예정
        return StockCandlesResponse.builder()
                .ticker(ticker)
                .resolution(resolution)
                .items(List.of())
                .build();
    }
}
