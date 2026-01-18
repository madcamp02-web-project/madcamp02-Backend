package com.madcamp02.external;

//======================================
// FinnhubClient - Finnhub 주식 데이터 API 클라이언트
//======================================
// Finnhub REST API를 호출하여 주식 시세, 뉴스, 검색 등의 데이터를 조회합니다.
//
// 엔드포인트 (Free Tier만 사용):
// 1) GET /api/v1/quote: 현재가 조회 (High Usage - Free Tier)
// 2) GET /api/v1/search: 종목 검색 (Symbol Lookup - Free Tier)
// 3) GET /api/v1/news: 시장 뉴스 (Market News - Free Tier)
//
// 주의: Candles (OHLCV)는 Premium API이므로 사용하지 않음
//======================================

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class FinnhubClient {

    private static final String BASE_URL = "https://finnhub.io/api/v1";
    
    private final RestTemplate restTemplate;
    private final String apiKey;

    public FinnhubClient(
            RestTemplate restTemplate,
            @Value("${finnhub.api-key}") String apiKey
    ) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        
        if (apiKey == null || apiKey.isEmpty() || "sandbox_api_key".equals(apiKey)) {
            log.warn("Finnhub API 키가 설정되지 않았거나 sandbox 모드입니다. 일부 기능이 제한될 수 있습니다.");
        }
    }

    //------------------------------------------
    // Quote 응답 DTO (현재가)
    //------------------------------------------
    @Getter
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuoteResponse {
        @JsonProperty("c")
        private Double currentPrice;      // 현재가
        
        @JsonProperty("h")
        private Double high;               // 당일 최고가
        
        @JsonProperty("l")
        private Double low;                // 당일 최저가
        
        @JsonProperty("o")
        private Double open;               // 당일 시가
        
        @JsonProperty("pc")
        private Double previousClose;      // 전일 종가
        
        @JsonProperty("t")
        private Long timestamp;            // UNIX timestamp (초)
    }

    //------------------------------------------
    // Search 응답 DTO (종목 검색)
    //------------------------------------------
    @Getter
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchResponse {
        @JsonProperty("count")
        private Integer count;             // 검색 결과 개수
        
        @JsonProperty("result")
        private List<SearchResult> result;  // 검색 결과 리스트
    }

    @Getter
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchResult {
        @JsonProperty("description")
        private String description;        // 종목 설명
        
        @JsonProperty("displaySymbol")
        private String displaySymbol;      // 표시 심볼
        
        @JsonProperty("symbol")
        private String symbol;             // 심볼
        
        @JsonProperty("type")
        private String type;               // 타입 (Common Stock, ETF 등)
    }

    //------------------------------------------
    // News 응답 DTO (뉴스)
    //------------------------------------------
    @Getter
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NewsItem {
        @JsonProperty("category")
        private String category;           // 카테고리
        
        @JsonProperty("datetime")
        private Long datetime;              // UNIX timestamp (초)
        
        @JsonProperty("headline")
        private String headline;           // 헤드라인
        
        @JsonProperty("id")
        private Integer id;                // 뉴스 ID (integer)
        
        @JsonProperty("image")
        private String image;              // 이미지 URL
        
        @JsonProperty("related")
        private String related;            // 관련 종목 심볼
        
        @JsonProperty("source")
        private String source;             // 출처
        
        @JsonProperty("summary")
        private String summary;            // 요약
        
        @JsonProperty("url")
        private String url;                // 뉴스 URL
    }

    //------------------------------------------
    // 현재가 조회 (GET /api/v1/quote)
    //------------------------------------------
    // 파라미터:
    //   - symbol: 종목 심볼 (예: AAPL, ^GSPC)
    //------------------------------------------
    public QuoteResponse getQuote(String symbol) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/quote")
                    .queryParam("symbol", symbol)
                    .queryParam("token", apiKey)
                    .toUriString();

            log.debug("Finnhub API 호출 (Quote): symbol={}", symbol);

            ResponseEntity<QuoteResponse> response = restTemplate.getForEntity(
                    url, QuoteResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.debug("Finnhub API 응답 (Quote): {}", response.getBody());
                return response.getBody();
            } else {
                log.warn("Finnhub API 응답 실패 (Quote): status={}, body={}", 
                        response.getStatusCode(), response.getBody());
                throw new FinnhubException("Quote 조회 실패: " + symbol);
            }
        } catch (RestClientException e) {
            log.error("Finnhub API 호출 실패 (Quote): symbol={}", symbol, e);
            throw new FinnhubException("Quote 조회 중 오류 발생: " + e.getMessage(), e);
        }
    }

    //------------------------------------------
    // 종목 검색 (GET /api/v1/search)
    //------------------------------------------
    // 파라미터:
    //   - query: 검색어 (종목명, 심볼, ISIN, CUSIP)
    //------------------------------------------
    public SearchResponse searchSymbol(String query) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/search")
                    .queryParam("q", query)
                    .queryParam("token", apiKey)
                    .toUriString();

            log.debug("Finnhub API 호출 (Search): query={}", query);

            ResponseEntity<SearchResponse> response = restTemplate.getForEntity(
                    url, SearchResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                SearchResponse search = response.getBody();
                log.debug("Finnhub API 응답 (Search): count={}", search.getCount());
                return search;
            } else {
                log.warn("Finnhub API 응답 실패 (Search): status={}, body={}", 
                        response.getStatusCode(), response.getBody());
                throw new FinnhubException("Search 조회 실패: " + query);
            }
        } catch (RestClientException e) {
            log.error("Finnhub API 호출 실패 (Search): query={}", query, e);
            throw new FinnhubException("Search 조회 중 오류 발생: " + e.getMessage(), e);
        }
    }

    //------------------------------------------
    // 시장 뉴스 조회 (GET /api/v1/news)
    //------------------------------------------
    // 파라미터:
    //   - category: 카테고리 (general, forex, crypto, merger)
    //------------------------------------------
    public List<NewsItem> getNews(String category) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/news")
                    .queryParam("category", category)
                    .queryParam("token", apiKey)
                    .toUriString();

            log.debug("Finnhub API 호출 (News): category={}", category);

            ResponseEntity<NewsItem[]> response = restTemplate.getForEntity(
                    url, NewsItem[].class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<NewsItem> news = List.of(response.getBody());
                log.debug("Finnhub API 응답 (News): count={}", news.size());
                return news;
            } else {
                log.warn("Finnhub API 응답 실패 (News): status={}, body={}", 
                        response.getStatusCode(), response.getBody());
                return new ArrayList<>();
            }
        } catch (RestClientException e) {
            log.error("Finnhub API 호출 실패 (News): category={}", category, e);
            return new ArrayList<>();
        }
    }

    //------------------------------------------
    // Finnhub API 예외 클래스
    //------------------------------------------
    public static class FinnhubException extends RuntimeException {
        public FinnhubException(String message) {
            super(message);
        }

        public FinnhubException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
