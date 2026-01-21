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
import java.util.ArrayList;
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
                if (changePercent == null && quote.getPreviousClose() != null && quote.getPreviousClose() != 0
                                && change != null) {
                        changePercent = (change / quote.getPreviousClose()) * 100;
                }

                // 거래량 조회 (로컬 DB에서 - EODHD API 일일 한도 초과 대응)
                Long volume = 0L;
                try {
                        // DB에서 해당 티커의 가장 최근 일봉 데이터 조회
                        List<StockCandle> recentCandles = stockCandleRepository
                                        .findBySymbolAndPeriodOrderByDateDesc(ticker, "d");
                        if (recentCandles != null && !recentCandles.isEmpty()) {
                                StockCandle latestCandle = recentCandles.get(0);
                                volume = latestCandle.getVolume() != null ? latestCandle.getVolume() : 0L;
                                log.debug("Quote 거래량 조회 성공 (DB): ticker={}, volume={}, date={}", ticker, volume,
                                                latestCandle.getDate());
                        } else {
                                log.debug("Quote 거래량 조회: DB에 캔들 데이터 없음 (ticker={})", ticker);
                        }
                } catch (Exception e) {
                        log.debug("Quote 거래량 조회 실패 (무시): ticker={}, error={}", ticker, e.getMessage());
                        // 거래량 조회 실패해도 계속 진행 (0으로 유지)
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
                                .volume(volume)
                                .build();
        }

        // ------------------------------------------
        // Helper 메서드: resolution을 period로 변환
        // ------------------------------------------
        /**
         * resolution 문자열을 period로 변환
         * 
         * @param resolution resolution 문자열 (d, w, m)
         * @return period 문자열 (d, w, m)
         */
        private String determinePeriod(String resolution) {
                if (resolution != null && !resolution.isEmpty()) {
                        String lowerResolution = resolution.toLowerCase();
                        if ("w".equals(lowerResolution)) {
                                return "w"; // 주간
                        } else if ("m".equals(lowerResolution)) {
                                return "m"; // 월간
                        } else if ("d".equals(lowerResolution)) {
                                return "d"; // 일간
                        } else {
                                // 알 수 없는 값은 기본값(일간) 사용
                                log.warn("알 수 없는 resolution 값: {}. 기본값(d) 사용", resolution);
                                return "d";
                        }
                }
                return "d"; // 기본값: 일간
        }

        // ------------------------------------------
        // 배치 로드: d, w, m 모든 resolution을 한번에 가져오기
        // ------------------------------------------
        /**
         * DB에 해당 종목의 데이터가 없을 때 d, w, m 모든 resolution을 한번에 가져와서 저장
         * API 호출은 1회만 카운트 (3개 resolution을 한번에 처리)
         * 
         * @param ticker   종목 심볼
         * @param fromDate 시작 날짜
         * @param toDate   종료 날짜
         * @return 배치 로드 성공 여부
         */
        private boolean batchLoadAllResolutions(String ticker, LocalDate fromDate, LocalDate toDate) {
                // DB에 해당 종목의 d 데이터가 하나도 없을 때만 실행
                if (stockCandleRepository.existsBySymbolAndPeriod(ticker, "d")) {
                        log.debug("배치 로드 스킵: {} 종목의 데이터가 이미 존재함", ticker);
                        return false;
                }

                log.info("배치 로드 시작: {} 종목의 d, w, m 모든 resolution 데이터 가져오기", ticker);

                String order = "a"; // 오름차순
                String[] periods = { "d", "w", "m" };
                int successCount = 0;

                // Quota 체크 (배치 로드 전에 한번만 체크)
                if (!quotaManager.checkQuota("EODHD")) {
                        log.warn("배치 로드 실패: EODHD Quota 초과");
                        return false;
                }

                // 각 period별로 순차 호출 및 저장
                for (String period : periods) {
                        try {
                                List<EodhdClient.EodhdCandle> eodhdCandles = eodhdClient
                                                .getHistoricalData(ticker, fromDate, toDate, period, order);

                                if (eodhdCandles != null && !eodhdCandles.isEmpty()) {
                                        List<StockCandle> newCandles = eodhdCandles.stream()
                                                        .filter(c -> c.getDate() != null)
                                                        .map(c -> StockCandle.builder()
                                                                        .symbol(ticker)
                                                                        .date(c.getDate())
                                                                        .period(period)
                                                                        .open(BigDecimal.valueOf(
                                                                                        c.getOpen() != null
                                                                                                        ? c.getOpen()
                                                                                                        : 0.0))
                                                                        .high(BigDecimal.valueOf(
                                                                                        c.getHigh() != null
                                                                                                        ? c.getHigh()
                                                                                                        : 0.0))
                                                                        .low(BigDecimal.valueOf(
                                                                                        c.getLow() != null ? c.getLow()
                                                                                                        : 0.0))
                                                                        .close(BigDecimal.valueOf(
                                                                                        c.getClose() != null
                                                                                                        ? c.getClose()
                                                                                                        : 0.0))
                                                                        .volume(c.getVolume() != null ? c.getVolume()
                                                                                        : 0L)
                                                                        .build())
                                                        .collect(Collectors.toList());

                                        if (!newCandles.isEmpty()) {
                                                stockCandleRepository.saveAll(newCandles);
                                                successCount++;
                                                log.info("배치 로드 성공: {} period={}, count={}", ticker, period,
                                                                newCandles.size());
                                        }
                                } else {
                                        log.warn("배치 로드: {} period={} 데이터 없음", ticker, period);
                                }
                        } catch (Exception e) {
                                log.error("배치 로드 실패: {} period={}, error={}", ticker, period, e.getMessage(), e);
                        }
                }

                // 배치 로드가 하나라도 성공했으면 Quota 1회만 카운트
                if (successCount > 0) {
                        quotaManager.incrementUsage("EODHD");
                        log.info("배치 로드 완료: {} 종목, 성공한 resolution={}/3, Quota 1회 카운트", ticker, successCount);
                } else {
                        log.warn("배치 로드 실패: {} 종목의 모든 resolution 실패", ticker);
                }

                return successCount > 0;
        }

        /**
         * 부분 배치 로드: d는 있지만 w, m 중 없는 resolution들을 한번에 가져오기
         * API 호출은 1회만 카운트 (여러 resolution을 한번에 처리)
         * 
         * @param ticker   종목 심볼
         * @param fromDate 시작 날짜
         * @param toDate   종료 날짜
         * @return 배치 로드 성공 여부
         */
        private boolean batchLoadMissingResolutions(String ticker, LocalDate fromDate, LocalDate toDate) {
                // d는 있어야 하고, w 또는 m이 없어야 함
                if (!stockCandleRepository.existsBySymbolAndPeriod(ticker, "d")) {
                        log.debug("부분 배치 로드 스킵: {} 종목의 d 데이터가 없음 (전체 배치 로드 필요)", ticker);
                        return false;
                }

                // 없는 resolution 체크
                boolean hasW = stockCandleRepository.existsBySymbolAndPeriod(ticker, "w");
                boolean hasM = stockCandleRepository.existsBySymbolAndPeriod(ticker, "m");

                if (hasW && hasM) {
                        log.debug("부분 배치 로드 스킵: {} 종목의 w, m 데이터가 모두 존재함", ticker);
                        return false;
                }

                // 없는 resolution 리스트 생성
                List<String> missingPeriods = new ArrayList<>();
                if (!hasW) {
                        missingPeriods.add("w");
                }
                if (!hasM) {
                        missingPeriods.add("m");
                }

                log.info("부분 배치 로드 시작: {} 종목의 누락된 resolution 가져오기 (missing: {})", ticker, missingPeriods);

                String order = "a"; // 오름차순
                int successCount = 0;

                // Quota 체크 (배치 로드 전에 한번만 체크)
                if (!quotaManager.checkQuota("EODHD")) {
                        log.warn("부분 배치 로드 실패: EODHD Quota 초과");
                        return false;
                }

                // 없는 period별로 순차 호출 및 저장
                for (String period : missingPeriods) {
                        try {
                                List<EodhdClient.EodhdCandle> eodhdCandles = eodhdClient
                                                .getHistoricalData(ticker, fromDate, toDate, period, order);

                                if (eodhdCandles != null && !eodhdCandles.isEmpty()) {
                                        List<StockCandle> newCandles = eodhdCandles.stream()
                                                        .filter(c -> c.getDate() != null)
                                                        .map(c -> StockCandle.builder()
                                                                        .symbol(ticker)
                                                                        .date(c.getDate())
                                                                        .period(period)
                                                                        .open(BigDecimal.valueOf(
                                                                                        c.getOpen() != null
                                                                                                        ? c.getOpen()
                                                                                                        : 0.0))
                                                                        .high(BigDecimal.valueOf(
                                                                                        c.getHigh() != null
                                                                                                        ? c.getHigh()
                                                                                                        : 0.0))
                                                                        .low(BigDecimal.valueOf(
                                                                                        c.getLow() != null ? c.getLow()
                                                                                                        : 0.0))
                                                                        .close(BigDecimal.valueOf(
                                                                                        c.getClose() != null
                                                                                                        ? c.getClose()
                                                                                                        : 0.0))
                                                                        .volume(c.getVolume() != null ? c.getVolume()
                                                                                        : 0L)
                                                                        .build())
                                                        .collect(Collectors.toList());

                                        if (!newCandles.isEmpty()) {
                                                stockCandleRepository.saveAll(newCandles);
                                                successCount++;
                                                log.info("부분 배치 로드 성공: {} period={}, count={}", ticker, period,
                                                                newCandles.size());
                                        }
                                } else {
                                        log.warn("부분 배치 로드: {} period={} 데이터 없음", ticker, period);
                                }
                        } catch (Exception e) {
                                log.error("부분 배치 로드 실패: {} period={}, error={}", ticker, period, e.getMessage(), e);
                        }
                }

                // 부분 배치 로드가 하나라도 성공했으면 Quota 1회만 카운트
                if (successCount > 0) {
                        quotaManager.incrementUsage("EODHD");
                        log.info("부분 배치 로드 완료: {} 종목, 성공한 resolution={}/{}, Quota 1회 카운트", ticker, successCount,
                                        missingPeriods.size());
                } else {
                        log.warn("부분 배치 로드 실패: {} 종목의 모든 누락된 resolution 실패", ticker);
                }

                return successCount > 0;
        }

        /**
         * 개별 resolution 보완: 요청된 resolution만 개별적으로 가져오기
         * 
         * @param ticker   종목 심볼
         * @param period   resolution (d, w, m)
         * @param fromDate 시작 날짜
         * @param toDate   종료 날짜
         * @return 로드 성공 여부
         */
        private boolean loadSingleResolution(String ticker, String period, LocalDate fromDate, LocalDate toDate) {
                // 이미 존재하는지 체크
                if (stockCandleRepository.existsBySymbolAndPeriod(ticker, period)) {
                        log.debug("개별 로드 스킵: {} period={} 데이터가 이미 존재함", ticker, period);
                        return false;
                }

                log.info("개별 resolution 로드 시작: {} period={}", ticker, period);

                String order = "a"; // 오름차순

                // Quota 체크
                if (!quotaManager.checkQuota("EODHD")) {
                        log.warn("개별 로드 실패: EODHD Quota 초과");
                        return false;
                }

                try {
                        List<EodhdClient.EodhdCandle> eodhdCandles = eodhdClient
                                        .getHistoricalData(ticker, fromDate, toDate, period, order);

                        if (eodhdCandles != null && !eodhdCandles.isEmpty()) {
                                List<StockCandle> newCandles = eodhdCandles.stream()
                                                .filter(c -> c.getDate() != null)
                                                .map(c -> StockCandle.builder()
                                                                .symbol(ticker)
                                                                .date(c.getDate())
                                                                .period(period)
                                                                .open(BigDecimal.valueOf(
                                                                                c.getOpen() != null ? c.getOpen()
                                                                                                : 0.0))
                                                                .high(BigDecimal.valueOf(
                                                                                c.getHigh() != null ? c.getHigh()
                                                                                                : 0.0))
                                                                .low(BigDecimal.valueOf(
                                                                                c.getLow() != null ? c.getLow() : 0.0))
                                                                .close(BigDecimal.valueOf(
                                                                                c.getClose() != null ? c.getClose()
                                                                                                : 0.0))
                                                                .volume(c.getVolume() != null ? c.getVolume() : 0L)
                                                                .build())
                                                .collect(Collectors.toList());

                                if (!newCandles.isEmpty()) {
                                        stockCandleRepository.saveAll(newCandles);
                                        quotaManager.incrementUsage("EODHD");
                                        log.info("개별 resolution 로드 완료: {} period={}, count={}, Quota 1회 카운트", ticker,
                                                        period, newCandles.size());
                                        return true;
                                }
                        } else {
                                log.warn("개별 로드: {} period={} 데이터 없음", ticker, period);
                        }
                } catch (Exception e) {
                        log.error("개별 로드 실패: {} period={}, error={}", ticker, period, e.getMessage(), e);
                }

                return false;
        }

        // ------------------------------------------
        // 캔들 차트 데이터 조회 (GET /api/v1/stock/candles/{ticker})
        // ------------------------------------------
        // Phase 3.5 Data Strategy: EODHD + DB Caching + Quota Management
        // Step 1: DB 조회
        // Step 2: 데이터 최신성 체크 (오늘 장 종료 후 오늘 데이터 존재 여부)
        // Step 3: 배치 로드 체크 (데이터 없을 때만 d, w, m 모두 가져오기)
        // Step 4: Quota 체크 → EODHD 호출 또는 기존 데이터 반환
        // Step 5: Quota 초과 시 Case A(기존 데이터 반환 + Stale 표시) 또는 Case B(429 에러)
        // ------------------------------------------
        @Transactional
        public StockCandlesResponse getCandles(String ticker, String resolution, LocalDateTime from, LocalDateTime to) {
                log.debug("캔들 차트 데이터 조회 요청: ticker={}, from={}, to={}", ticker, from, to);

                // Step 1: 날짜 변환 및 resolution → period 매핑
                LocalDate fromDate = from.toLocalDate();
                LocalDate toDate = to.toLocalDate();
                LocalDate today = LocalDate.now();

                // resolution을 EODHD API의 period로 변환
                // resolution: d (daily), w (weekly), m (monthly) - EODHD API와 동일한 형식
                // period: d (daily), w (weekly), m (monthly)
                // 람다 표현식에서 사용하기 위해 final 변수로 선언
                final String period = determinePeriod(resolution);
                String order = "a"; // 오름차순 (기본값)

                // Step 2: 전체 배치 로드 체크 (d 데이터가 없을 때만)
                if (!stockCandleRepository.existsBySymbolAndPeriod(ticker, "d")) {
                        log.debug("전체 배치 로드 조건 충족: {} 종목의 d 데이터가 없음", ticker);
                        batchLoadAllResolutions(ticker, fromDate, toDate);
                } else {
                        // Step 2-1: 부분 배치 로드 체크 (d는 있지만 w, m 중 일부가 없을 때)
                        batchLoadMissingResolutions(ticker, fromDate, toDate);
                }

                // Step 3: DB 조회 (요청된 period)
                List<StockCandle> cachedCandles = stockCandleRepository
                                .findAllBySymbolAndPeriodAndDateBetweenOrderByDateAsc(ticker, period, fromDate, toDate);

                // Step 4: 요청된 resolution이 없으면 개별 보완
                // 주의: Step 2-1에서 부분 배치 로드가 실행되었을 수 있으므로,
                // 요청된 period가 w 또는 m이고 해당 period의 데이터가 전혀 없을 때만 개별 보완 실행
                boolean singleResolutionLoaded = false;
                if (cachedCandles.isEmpty() && !stockCandleRepository.existsBySymbolAndPeriod(ticker, period)) {
                        // d는 전체 배치 로드에서 처리되므로, w 또는 m만 개별 보완
                        if ("w".equals(period) || "m".equals(period)) {
                                log.debug("개별 resolution 보완 필요: {} period={} 데이터가 없음", ticker, period);
                                singleResolutionLoaded = loadSingleResolution(ticker, period, fromDate, toDate);
                                if (singleResolutionLoaded) {
                                        // 다시 조회
                                        cachedCandles = stockCandleRepository
                                                        .findAllBySymbolAndPeriodAndDateBetweenOrderByDateAsc(ticker,
                                                                        period, fromDate, toDate);
                                }
                        }
                }

                // Step 5: 데이터 최신성 체크 (오늘 장 종료 후 오늘 데이터 존재 여부)
                // 미국 주식 시장은 보통 9:30 AM - 4:00 PM ET (동부시간)
                // 한국시간으로는 약 22:30 - 05:00 (다음날)이지만, 간단하게 "오늘 날짜의 데이터가 있는지"만 체크
                boolean needsRefresh = false;
                if (!cachedCandles.isEmpty()) {
                        // 요청 범위에 오늘 날짜가 포함되어 있고, 오늘 데이터가 없으면 갱신 필요
                        if (toDate.isAfter(today.minusDays(1)) &&
                                        cachedCandles.stream().noneMatch(c -> c.getDate().equals(today))) {
                                needsRefresh = true;
                                log.debug("오늘 데이터가 없어 갱신 필요: ticker={}, period={}, latestDate={}", ticker, period,
                                                cachedCandles.isEmpty() ? null
                                                                : cachedCandles.get(cachedCandles.size() - 1)
                                                                                .getDate());
                        }
                } else {
                        // DB에 해당 period 데이터가 없으면 갱신 필요
                        needsRefresh = true;
                }

                // Step 6: Quota 체크 및 EODHD 호출 (필요한 경우에만)
                // 주의: Step 4에서 개별 보완을 실행했다면, 이미 데이터를 가져왔으므로 Step 6은 스킵
                boolean isStale = false;
                if (needsRefresh && !singleResolutionLoaded) {
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
                                                                                .period(period)
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
                                                        log.info("EODHD 데이터 적재 완료: ticker={}, period={}, count={}",
                                                                        ticker,
                                                                        period, newCandles.size());
                                                }
                                        }
                                } catch (Exception e) {
                                        log.error("EODHD API 처리 중 오류 발생: {}", e.getMessage(), e);
                                        // API 호출 실패 시 기존 데이터가 있으면 그것을 반환 (Stale 표시)
                                        if (cachedCandles.isEmpty()) {
                                                // 기존 데이터도 없으면 예외를 던지지 않고 빈 리스트 반환
                                                log.warn("EODHD API 실패 및 기존 데이터 없음: ticker={}, period={}", ticker,
                                                                period);
                                        } else {
                                                isStale = true;
                                                log.warn("EODHD API 실패, 기존 데이터 반환 (Stale): ticker={}, period={}",
                                                                ticker, period);
                                        }
                                }
                        } else {
                                // Quota 초과 시 Case A 또는 Case B
                                if (cachedCandles.isEmpty()) {
                                        // Case B: 기존 데이터 없음 → 429 에러
                                        log.warn("EODHD Quota 초과 및 기존 데이터 없음: ticker={}, period={}", ticker, period);
                                        throw new BusinessException(ErrorCode.QUOTA_EXCEEDED);
                                } else {
                                        // Case A: 기존 데이터 반환 + Stale 표시
                                        isStale = true;
                                        log.warn("EODHD Quota 초과, 기존 데이터 반환 (Stale): ticker={}, period={}", ticker,
                                                        period);
                                }
                        }
                }

                // Step 7: Entity -> Response DTO 변환
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
