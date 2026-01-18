package com.madcamp02.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madcamp02.dto.response.StockCandlesResponse;
import com.madcamp02.dto.response.StockQuoteResponse;
import com.madcamp02.dto.response.StockSearchResponse;
import com.madcamp02.external.FinnhubClient;
import com.madcamp02.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * StockController 테스트
 * Phase 3 요구사항 검증:
 * - GET /api/v1/stock/search?keyword={keyword}
 * - GET /api/v1/stock/quote/{ticker}
 * - GET /api/v1/stock/candles/{ticker}?resolution={D}&from={timestamp}&to={timestamp}
 */
@WebMvcTest(controllers = StockController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
        })
@DisplayName("Stock API 테스트")
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StockService stockService;

    @MockBean
    private FinnhubClient finnhubClient;

    private StockSearchResponse mockSearchResponse;
    private StockQuoteResponse mockQuoteResponse;
    private StockCandlesResponse mockCandlesResponse;

    @BeforeEach
    void setUp() {
        // Mock Search Response
        StockSearchResponse.Item searchItem = StockSearchResponse.Item.builder()
                .symbol("AAPL")
                .description("Apple Inc")
                .displaySymbol("AAPL")
                .type("Common Stock")
                .build();

        mockSearchResponse = StockSearchResponse.builder()
                .items(Arrays.asList(searchItem))
                .build();

        // Mock Quote Response
        mockQuoteResponse = StockQuoteResponse.builder()
                .ticker("AAPL")
                .currentPrice(150.0)
                .open(148.0)
                .high(152.0)
                .low(147.0)
                .previousClose(149.0)
                .change(1.0)
                .changePercent(0.67)
                .timestamp(1705564800L)
                .build();

        // Mock Candles Response (빈 응답 - Premium API 제한)
        mockCandlesResponse = StockCandlesResponse.builder()
                .ticker("AAPL")
                .resolution("D")
                .items(Arrays.asList())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/stock/search - 종목 검색 성공")
    void testSearchStock() throws Exception {
        // Given
        String keyword = "Apple";
        when(stockService.searchStock(keyword)).thenReturn(mockSearchResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/stock/search")
                        .param("keyword", keyword))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$.items[0].description").value("Apple Inc"))
                .andExpect(jsonPath("$.items[0].type").value("Common Stock"));
    }

    @Test
    @DisplayName("GET /api/v1/stock/quote/{ticker} - 현재가 조회 성공")
    void testGetQuote() throws Exception {
        // Given
        String ticker = "AAPL";
        when(stockService.getQuote(ticker)).thenReturn(mockQuoteResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/stock/quote/{ticker}", ticker))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.ticker").value("AAPL"))
                .andExpect(jsonPath("$.currentPrice").value(150.0))
                .andExpect(jsonPath("$.open").value(148.0))
                .andExpect(jsonPath("$.high").value(152.0))
                .andExpect(jsonPath("$.low").value(147.0))
                .andExpect(jsonPath("$.previousClose").value(149.0))
                .andExpect(jsonPath("$.change").value(1.0))
                .andExpect(jsonPath("$.changePercent").value(0.67))
                .andExpect(jsonPath("$.timestamp").value(1705564800L));
    }

    @Test
    @DisplayName("GET /api/v1/stock/candles/{ticker} - 캔들 차트 데이터 조회 (Premium API 제한으로 빈 응답)")
    void testGetCandles() throws Exception {
        // Given
        String ticker = "AAPL";
        String resolution = "D";
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 18, 0, 0);

        when(stockService.getCandles(eq(ticker), eq(resolution), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockCandlesResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/stock/candles/{ticker}", ticker)
                        .param("resolution", resolution)
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.ticker").value("AAPL"))
                .andExpect(jsonPath("$.resolution").value("D"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/stock/search - 인증 없이 접근 가능 (Public API)")
    void testSearchStockWithoutAuth() throws Exception {
        // Given
        String keyword = "Apple";
        when(stockService.searchStock(keyword)).thenReturn(mockSearchResponse);

        // When & Then - 인증 헤더 없이도 접근 가능해야 함
        mockMvc.perform(get("/api/v1/stock/search")
                        .param("keyword", keyword))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/stock/search - keyword 파라미터 필수")
    void testSearchStockWithoutKeyword() throws Exception {
        // When & Then - keyword 파라미터가 없으면 400 Bad Request
        mockMvc.perform(get("/api/v1/stock/search"))
                .andExpect(status().isBadRequest());
    }
}
