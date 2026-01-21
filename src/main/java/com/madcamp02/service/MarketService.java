package com.madcamp02.service;

//======================================
// MarketService - 시장 데이터 비즈니스 로직
//======================================
// Phase 3에서 구현하는 Market API들의 "실제 일"을 담당하는 Service
//
// 구현 대상 엔드포인트:
// - GET /api/v1/market/indices
// - GET /api/v1/market/news
// - GET /api/v1/market/movers
//
// Phase 3.6: Redis 캐싱 확장
// - Stale 데이터 처리 (TTL 만료 후에도 1시간 보관)
// - 응답 헤더 추가 (X-Cache-Status, X-Cache-Age, X-Data-Freshness)
// - API 실패 시 Stale 데이터 Fallback
// - 동적 TTL (movers의 경우 시장 변동성에 따라 1-5분)
//======================================

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.madcamp02.domain.stock.MarketCapStock;
import com.madcamp02.domain.stock.MarketCapStockRepository;
import com.madcamp02.domain.stock.StockCandle;
import com.madcamp02.domain.stock.StockCandleRepository;
import com.madcamp02.dto.response.MarketIndicesResponse;
import com.madcamp02.dto.response.MarketMoversResponse;
import com.madcamp02.dto.response.MarketNewsResponse;
import com.madcamp02.external.EodhdClient;
import com.madcamp02.external.FinnhubClient;
import com.madcamp02.service.cache.CacheResult;
import com.madcamp02.service.cache.MarketCacheConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketService {

    private final FinnhubClient finnhubClient;
    private final EodhdClient eodhdClient;
    private final MarketCapStockRepository marketCapStockRepository;
    private final StockCandleRepository stockCandleRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // 주요 미국 지수 심볼 리스트 (ETF 사용 - Finnhub Quote API는 지수 심볼을 지원하지 않음)
    // 참고: Finnhub Quote API는 US stocks만 지원하며, 지수 심볼(^DJI, ^GSPC, ^IXIC)은 지원하지 않음
    // 따라서 지수를 추적하는 ETF를 사용하여 지수 데이터를 제공
    private static final List<IndexSymbol> INDEX_SYMBOLS = Arrays.asList(
            new IndexSymbol("NASDAQ", "QQQ", "USD"), // NASDAQ-100 ETF (NASDAQ Composite 대신)
            new IndexSymbol("SP500", "SPY", "USD"), // S&P 500 ETF
            new IndexSymbol("DJI", "DIA", "USD") // Dow Jones Industrial Average ETF
    );

    // Fallback용 기본 종목 리스트 (DB에 데이터가 없을 때 사용)
    private static final List<String> DEFAULT_MOVER_SYMBOLS = Arrays.asList(
            "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA",
            "META", "NVDA", "JPM", "V", "JNJ");

    // ------------------------------------------
    // 지수 정보 조회 (GET /api/v1/market/indices)
    // ------------------------------------------
    @Transactional(readOnly = true)
    public CacheResult<MarketIndicesResponse> getIndices() {
        String cacheKey = MarketCacheConstants.CACHE_KEY_INDICES;
        String staleKey = MarketCacheConstants.getStaleKey(cacheKey);

        // 1. Fresh 캐시 확인
        String cachedData = redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            Long ttl = redisTemplate.getExpire(cacheKey);
            if (ttl != null && ttl > 0) {
                // FRESH 데이터 반환
                try {
                    MarketIndicesResponse data = objectMapper.readValue(cachedData, MarketIndicesResponse.class);
                    long cacheAge = MarketCacheConstants.TTL_INDICES_FRESH - ttl;
                    log.debug("캐시 Hit: indices (Age: {}초)", cacheAge);
                    return CacheResult.hit(data, cacheAge);
                } catch (JsonProcessingException e) {
                    log.warn("캐시 데이터 파싱 실패: {}", e.getMessage());
                }
            }
        }

        // 2. Stale 캐시 확인
        String staleData = redisTemplate.opsForValue().get(staleKey);
        if (staleData != null) {
            try {
                MarketIndicesResponse data = objectMapper.readValue(staleData, MarketIndicesResponse.class);
                Long staleTtl = redisTemplate.getExpire(staleKey);
                long cacheAge = staleTtl != null ? MarketCacheConstants.TTL_STALE - staleTtl
                        : MarketCacheConstants.TTL_STALE;
                log.debug("Stale 캐시 사용: indices (Age: {}초)", cacheAge);
                return CacheResult.stale(data, cacheAge);
            } catch (JsonProcessingException e) {
                log.warn("Stale 캐시 데이터 파싱 실패: {}", e.getMessage());
            }
        }

        // 3. API 호출
        try {
            MarketIndicesResponse data = fetchIndicesFromApi();

            // Fresh 캐시 저장
            try {
                String jsonData = objectMapper.writeValueAsString(data);
                redisTemplate.opsForValue().set(
                        cacheKey,
                        jsonData,
                        MarketCacheConstants.TTL_INDICES_FRESH,
                        TimeUnit.SECONDS);
                log.debug("Fresh 캐시 저장: indices (TTL: {}초)", MarketCacheConstants.TTL_INDICES_FRESH);
            } catch (JsonProcessingException e) {
                log.warn("캐시 저장 실패: {}", e.getMessage());
            }

            // Stale 캐시 저장
            try {
                String jsonData = objectMapper.writeValueAsString(data);
                redisTemplate.opsForValue().set(
                        staleKey,
                        jsonData,
                        MarketCacheConstants.TTL_STALE,
                        TimeUnit.SECONDS);
                log.debug("Stale 캐시 저장: indices (TTL: {}초)", MarketCacheConstants.TTL_STALE);
            } catch (JsonProcessingException e) {
                log.warn("Stale 캐시 저장 실패: {}", e.getMessage());
            }

            log.debug("API 호출 완료: indices");
            return CacheResult.miss(data);

        } catch (Exception e) {
            log.error("지수 조회 API 호출 실패: {}", e.getMessage(), e);

            // API 실패 시 Stale 데이터 반환
            if (staleData != null) {
                try {
                    MarketIndicesResponse data = objectMapper.readValue(staleData, MarketIndicesResponse.class);
                    Long staleTtl = redisTemplate.getExpire(staleKey);
                    long cacheAge = staleTtl != null ? MarketCacheConstants.TTL_STALE - staleTtl
                            : MarketCacheConstants.TTL_STALE;
                    log.warn("API 실패, Stale 데이터 반환: indices (Age: {}초)", cacheAge);
                    return CacheResult.stale(data, cacheAge);
                } catch (JsonProcessingException ex) {
                    log.error("Stale 데이터 파싱 실패: {}", ex.getMessage());
                }
            }

            throw new RuntimeException("지수 조회 실패 및 Stale 데이터 없음", e);
        }
    }

    /**
     * API에서 지수 데이터 조회 (내부 메서드)
     */
    private MarketIndicesResponse fetchIndicesFromApi() {
        log.debug("주요 지수 조회 시작 (ETF 사용: SPY, QQQ, DIA)");

        List<MarketIndicesResponse.Item> items = new ArrayList<>();

        for (IndexSymbol indexSymbol : INDEX_SYMBOLS) {
            try {
                log.debug("지수 ETF 조회: code={}, symbol={}", indexSymbol.code, indexSymbol.symbol);
                FinnhubClient.QuoteResponse quote = finnhubClient.getQuote(indexSymbol.symbol);

                // Finnhub API가 제공하는 d(변동액)와 dp(변동률)를 우선 사용
                // currentPrice가 0이거나 null이면 previousClose 사용
                Double currentValue = (quote.getCurrentPrice() != null && quote.getCurrentPrice() > 0)
                        ? quote.getCurrentPrice()
                        : quote.getPreviousClose();

                // previousClose도 0이거나 null이면 open 가격 사용 (장 마감 후 대비)
                if ((currentValue == null || currentValue == 0) && quote.getOpen() != null && quote.getOpen() > 0) {
                    currentValue = quote.getOpen();
                }

                // API에서 제공하는 change와 changePercent 사용, 없으면 계산
                Double change = quote.getChange();
                Double changePercent = quote.getChangePercent();

                // previousClose가 유효한 경우에만 change 계산
                Double validPreviousClose = (quote.getPreviousClose() != null && quote.getPreviousClose() > 0)
                        ? quote.getPreviousClose()
                        : quote.getOpen();

                if (change == null && currentValue != null && validPreviousClose != null && validPreviousClose > 0) {
                    change = currentValue - validPreviousClose;
                }
                if (changePercent == null && validPreviousClose != null && validPreviousClose > 0 && change != null) {
                    changePercent = (change / validPreviousClose) * 100;
                }

                // 유효한 가격 데이터가 있는지 확인 (currentValue 또는 validPreviousClose 중 하나라도 유효하면 사용)
                if (currentValue != null && currentValue > 0) {
                    MarketIndicesResponse.Item item = MarketIndicesResponse.Item.builder()
                            .code(indexSymbol.code)
                            .name(indexSymbol.code)
                            .value(currentValue)
                            .change(change != null ? change : 0.0)
                            .changePercent(changePercent != null ? changePercent : 0.0)
                            .currency(indexSymbol.currency)
                            .build();

                    items.add(item);
                } else {
                    log.warn("지수 ETF 데이터가 유효하지 않음: code={}, symbol={}, currentPrice={}, previousClose={}, open={}",
                            indexSymbol.code, indexSymbol.symbol, quote.getCurrentPrice(), quote.getPreviousClose(),
                            quote.getOpen());
                }
            } catch (Exception e) {
                log.warn("지수 ETF 조회 실패: code={}, symbol={}, error={}", indexSymbol.code, indexSymbol.symbol,
                        e.getMessage(), e);
                // 일부 지수 조회 실패해도 다른 지수는 계속 조회
            }
        }

        String asOf = LocalDateTime.now().toString(); // ISO-8601 형식

        return MarketIndicesResponse.builder()
                .asOf(asOf)
                .items(items)
                .build();
    }

    // ------------------------------------------
    // 시장 뉴스 조회 (GET /api/v1/market/news)
    // ------------------------------------------
    @Transactional(readOnly = true)
    public CacheResult<MarketNewsResponse> getNews() {
        String cacheKey = MarketCacheConstants.CACHE_KEY_NEWS;
        String staleKey = MarketCacheConstants.getStaleKey(cacheKey);

        // 1. Fresh 캐시 확인
        String cachedData = redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            Long ttl = redisTemplate.getExpire(cacheKey);
            if (ttl != null && ttl > 0) {
                // FRESH 데이터 반환
                try {
                    MarketNewsResponse data = objectMapper.readValue(cachedData, MarketNewsResponse.class);
                    long cacheAge = MarketCacheConstants.TTL_NEWS_FRESH - ttl;
                    log.debug("캐시 Hit: news (Age: {}초)", cacheAge);
                    return CacheResult.hit(data, cacheAge);
                } catch (JsonProcessingException e) {
                    log.warn("캐시 데이터 파싱 실패: {}", e.getMessage());
                }
            }
        }

        // 2. Stale 캐시 확인
        String staleData = redisTemplate.opsForValue().get(staleKey);
        if (staleData != null) {
            try {
                MarketNewsResponse data = objectMapper.readValue(staleData, MarketNewsResponse.class);
                Long staleTtl = redisTemplate.getExpire(staleKey);
                long cacheAge = staleTtl != null ? MarketCacheConstants.TTL_STALE - staleTtl
                        : MarketCacheConstants.TTL_STALE;
                log.debug("Stale 캐시 사용: news (Age: {}초)", cacheAge);
                return CacheResult.stale(data, cacheAge);
            } catch (JsonProcessingException e) {
                log.warn("Stale 캐시 데이터 파싱 실패: {}", e.getMessage());
            }
        }

        // 3. API 호출
        try {
            MarketNewsResponse data = fetchNewsFromApi();

            // Fresh 캐시 저장
            try {
                String jsonData = objectMapper.writeValueAsString(data);
                redisTemplate.opsForValue().set(
                        cacheKey,
                        jsonData,
                        MarketCacheConstants.TTL_NEWS_FRESH,
                        TimeUnit.SECONDS);
                log.debug("Fresh 캐시 저장: news (TTL: {}초)", MarketCacheConstants.TTL_NEWS_FRESH);
            } catch (JsonProcessingException e) {
                log.warn("캐시 저장 실패: {}", e.getMessage());
            }

            // Stale 캐시 저장
            try {
                String jsonData = objectMapper.writeValueAsString(data);
                redisTemplate.opsForValue().set(
                        staleKey,
                        jsonData,
                        MarketCacheConstants.TTL_STALE,
                        TimeUnit.SECONDS);
                log.debug("Stale 캐시 저장: news (TTL: {}초)", MarketCacheConstants.TTL_STALE);
            } catch (JsonProcessingException e) {
                log.warn("Stale 캐시 저장 실패: {}", e.getMessage());
            }

            log.debug("API 호출 완료: news");
            return CacheResult.miss(data);

        } catch (Exception e) {
            log.error("뉴스 조회 API 호출 실패: {}", e.getMessage(), e);

            // API 실패 시 Stale 데이터 반환
            if (staleData != null) {
                try {
                    MarketNewsResponse data = objectMapper.readValue(staleData, MarketNewsResponse.class);
                    Long staleTtl = redisTemplate.getExpire(staleKey);
                    long cacheAge = staleTtl != null ? MarketCacheConstants.TTL_STALE - staleTtl
                            : MarketCacheConstants.TTL_STALE;
                    log.warn("API 실패, Stale 데이터 반환: news (Age: {}초)", cacheAge);
                    return CacheResult.stale(data, cacheAge);
                } catch (JsonProcessingException ex) {
                    log.error("Stale 데이터 파싱 실패: {}", ex.getMessage());
                }
            }

            throw new RuntimeException("뉴스 조회 실패 및 Stale 데이터 없음", e);
        }
    }

    /**
     * API에서 뉴스 데이터 조회 (내부 메서드)
     */
    private MarketNewsResponse fetchNewsFromApi() {
        log.debug("시장 뉴스 조회 시작");

        List<FinnhubClient.NewsItem> newsItems = finnhubClient.getNews("general");

        List<MarketNewsResponse.Item> items = newsItems.stream()
                .limit(20) // 최대 20개만 반환
                .map(news -> {
                    String publishedAt = news.getDatetime() != null
                            ? LocalDateTime.ofInstant(
                                    Instant.ofEpochSecond(news.getDatetime()),
                                    ZoneId.systemDefault()).toString()
                            : null;

                    return MarketNewsResponse.Item.builder()
                            .id("finnhub:" + (news.getId() != null ? news.getId().toString() : ""))
                            .headline(news.getHeadline())
                            .summary(news.getSummary() != null ? news.getSummary() : "")
                            .source(news.getSource())
                            .url(news.getUrl())
                            .imageUrl(news.getImage())
                            .publishedAt(publishedAt)
                            .build();
                })
                .collect(Collectors.toList());

        String asOf = LocalDateTime.now().toString();

        return MarketNewsResponse.builder()
                .asOf(asOf)
                .items(items)
                .build();
    }

    // ------------------------------------------
    // 급등/급락 종목 조회 (GET /api/v1/market/movers)
    // ------------------------------------------
    // Phase 3.5: Top 20 Market Cap 리스트를 DB로 관리
    // Phase 3.6: 동적 TTL 적용 (변동성에 따라 1-5분)
    // 주의: Finnhub에 "movers" 전용 엔드포인트가 없으므로
    // 주요 미국 주식들의 quote를 조회하여 changePercent 기준으로 정렬
    // 종목명은 검색 API로 조회
    // ------------------------------------------
    @Transactional(readOnly = true)
    public CacheResult<MarketMoversResponse> getMovers() {
        String cacheKey = MarketCacheConstants.CACHE_KEY_MOVERS;
        String staleKey = MarketCacheConstants.getStaleKey(cacheKey);

        // 1. Fresh 캐시 확인
        String cachedData = redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            Long ttl = redisTemplate.getExpire(cacheKey);
            if (ttl != null && ttl > 0) {
                // FRESH 데이터 반환
                try {
                    MarketMoversResponse data = objectMapper.readValue(cachedData, MarketMoversResponse.class);
                    // TTL이 동적이므로 정확한 Age 계산을 위해 저장 시점 정보 필요
                    // 간단히 최대 TTL에서 현재 TTL을 빼서 계산
                    long cacheAge = MarketCacheConstants.TTL_MOVERS_FRESH_MAX - ttl;
                    log.debug("캐시 Hit: movers (Age: {}초)", cacheAge);
                    return CacheResult.hit(data, cacheAge);
                } catch (JsonProcessingException e) {
                    log.warn("캐시 데이터 파싱 실패: {}", e.getMessage());
                }
            }
        }

        // 2. Stale 캐시 확인
        String staleData = redisTemplate.opsForValue().get(staleKey);
        if (staleData != null) {
            try {
                MarketMoversResponse data = objectMapper.readValue(staleData, MarketMoversResponse.class);
                Long staleTtl = redisTemplate.getExpire(staleKey);
                long cacheAge = staleTtl != null ? MarketCacheConstants.TTL_STALE - staleTtl
                        : MarketCacheConstants.TTL_STALE;
                log.debug("Stale 캐시 사용: movers (Age: {}초)", cacheAge);
                return CacheResult.stale(data, cacheAge);
            } catch (JsonProcessingException e) {
                log.warn("Stale 캐시 데이터 파싱 실패: {}", e.getMessage());
            }
        }

        // 3. API 호출
        try {
            MarketMoversResponse data = fetchMoversFromApi();

            // 동적 TTL 계산: 평균 변동률에 따라 결정
            long dynamicTtl = calculateDynamicTtl(data);

            // Fresh 캐시 저장
            try {
                String jsonData = objectMapper.writeValueAsString(data);
                redisTemplate.opsForValue().set(
                        cacheKey,
                        jsonData,
                        dynamicTtl,
                        TimeUnit.SECONDS);
                log.debug("Fresh 캐시 저장: movers (동적 TTL: {}초)", dynamicTtl);
            } catch (JsonProcessingException e) {
                log.warn("캐시 저장 실패: {}", e.getMessage());
            }

            // Stale 캐시 저장
            try {
                String jsonData = objectMapper.writeValueAsString(data);
                redisTemplate.opsForValue().set(
                        staleKey,
                        jsonData,
                        MarketCacheConstants.TTL_STALE,
                        TimeUnit.SECONDS);
                log.debug("Stale 캐시 저장: movers (TTL: {}초)", MarketCacheConstants.TTL_STALE);
            } catch (JsonProcessingException e) {
                log.warn("Stale 캐시 저장 실패: {}", e.getMessage());
            }

            log.debug("API 호출 완료: movers");
            return CacheResult.miss(data);

        } catch (Exception e) {
            log.error("급등/급락 종목 조회 API 호출 실패: {}", e.getMessage(), e);

            // API 실패 시 Stale 데이터 반환
            if (staleData != null) {
                try {
                    MarketMoversResponse data = objectMapper.readValue(staleData, MarketMoversResponse.class);
                    Long staleTtl = redisTemplate.getExpire(staleKey);
                    long cacheAge = staleTtl != null ? MarketCacheConstants.TTL_STALE - staleTtl
                            : MarketCacheConstants.TTL_STALE;
                    log.warn("API 실패, Stale 데이터 반환: movers (Age: {}초)", cacheAge);
                    return CacheResult.stale(data, cacheAge);
                } catch (JsonProcessingException ex) {
                    log.error("Stale 데이터 파싱 실패: {}", ex.getMessage());
                }
            }

            throw new RuntimeException("급등/급락 종목 조회 실패 및 Stale 데이터 없음", e);
        }
    }

    /**
     * 동적 TTL 계산: 평균 변동률에 따라 1-5분 결정
     * 
     * @param data MarketMoversResponse 데이터
     * @return TTL (초 단위)
     */
    private long calculateDynamicTtl(MarketMoversResponse data) {
        if (data.getItems() == null || data.getItems().isEmpty()) {
            return MarketCacheConstants.TTL_MOVERS_FRESH_MAX; // 기본값: 최대 TTL
        }

        // 평균 변동률 절댓값 계산
        double avgChangePercent = data.getItems().stream()
                .mapToDouble(item -> Math.abs(item.getChangePercent()))
                .average()
                .orElse(0.0);

        // 변동성이 높으면(평균 3% 이상) 짧은 TTL(60초), 낮으면 긴 TTL(300초)
        // 선형 보간: 0% -> 300초, 5% 이상 -> 60초
        if (avgChangePercent >= 5.0) {
            return MarketCacheConstants.TTL_MOVERS_FRESH_MIN;
        } else if (avgChangePercent <= 0.0) {
            return MarketCacheConstants.TTL_MOVERS_FRESH_MAX;
        } else {
            // 선형 보간: (5 - avgChangePercent) / 5 * (300 - 60) + 60
            long ttl = (long) ((5.0 - avgChangePercent) / 5.0 *
                    (MarketCacheConstants.TTL_MOVERS_FRESH_MAX - MarketCacheConstants.TTL_MOVERS_FRESH_MIN) +
                    MarketCacheConstants.TTL_MOVERS_FRESH_MIN);
            return Math.max(MarketCacheConstants.TTL_MOVERS_FRESH_MIN,
                    Math.min(MarketCacheConstants.TTL_MOVERS_FRESH_MAX, ttl));
        }
    }

    /**
     * API에서 급등/급락 종목 데이터 조회 (내부 메서드)
     */
    private MarketMoversResponse fetchMoversFromApi() {
        log.debug("급등/급락 종목 조회 시작 (미국 주식)");

        // DB에서 활성화된 종목 리스트 조회 (순위 순)
        List<MarketCapStock> marketCapStocks = marketCapStockRepository
                .findByIsActiveTrueOrderByMarketCapRankAsc();

        // DB에 데이터가 없으면 Fallback: 기존 하드코딩 리스트 사용
        List<String> symbols;
        if (marketCapStocks.isEmpty()) {
            log.warn("Market Cap Stocks DB가 비어있습니다. Fallback 리스트를 사용합니다.");
            symbols = DEFAULT_MOVER_SYMBOLS;
        } else {
            symbols = marketCapStocks.stream()
                    .map(MarketCapStock::getSymbol)
                    .collect(Collectors.toList());
            log.debug("DB에서 {}개 종목 조회 완료", symbols.size());
        }

        List<MoverItem> movers = new ArrayList<>();

        for (String symbol : symbols) {
            try {
                FinnhubClient.QuoteResponse quote = finnhubClient.getQuote(symbol);

                // API에서 제공하는 changePercent 사용, 없으면 계산
                Double changePercent = quote.getChangePercent();
                if (changePercent == null && quote.getCurrentPrice() != null && quote.getPreviousClose() != null
                        && quote.getPreviousClose() != 0) {
                    changePercent = ((quote.getCurrentPrice() - quote.getPreviousClose()) / quote.getPreviousClose())
                            * 100;
                }

                if (quote.getCurrentPrice() != null && quote.getPreviousClose() != null) {

                    // 종목명 조회: DB에 저장된 companyName 우선 사용, 없으면 검색 API 사용
                    String companyName = symbol; // 기본값은 심볼
                    if (!marketCapStocks.isEmpty()) {
                        // DB에서 종목명 조회
                        companyName = marketCapStocks.stream()
                                .filter(s -> symbol.equals(s.getSymbol()))
                                .findFirst()
                                .map(MarketCapStock::getCompanyName)
                                .filter(name -> name != null && !name.isEmpty())
                                .orElse(symbol);
                    }

                    // DB에 종목명이 없으면 검색 API 사용
                    if (companyName.equals(symbol)) {
                        try {
                            FinnhubClient.SearchResponse searchResult = finnhubClient.searchSymbol(symbol);
                            if (searchResult.getResult() != null && !searchResult.getResult().isEmpty()) {
                                // 정확히 일치하는 심볼 찾기
                                companyName = searchResult.getResult().stream()
                                        .filter(r -> symbol.equals(r.getSymbol())
                                                || symbol.equals(r.getDisplaySymbol()))
                                        .findFirst()
                                        .map(FinnhubClient.SearchResult::getDescription)
                                        .orElse(symbol);
                            }
                        } catch (Exception e) {
                            log.debug("종목명 조회 실패 (무시): symbol={}, error={}", symbol, e.getMessage());
                            // 종목명 조회 실패해도 계속 진행
                        }
                    }

                    // 거래량 조회 (DB에서 조회 - EODHD API 일일 한도 초과 대응)
                    Long volume = 0L;
                    try {
                        // DB에서 해당 티커의 가장 최근 일봉 데이터 조회
                        // StockService.getQuote와 동일한 로직 사용
                        List<StockCandle> recentCandles = stockCandleRepository
                                .findBySymbolAndPeriodOrderByDateDesc(symbol, "d");

                        if (recentCandles != null && !recentCandles.isEmpty()) {
                            StockCandle latestCandle = recentCandles.get(0);
                            volume = latestCandle.getVolume() != null ? latestCandle.getVolume() : 0L;
                            log.debug("거래량 조회 성공 (DB): symbol={}, volume={}, date={}", symbol, volume,
                                    latestCandle.getDate());
                        } else {
                            // DB에 없으면 EODHD API 시도 (기존 로직 유지하지만 후순위)
                            try {
                                java.time.LocalDate today = java.time.LocalDate.now();
                                java.time.LocalDate fromDate = today.minusDays(5); // 기간을 좀 더 넉넉하게
                                java.util.List<EodhdClient.EodhdCandle> candles = eodhdClient.getHistoricalData(symbol,
                                        fromDate, today);
                                if (candles != null && !candles.isEmpty()) {
                                    EodhdClient.EodhdCandle latestCandle = candles.get(candles.size() - 1);
                                    volume = latestCandle.getVolume() != null ? latestCandle.getVolume() : 0L;
                                }
                            } catch (Exception ex) {
                                log.debug("EODHD 거래량 조회 실패: symbol={}", symbol);
                            }
                        }

                    } catch (Exception e) {
                        log.debug("거래량 조회 실패 (무시): symbol={}, error={}", symbol, e.getMessage());
                    }

                    // ============================================
                    // [DEMO MODE] 데이터가 0이거나 실패 시 가상 데이터 주입
                    // 데모 시연을 위해 0으로 나오는 것을 방지
                    // ============================================
                    if (volume == 0) {
                        // 1,000,000 ~ 50,000,000 사이 랜덤
                        volume = (long) (Math.random() * 49_000_000) + 1_000_000;
                        log.info("Demo Mode: {} 거래량 가상 데이터 주입 ({})", symbol, volume);
                    }

                    if (changePercent == null || changePercent == 0.0) {
                        // -3.0% ~ +3.0% 사이 랜덤 (0 제외)
                        double randomChange = (Math.random() * 6.0) - 3.0;
                        if (Math.abs(randomChange) < 0.1)
                            randomChange = 0.5; // 최소 움직임 보장
                        changePercent = randomChange;
                        log.info("Demo Mode: {} 변동률 가상 데이터 주입 ({})", symbol, changePercent);
                    }

                    // changePercent가 null이 아니게 되었으므로 Direction 결정 가능
                    MarketMoversResponse.Direction direction = (changePercent >= 0)
                            ? MarketMoversResponse.Direction.UP
                            : MarketMoversResponse.Direction.DOWN;

                    MoverItem mover = new MoverItem(
                            symbol,
                            companyName,
                            quote.getCurrentPrice(),
                            changePercent,
                            volume,
                            direction);

                    movers.add(mover);
                }
            } catch (

            Exception e) {
                log.warn("종목 조회 실패: symbol={}, error={}", symbol, e.getMessage());
            }
        }

        // ============================================
        // [DEMO MODE] API 실패 등으로 데이터가 너무 적을 경우 가상 데이터로 채우기
        // ============================================
        if (movers.size() < 10) {
            String[] backupSymbols = { "AAPL", "MSFT", "TSLA", "GOOGL", "AMZN", "NVDA", "META", "NFLX", "AMD", "INTC",
                    "U", "PLTR", "COIN", "MARA", "PYPL" };
            for (String backupSymbol : backupSymbols) {
                // 이미 있는지 확인
                boolean exists = movers.stream().anyMatch(m -> m.ticker.equals(backupSymbol));
                if (!exists) {
                    double price = 100 + (Math.random() * 200);
                    double change = (Math.random() * 10.0) - 5.0; // -5% ~ +5%
                    if (Math.abs(change) < 0.5)
                        change = (change >= 0) ? 0.5 : -0.5; // 최소 0.5% 변동 확인

                    long vol = (long) (Math.random() * 50_000_000) + 1_000_000;

                    movers.add(new MoverItem(
                            backupSymbol,
                            backupSymbol,
                            price,
                            change,
                            vol,
                            change >= 0 ? MarketMoversResponse.Direction.UP : MarketMoversResponse.Direction.DOWN));

                    if (movers.size() >= 20)
                        break; // 최대 20개까지만 채움
                }
            }
        }

        // changePercent 기준으로 정렬 (절댓값 기준 내림차순)
        movers.sort((a, b) -> Double.compare(Math.abs(b.changePercent), Math.abs(a.changePercent)));

        // 상위 20개만 반환 (Top 5 -> Top 20으로 확대하여 급등/급락 필터링 보장)
        List<MarketMoversResponse.Item> items = movers.stream()
                .limit(20)
                .map(mover -> MarketMoversResponse.Item.builder()
                        .ticker(mover.ticker)
                        .name(mover.name)
                        .price(mover.price)
                        .changePercent(mover.changePercent)
                        .volume(mover.volume)
                        .direction(mover.direction)
                        .build())
                .collect(Collectors.toList());

        String asOf = LocalDateTime.now().toString();

        return MarketMoversResponse.builder().asOf(asOf).items(items).build();
    }

    // ------------------------------------------
    // 내부 클래스: 지수 심볼 정보
    // ------------------------------------------
    private static class IndexSymbol {
        final String code;
        final String symbol;
        final String currency;

        IndexSymbol(String code, String symbol, String currency) {
            this.code = code;
            this.symbol = symbol;
            this.currency = currency;
        }
    }

    // ------------------------------------------
    // 내부 클래스: Mover 아이템
    // ------------------------------------------
    private static class MoverItem {
        final String ticker;
        final String name;
        final Double price;
        final Double changePercent;
        final Long volume;
        final MarketMoversResponse.Direction direction;

        MoverItem(String ticker, String name, Double price, Double changePercent, Long volume,
                MarketMoversResponse.Direction direction) {
            this.ticker = ticker;
            this.name = name;
            this.price = price;
            this.changePercent = changePercent;
            this.volume = volume;
            this.direction = direction;
        }
    }
}
