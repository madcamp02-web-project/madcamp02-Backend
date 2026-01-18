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

    // 주요 미국 지수 심볼 리스트 (미국 주식 모의 트레이딩)
    private static final List<IndexSymbol> INDEX_SYMBOLS = Arrays.asList(
            new IndexSymbol("NASDAQ", "^IXIC", "USD"),      // NASDAQ Composite
            new IndexSymbol("SP500", "^GSPC", "USD"),        // S&P 500
            new IndexSymbol("DJI", "^DJI", "USD")            // Dow Jones Industrial Average
    );

    // Movers를 위한 주요 종목 리스트 (임시 하드코딩)
    // 실제로는 더 많은 종목을 조회하거나 다른 API를 사용해야 함
    private static final List<String> MOVER_SYMBOLS = Arrays.asList(
            "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA",
            "META", "NVDA", "JPM", "V", "JNJ"
    );

    //------------------------------------------
    // 지수 정보 조회 (GET /api/v1/market/indices)
    //------------------------------------------
    @Transactional(readOnly = true)
    @Cacheable(value = "market:indices", key = "'indices'")
    public MarketIndicesResponse getIndices() {
        log.debug("주요 지수 조회 시작");

        List<MarketIndicesResponse.Item> items = new ArrayList<>();

        for (IndexSymbol indexSymbol : INDEX_SYMBOLS) {
            try {
                FinnhubClient.QuoteResponse quote = finnhubClient.getQuote(indexSymbol.symbol);
                
                if (quote.getCurrentPrice() != null && quote.getPreviousClose() != null) {
                    double change = quote.getCurrentPrice() - quote.getPreviousClose();
                    double changePercent = quote.getPreviousClose() != 0 
                            ? (change / quote.getPreviousClose()) * 100 
                            : 0.0;

                    MarketIndicesResponse.Item item = MarketIndicesResponse.Item.builder()
                            .code(indexSymbol.code)
                            .name(indexSymbol.code)
                            .value(quote.getCurrentPrice())
                            .change(change)
                            .changePercent(changePercent)
                            .currency(indexSymbol.currency)
                            .build();

                    items.add(item);
                }
            } catch (Exception e) {
                log.warn("지수 조회 실패: symbol={}, error={}", indexSymbol.symbol, e.getMessage());
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
    // 주의: Finnhub에 "movers" 전용 엔드포인트가 없으므로
    // 주요 미국 주식들의 quote를 조회하여 changePercent 기준으로 정렬
    // 종목명은 검색 API로 조회
    //------------------------------------------
    @Transactional(readOnly = true)
    @Cacheable(value = "market:movers", key = "'movers'")
    public MarketMoversResponse getMovers() {
        log.debug("급등/급락 종목 조회 시작 (미국 주식)");

        List<MoverItem> movers = new ArrayList<>();

        for (String symbol : MOVER_SYMBOLS) {
            try {
                FinnhubClient.QuoteResponse quote = finnhubClient.getQuote(symbol);
                
                if (quote.getCurrentPrice() != null && quote.getPreviousClose() != null) {
                    double changePercent = quote.getPreviousClose() != 0
                            ? ((quote.getCurrentPrice() - quote.getPreviousClose()) / quote.getPreviousClose()) * 100
                            : 0.0;

                    // 종목명 조회 (검색 API 사용)
                    String companyName = symbol; // 기본값은 심볼
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

                    MoverItem mover = new MoverItem(
                            symbol,
                            companyName,
                            quote.getCurrentPrice(),
                            changePercent,
                            0L, // volume은 quote에 없으므로 0으로 설정
                            changePercent >= 0 ? MarketMoversResponse.Direction.UP : MarketMoversResponse.Direction.DOWN
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
