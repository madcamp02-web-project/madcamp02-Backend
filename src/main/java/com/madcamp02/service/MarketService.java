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
//======================================

import com.madcamp02.domain.stock.MarketCapStock;
import com.madcamp02.domain.stock.MarketCapStockRepository;
import com.madcamp02.dto.response.MarketIndicesResponse;
import com.madcamp02.dto.response.MarketMoversResponse;
import com.madcamp02.dto.response.MarketNewsResponse;
import com.madcamp02.external.FinnhubClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketService {

    private final FinnhubClient finnhubClient;
    private final MarketCapStockRepository marketCapStockRepository;

    // 주요 미국 지수 심볼 리스트 (ETF 사용 - Finnhub Quote API는 지수 심볼을 지원하지 않음)
    // 참고: Finnhub Quote API는 US stocks만 지원하며, 지수 심볼(^DJI, ^GSPC, ^IXIC)은 지원하지 않음
    // 따라서 지수를 추적하는 ETF를 사용하여 지수 데이터를 제공
    private static final List<IndexSymbol> INDEX_SYMBOLS = Arrays.asList(
            new IndexSymbol("NASDAQ", "QQQ", "USD"),         // NASDAQ-100 ETF (NASDAQ Composite 대신)
            new IndexSymbol("SP500", "SPY", "USD"),          // S&P 500 ETF
            new IndexSymbol("DJI", "DIA", "USD")             // Dow Jones Industrial Average ETF
    );

    // Fallback용 기본 종목 리스트 (DB에 데이터가 없을 때 사용)
    private static final List<String> DEFAULT_MOVER_SYMBOLS = Arrays.asList(
            "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA",
            "META", "NVDA", "JPM", "V", "JNJ"
    );

    //------------------------------------------
    // 지수 정보 조회 (GET /api/v1/market/indices)
    //------------------------------------------
    @Transactional(readOnly = true)
    @Cacheable(value = "market:indices", key = "'indices'")
    public MarketIndicesResponse getIndices() {
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
                            indexSymbol.code, indexSymbol.symbol, quote.getCurrentPrice(), quote.getPreviousClose(), quote.getOpen());
                }
            } catch (Exception e) {
                log.warn("지수 ETF 조회 실패: code={}, symbol={}, error={}", indexSymbol.code, indexSymbol.symbol, e.getMessage(), e);
                // 일부 지수 조회 실패해도 다른 지수는 계속 조회
            }
        }

        String asOf = LocalDateTime.now().toString(); // ISO-8601 형식

        return MarketIndicesResponse.builder()
                .asOf(asOf)
                .items(items)
                .build();
    }

    //------------------------------------------
    // 시장 뉴스 조회 (GET /api/v1/market/news)
    //------------------------------------------
    @Transactional(readOnly = true)
    @Cacheable(value = "market:news", key = "'news'")
    public MarketNewsResponse getNews() {
        log.debug("시장 뉴스 조회 시작");

        List<FinnhubClient.NewsItem> newsItems = finnhubClient.getNews("general");

        List<MarketNewsResponse.Item> items = newsItems.stream()
                .limit(20) // 최대 20개만 반환
                .map(news -> {
                    String publishedAt = news.getDatetime() != null
                            ? LocalDateTime.ofInstant(
                                    Instant.ofEpochSecond(news.getDatetime()),
                                    ZoneId.systemDefault()
                            ).toString()
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

    //------------------------------------------
    // 급등/급락 종목 조회 (GET /api/v1/market/movers)
    //------------------------------------------
    // Phase 3.5: Top 20 Market Cap 리스트를 DB로 관리
    // 주의: Finnhub에 "movers" 전용 엔드포인트가 없으므로
    // 주요 미국 주식들의 quote를 조회하여 changePercent 기준으로 정렬
    // 종목명은 검색 API로 조회
    //------------------------------------------
    @Transactional(readOnly = true)
    @Cacheable(value = "market:movers", key = "'movers'")
    public MarketMoversResponse getMovers() {
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
                if (changePercent == null && quote.getCurrentPrice() != null && quote.getPreviousClose() != null && quote.getPreviousClose() != 0) {
                    changePercent = ((quote.getCurrentPrice() - quote.getPreviousClose()) / quote.getPreviousClose()) * 100;
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
                                        .filter(r -> symbol.equals(r.getSymbol()) || symbol.equals(r.getDisplaySymbol()))
                                        .findFirst()
                                        .map(FinnhubClient.SearchResult::getDescription)
                                        .orElse(symbol);
                            }
                        } catch (Exception e) {
                            log.debug("종목명 조회 실패 (무시): symbol={}, error={}", symbol, e.getMessage());
                            // 종목명 조회 실패해도 계속 진행
                        }
                    }

                    MoverItem mover = new MoverItem(
                            symbol,
                            companyName,
                            quote.getCurrentPrice(),
                            changePercent != null ? changePercent : 0.0,
                            0L, // volume은 quote에 없으므로 0으로 설정
                            (changePercent != null && changePercent >= 0) ? MarketMoversResponse.Direction.UP : MarketMoversResponse.Direction.DOWN
                    );

                    movers.add(mover);
                }
            } catch (Exception e) {
                log.warn("종목 조회 실패: symbol={}, error={}", symbol, e.getMessage());
            }
        }

        // changePercent 기준으로 정렬 (절댓값 기준 내림차순)
        movers.sort((a, b) -> Double.compare(Math.abs(b.changePercent), Math.abs(a.changePercent)));

        // 상위 5개만 반환
        List<MarketMoversResponse.Item> items = movers.stream()
                .limit(5)
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

        return MarketMoversResponse.builder()
                .asOf(asOf)
                .items(items)
                .build();
    }

    //------------------------------------------
    // 내부 클래스: 지수 심볼 정보
    //------------------------------------------
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

    //------------------------------------------
    // 내부 클래스: Mover 아이템
    //------------------------------------------
    private static class MoverItem {
        final String ticker;
        final String name;
        final Double price;
        final Double changePercent;
        final Long volume;
        final MarketMoversResponse.Direction direction;

        MoverItem(String ticker, String name, Double price, Double changePercent, Long volume, MarketMoversResponse.Direction direction) {
            this.ticker = ticker;
            this.name = name;
            this.price = price;
            this.changePercent = changePercent;
            this.volume = volume;
            this.direction = direction;
        }
    }
}
