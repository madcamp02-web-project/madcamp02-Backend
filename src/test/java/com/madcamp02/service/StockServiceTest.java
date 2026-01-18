package com.madcamp02.service;

import com.madcamp02.domain.stock.StockCandleRepository;
import com.madcamp02.dto.response.StockCandlesResponse;
import com.madcamp02.dto.response.StockQuoteResponse;
import com.madcamp02.dto.response.StockSearchResponse;
import com.madcamp02.external.EodhdClient;
import com.madcamp02.external.FinnhubClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * StockService 테스트
 * Phase 3 요구사항 검증:
 * - 종목 검색 로직
 * - 현재가 조회 로직
 * - 캔들 차트 데이터 조회 로직 (Premium API 제한으로 빈 응답)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StockService 테스트")
class StockServiceTest {

    @Mock
    private FinnhubClient finnhubClient;

    @Mock
    private EodhdClient eodhdClient;

    @Mock
    private StockCandleRepository stockCandleRepository;

    @Mock
    private QuotaManager quotaManager;

    @InjectMocks
    private StockService stockService;

    private FinnhubClient.SearchResponse mockSearchResponse;
    private FinnhubClient.QuoteResponse mockQuoteResponse;

    @BeforeEach
    void setUp() {
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

        // Mock Quote Response
        mockQuoteResponse = FinnhubClient.QuoteResponse.builder()
                .currentPrice(150.0)
                .previousClose(149.0)
                .high(152.0)
                .low(147.0)
                .open(148.0)
                .timestamp(1705564800L)
                .build();
    }

    @Test
    @DisplayName("searchStock - 종목 검색 성공")
    void testSearchStock() {
        // Given
        String keyword = "Apple";
        when(finnhubClient.searchSymbol(keyword)).thenReturn(mockSearchResponse);

        // When
        StockSearchResponse response = stockService.searchStock(keyword);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getItems()).isNotEmpty();
        assertThat(response.getItems().size()).isLessThanOrEqualTo(20); // 최대 20개
        
        StockSearchResponse.Item firstItem = response.getItems().get(0);
        assertThat(firstItem.getSymbol()).isEqualTo("AAPL");
        assertThat(firstItem.getDescription()).isEqualTo("Apple Inc");
        assertThat(firstItem.getType()).isEqualTo("Common Stock");
    }

    @Test
    @DisplayName("getQuote - 현재가 조회 성공")
    void testGetQuote() {
        // Given
        String ticker = "AAPL";
        when(finnhubClient.getQuote(ticker)).thenReturn(mockQuoteResponse);

        // When
        StockQuoteResponse response = stockService.getQuote(ticker);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTicker()).isEqualTo("AAPL");
        assertThat(response.getCurrentPrice()).isEqualTo(150.0);
        assertThat(response.getOpen()).isEqualTo(148.0);
        assertThat(response.getHigh()).isEqualTo(152.0);
        assertThat(response.getLow()).isEqualTo(147.0);
        assertThat(response.getPreviousClose()).isEqualTo(149.0);
        assertThat(response.getChange()).isEqualTo(1.0); // 150.0 - 149.0
        assertThat(response.getChangePercent()).isCloseTo(0.67, org.assertj.core.data.Offset.offset(0.01)); // (1.0 / 149.0) * 100
        assertThat(response.getTimestamp()).isEqualTo(1705564800L);
    }

    @Test
    @DisplayName("getCandles - DB에 데이터 없고 Quota 초과로 빈 응답 반환")
    void testGetCandles() {
        // Given
        String ticker = "AAPL";
        String resolution = "D";
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 18, 0, 0);

        // DB에 데이터 없음
        when(stockCandleRepository.findAllBySymbolAndDateBetweenOrderByDateAsc(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        
        // Quota 초과
        when(quotaManager.checkQuota("EODHD")).thenReturn(false);

        // When
        StockCandlesResponse response = stockService.getCandles(ticker, resolution, from, to);

        // Then - DB에 데이터 없고 Quota 초과이므로 빈 응답 반환
        assertThat(response).isNotNull();
        assertThat(response.getTicker()).isEqualTo("AAPL");
        assertThat(response.getResolution()).isEqualTo("D");
        assertThat(response.getItems()).isEmpty(); // 빈 리스트 반환
    }

    @Test
    @DisplayName("getQuote - changePercent 계산 검증 (0으로 나누기 방지)")
    void testGetQuoteWithZeroPreviousClose() {
        // Given
        String ticker = "AAPL";
        FinnhubClient.QuoteResponse quoteWithZero = FinnhubClient.QuoteResponse.builder()
                .currentPrice(150.0)
                .previousClose(0.0) // 0으로 나누기 방지 테스트
                .high(152.0)
                .low(147.0)
                .open(148.0)
                .timestamp(1705564800L)
                .build();
        when(finnhubClient.getQuote(ticker)).thenReturn(quoteWithZero);

        // When
        StockQuoteResponse response = stockService.getQuote(ticker);

        // Then - changePercent가 0이어야 함 (0으로 나누기 방지)
        assertThat(response).isNotNull();
        assertThat(response.getChangePercent()).isEqualTo(0.0);
    }
}
