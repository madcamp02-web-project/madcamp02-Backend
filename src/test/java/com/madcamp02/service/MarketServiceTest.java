package com.madcamp02.service;

import com.madcamp02.dto.response.MarketIndicesResponse;
import com.madcamp02.dto.response.MarketMoversResponse;
import com.madcamp02.dto.response.MarketNewsResponse;
import com.madcamp02.external.FinnhubClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * MarketService 테스트
 * Phase 3 요구사항 검증:
 * - 지수 조회 로직
 * - 뉴스 조회 로직
 * - 급등/급락 종목 조회 로직
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MarketService 테스트")
class MarketServiceTest {

    @Mock
    private FinnhubClient finnhubClient;

    @InjectMocks
    private MarketService marketService;

    private FinnhubClient.QuoteResponse mockQuoteResponse;
    private FinnhubClient.NewsItem mockNewsItem;
    private FinnhubClient.SearchResponse mockSearchResponse;

    @BeforeEach
    void setUp() {
        // Mock Quote Response
        mockQuoteResponse = FinnhubClient.QuoteResponse.builder()
                .currentPrice(15000.0)
                .previousClose(14900.0)
                .high(15100.0)
                .low(14800.0)
                .open(14950.0)
                .timestamp(1705564800L)
                .build();

        // Mock News Item
        mockNewsItem = FinnhubClient.NewsItem.builder()
                .id(12345)
                .headline("Test News Headline")
                .summary("Test News Summary")
                .source("Test Source")
                .url("https://example.com/news")
                .image("https://example.com/image.jpg")
                .datetime(1705564800L)
                .build();

        // Mock Search Response
        FinnhubClient.SearchResult searchResult = FinnhubClient.SearchResult.builder()
                .symbol("AAPL")
                .description("Apple Inc")
                .displaySymbol("AAPL")
                .type("Common Stock")
                .build();

        mockSearchResponse = FinnhubClient.SearchResponse.builder()
                .count(1)
                .result(Arrays.asList(searchResult))
                .build();
    }

    @Test
    @DisplayName("getIndices - 주요 지수 조회 성공")
    void testGetIndices() {
        // Given
        when(finnhubClient.getQuote("^IXIC")).thenReturn(mockQuoteResponse);
        when(finnhubClient.getQuote("^GSPC")).thenReturn(mockQuoteResponse);
        when(finnhubClient.getQuote("^DJI")).thenReturn(mockQuoteResponse);

        // When
        MarketIndicesResponse response = marketService.getIndices();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAsOf()).isNotNull();
        assertThat(response.getItems()).isNotEmpty();
        assertThat(response.getItems().size()).isGreaterThan(0);
        
        // 첫 번째 지수 검증
        MarketIndicesResponse.Item firstItem = response.getItems().get(0);
        assertThat(firstItem.getCode()).isIn("NASDAQ", "SP500", "DJI");
        assertThat(firstItem.getValue()).isNotNull();
        assertThat(firstItem.getCurrency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("getNews - 시장 뉴스 조회 성공")
    void testGetNews() {
        // Given
        when(finnhubClient.getNews("general")).thenReturn(Arrays.asList(mockNewsItem));

        // When
        MarketNewsResponse response = marketService.getNews();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAsOf()).isNotNull();
        assertThat(response.getItems()).isNotEmpty();
        
        MarketNewsResponse.Item firstItem = response.getItems().get(0);
        assertThat(firstItem.getId()).isEqualTo("finnhub:12345");
        assertThat(firstItem.getHeadline()).isEqualTo("Test News Headline");
        assertThat(firstItem.getSummary()).isEqualTo("Test News Summary");
        assertThat(firstItem.getSource()).isEqualTo("Test Source");
    }

    @Test
    @DisplayName("getMovers - 급등/급락 종목 조회 성공")
    void testGetMovers() {
        // Given
        when(finnhubClient.getQuote(any(String.class))).thenReturn(mockQuoteResponse);
        when(finnhubClient.searchSymbol(any(String.class))).thenReturn(mockSearchResponse);

        // When
        MarketMoversResponse response = marketService.getMovers();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAsOf()).isNotNull();
        assertThat(response.getItems()).isNotEmpty();
        
        // 상위 5개만 반환되는지 확인
        assertThat(response.getItems().size()).isLessThanOrEqualTo(5);
        
        // 첫 번째 종목 검증
        MarketMoversResponse.Item firstItem = response.getItems().get(0);
        assertThat(firstItem.getTicker()).isNotNull();
        assertThat(firstItem.getPrice()).isNotNull();
        assertThat(firstItem.getChangePercent()).isNotNull();
        assertThat(firstItem.getDirection()).isIn(MarketMoversResponse.Direction.UP, MarketMoversResponse.Direction.DOWN);
    }

    @Test
    @DisplayName("getIndices - 일부 지수 조회 실패해도 다른 지수는 계속 조회")
    void testGetIndicesWithPartialFailure() {
        // Given
        when(finnhubClient.getQuote("^IXIC")).thenReturn(mockQuoteResponse);
        when(finnhubClient.getQuote("^GSPC")).thenThrow(new RuntimeException("API Error"));
        when(finnhubClient.getQuote("^DJI")).thenReturn(mockQuoteResponse);

        // When
        MarketIndicesResponse response = marketService.getIndices();

        // Then - 일부 실패해도 성공한 지수는 반환되어야 함
        assertThat(response).isNotNull();
        assertThat(response.getItems()).isNotEmpty();
        assertThat(response.getItems().size()).isGreaterThan(0);
    }
}
