package com.madcamp02.service;

import com.madcamp02.domain.stock.StockCandle;
import com.madcamp02.domain.stock.StockCandleRepository;
import com.madcamp02.dto.response.StockCandlesResponse;
import com.madcamp02.dto.response.StockQuoteResponse;
import com.madcamp02.dto.response.StockSearchResponse;
import com.madcamp02.external.EodhdClient;
import com.madcamp02.external.FinnhubClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

        private final FinnhubClient finnhubClient;
        private final EodhdClient eodhdClient;
        private final StockCandleRepository stockCandleRepository;
        private final QuotaManager quotaManager;

        // ------------------------------------------
        // 종목 검색 (GET /api/v1/stock/search)
        // ------------------------------------------
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

        // ------------------------------------------
        // 현재가 조회 (GET /api/v1/stock/quote/{ticker})
        // ------------------------------------------
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

        // ------------------------------------------
        // 캔들 차트 데이터 조회 (GET /api/v1/stock/candles/{ticker})
        // ------------------------------------------
        // Phase 3.5 Data Strategy: EODHD + DB Caching + Quota Management
        // 1. DB 조회
        // 2. 데이터 없으면 Quota 체크 후 EODHD 호출
        // 3. DB 저장 및 반환
        // ------------------------------------------
        @Transactional
        public StockCandlesResponse getCandles(String ticker, String resolution, LocalDateTime from, LocalDateTime to) {
                log.debug("캔들 차트 데이터 조회 요청: ticker={}, from={}, to={}", ticker, from, to);

                // 1. 날짜 변환 (Time 무시, Date 기준)
                LocalDate fromDate = from.toLocalDate();
                LocalDate toDate = to.toLocalDate();

                // 2. DB 조회
                List<StockCandle> cachedCandles = stockCandleRepository
                                .findAllBySymbolAndDateBetweenOrderByDateAsc(ticker, fromDate, toDate);

                // 3. 데이터 전략 판단
                // 단순화 전략: DB에 데이터가 하나도 없으면 API 호출 시도
                // (향후 개선: 데이터가 있지만 비어있는 구간이 크거나, 최신 데이터가 누락된 경우도 포함 가능)
                if (cachedCandles.isEmpty()) {
                        if (quotaManager.checkQuota("EODHD")) {
                                try {
                                        // API 호출
                                        List<EodhdClient.EodhdCandle> eodhdCandles = eodhdClient
                                                        .getHistoricalData(ticker, fromDate, toDate);

                                        if (eodhdCandles != null && !eodhdCandles.isEmpty()) {
                                                List<StockCandle> newCandles = eodhdCandles.stream()
                                                                .filter(c -> c.getDate() != null)
                                                                .map(c -> StockCandle.builder()
                                                                                .symbol(ticker)
                                                                                .date(c.getDate())
                                                                                .open(BigDecimal.valueOf(
                                                                                                c.getOpen() != null ? c
                                                                                                                .getOpen()
                                                                                                                : 0.0))
                                                                                .high(BigDecimal.valueOf(
                                                                                                c.getHigh() != null ? c
                                                                                                                .getHigh()
                                                                                                                : 0.0))
                                                                                .low(BigDecimal.valueOf(
                                                                                                c.getLow() != null ? c
                                                                                                                .getLow()
                                                                                                                : 0.0))
                                                                                .close(BigDecimal.valueOf(
                                                                                                c.getClose() != null ? c
                                                                                                                .getClose()
                                                                                                                : 0.0))
                                                                                .volume(c.getVolume() != null
                                                                                                ? c.getVolume()
                                                                                                : 0L)
                                                                                .build())
                                                                .collect(Collectors.toList());

                                                if (!newCandles.isEmpty()) {
                                                        stockCandleRepository.saveAll(newCandles);
                                                        quotaManager.incrementUsage("EODHD");
                                                        cachedCandles = newCandles;
                                                        log.info("EODHD 데이터 적재 완료: ticker={}, count={}", ticker,
                                                                        newCandles.size());
                                                }
                                        }
                                } catch (Exception e) {
                                        log.error("EODHD API 처리 중 오류 발생 (무시하고 빈 응답 반환): {}", e.getMessage(), e);
                                        // 500 에러 방지: 예외를 던지지 않고 로그만 남김
                                }
                        } else {
                                log.warn("EODHD Quota 초과로 데이터 갱신 불가: ticker={}", ticker);
                                // Quota 초과 시: DB가 비어있으므로 그냥 빈 리스트 반환 (Case B)
                                // 만약 Stale Data라도 있으면 그걸 반환했겠지만, 여기는 isEmpty() 블록임.
                        }
                }

                // 4. Entity -> Response DTO 변환
                // 정렬 보장 (DB에서 이미 정렬했지만 안전하게 다시 정렬)
                cachedCandles.sort(Comparator.comparing(StockCandle::getDate));

                List<StockCandlesResponse.Candle> items = cachedCandles.stream()
                                .map(c -> StockCandlesResponse.Candle.builder()
                                                .timestamp(c.getDate().atStartOfDay().toEpochSecond(ZoneOffset.UTC))
                                                .open(c.getOpen().doubleValue())
                                                .high(c.getHigh().doubleValue())
                                                .low(c.getLow().doubleValue())
                                                .close(c.getClose().doubleValue())
                                                .volume(c.getVolume())
                                                .build())
                                .collect(Collectors.toList());

                return StockCandlesResponse.builder()
                                .ticker(ticker)
                                .resolution(resolution)
                                .items(items)
                                .build();
        }
}
