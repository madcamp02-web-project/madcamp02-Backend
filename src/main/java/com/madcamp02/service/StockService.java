package com.madcamp02.service;

import com.madcamp02.domain.stock.StockCandle;
import com.madcamp02.domain.stock.StockCandleRepository;
import com.madcamp02.dto.response.StockCandlesResponse;
import com.madcamp02.dto.response.StockQuoteResponse;
import com.madcamp02.dto.response.StockSearchResponse;
import com.madcamp02.exception.BusinessException;
import com.madcamp02.exception.ErrorCode;
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

                // Finnhub API가 제공하는 d(변동액)와 dp(변동률)를 우선 사용
                // 없으면 계산
                Double change = quote.getChange();
                Double changePercent = quote.getChangePercent();
                
                if (change == null && quote.getCurrentPrice() != null && quote.getPreviousClose() != null) {
                        change = quote.getCurrentPrice() - quote.getPreviousClose();
                }
                if (changePercent == null && quote.getPreviousClose() != null && quote.getPreviousClose() != 0 && change != null) {
                        changePercent = (change / quote.getPreviousClose()) * 100;
                }

                return StockQuoteResponse.builder()
                                .ticker(ticker)
                                .currentPrice(quote.getCurrentPrice())
                                .open(quote.getOpen())
                                .high(quote.getHigh())
                                .low(quote.getLow())
                                .previousClose(quote.getPreviousClose())
                                .change(change != null ? change : 0.0)
                                .changePercent(changePercent != null ? changePercent : 0.0)
                                .build();
        }

        // ------------------------------------------
        // 캔들 차트 데이터 조회 (GET /api/v1/stock/candles/{ticker})
        // ------------------------------------------
        // Phase 3.5 Data Strategy: EODHD + DB Caching + Quota Management
        // Step 1: DB 조회
        // Step 2: 데이터 최신성 체크 (오늘 장 종료 후 오늘 데이터 존재 여부)
        // Step 3: Quota 체크 → EODHD 호출 또는 기존 데이터 반환
        // Step 4: Quota 초과 시 Case A(기존 데이터 반환 + Stale 표시) 또는 Case B(429 에러)
        // ------------------------------------------
        @Transactional
        public StockCandlesResponse getCandles(String ticker, String resolution, LocalDateTime from, LocalDateTime to) {
                log.debug("캔들 차트 데이터 조회 요청: ticker={}, from={}, to={}", ticker, from, to);

                // Step 1: 날짜 변환 및 resolution → period 매핑
                LocalDate fromDate = from.toLocalDate();
                LocalDate toDate = to.toLocalDate();
                LocalDate today = LocalDate.now();

                // resolution을 EODHD API의 period로 변환
                // resolution: 1, 5, 15, 30, 60, D, W, M
                // period: d (daily), w (weekly), m (monthly)
                String period = "d"; // 기본값: 일간
                if (resolution != null) {
                        String upperResolution = resolution.toUpperCase();
                        if ("W".equals(upperResolution)) {
                                period = "w"; // 주간
                        } else if ("M".equals(upperResolution)) {
                                period = "m"; // 월간
                        } else {
                                // 1, 5, 15, 30, 60, D는 모두 일간 데이터로 처리
                                period = "d";
                        }
                }
                String order = "a"; // 오름차순 (기본값)

                // Step 2: DB 조회
                List<StockCandle> cachedCandles = stockCandleRepository
                                .findAllBySymbolAndDateBetweenOrderByDateAsc(ticker, fromDate, toDate);

                // Step 3: 데이터 최신성 체크 (오늘 장 종료 후 오늘 데이터 존재 여부)
                // 미국 주식 시장은 보통 9:30 AM - 4:00 PM ET (동부시간)
                // 한국시간으로는 약 22:30 - 05:00 (다음날)이지만, 간단하게 "오늘 날짜의 데이터가 있는지"만 체크
                boolean needsRefresh = false;
                if (!cachedCandles.isEmpty()) {
                        // 요청 범위에 오늘 날짜가 포함되어 있고, 오늘 데이터가 없으면 갱신 필요
                        if (toDate.isAfter(today.minusDays(1)) && 
                            cachedCandles.stream().noneMatch(c -> c.getDate().equals(today))) {
                                needsRefresh = true;
                                log.debug("오늘 데이터가 없어 갱신 필요: ticker={}, latestDate={}", ticker,
                                                cachedCandles.isEmpty() ? null : cachedCandles.get(cachedCandles.size() - 1).getDate());
                        }
                } else {
                        // DB에 데이터가 없으면 갱신 필요
                        needsRefresh = true;
                }

                // Step 4: Quota 체크 및 EODHD 호출
                boolean isStale = false;
                if (needsRefresh) {
                        if (quotaManager.checkQuota("EODHD")) {
                                try {
                                        // API 호출 (period, order 포함)
                                        List<EodhdClient.EodhdCandle> eodhdCandles = eodhdClient
                                                        .getHistoricalData(ticker, fromDate, toDate, period, order);

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
                                        log.error("EODHD API 처리 중 오류 발생: {}", e.getMessage(), e);
                                        // API 호출 실패 시 기존 데이터가 있으면 그것을 반환 (Stale 표시)
                                        if (cachedCandles.isEmpty()) {
                                                // 기존 데이터도 없으면 예외를 던지지 않고 빈 리스트 반환
                                                log.warn("EODHD API 실패 및 기존 데이터 없음: ticker={}", ticker);
                                        } else {
                                                isStale = true;
                                                log.warn("EODHD API 실패, 기존 데이터 반환 (Stale): ticker={}", ticker);
                                        }
                                }
                        } else {
                                // Quota 초과 시 Case A 또는 Case B
                                if (cachedCandles.isEmpty()) {
                                        // Case B: 기존 데이터 없음 → 429 에러
                                        log.warn("EODHD Quota 초과 및 기존 데이터 없음: ticker={}", ticker);
                                        throw new BusinessException(ErrorCode.QUOTA_EXCEEDED);
                                } else {
                                        // Case A: 기존 데이터 반환 + Stale 표시
                                        isStale = true;
                                        log.warn("EODHD Quota 초과, 기존 데이터 반환 (Stale): ticker={}", ticker);
                                }
                        }
                }

                // Step 5: Entity -> Response DTO 변환
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
                                .stale(isStale)
                                .build();
        }
}
