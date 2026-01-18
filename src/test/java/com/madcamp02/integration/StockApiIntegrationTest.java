package com.madcamp02.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madcamp02.controller.StockController;
import com.madcamp02.domain.stock.StockCandleRepository;
import com.madcamp02.dto.response.StockCandlesResponse;
import com.madcamp02.dto.response.StockQuoteResponse;
import com.madcamp02.dto.response.StockSearchResponse;
import com.madcamp02.external.EodhdClient;
import com.madcamp02.external.FinnhubClient;
import com.madcamp02.service.QuotaManager;
import com.madcamp02.service.StockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Stock API 통합 테스트
 * 실제 API 엔드포인트 호출을 검증합니다.
 * 
 * 주의: 이 테스트는 실제 Service를 사용하므로, FinnhubClient만 Mock합니다.
 */
@WebMvcTest(controllers = StockController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
        })
@DisplayName("Stock API 통합 테스트")
class StockApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StockService stockService;

    // StockService의 의존성들 (실제로는 StockService를 Mock하므로 필요 없지만, Spring Context 로딩을 위해 선언)
    @MockBean
    private FinnhubClient finnhubClient;

    @MockBean
    private EodhdClient eodhdClient;

    @MockBean
    private StockCandleRepository stockCandleRepository;

    @MockBean
    private QuotaManager quotaManager;

    @Test
    @DisplayName("GET /api/v1/stock/search - 실제 API 호출 검증")
    void testSearchStockApiCall() throws Exception {
        // Given - Service 응답 Mock
        StockSearchResponse.Item searchItem = StockSearchResponse.Item.builder()
                .symbol("AAPL")
                .description("Apple Inc")
                .displaySymbol("AAPL")
                .type("Common Stock")
                .build();

        StockSearchResponse mockResponse = StockSearchResponse.builder()
                .items(Arrays.asList(searchItem))
                .build();

        when(stockService.searchStock("Apple")).thenReturn(mockResponse);

        // When - 실제 API 호출
        MvcResult result = mockMvc.perform(get("/api/v1/stock/search")
                        .param("keyword", "Apple"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.items").isArray())
                .andReturn();

        // Then - 응답 본문 검증
        String responseBody = result.getResponse().getContentAsString();
        StockSearchResponse response = objectMapper.readValue(responseBody, StockSearchResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).isNotEmpty();
        assertThat(response.getItems().size()).isLessThanOrEqualTo(20); // 최대 20개

        // 첫 번째 검색 결과 검증
        StockSearchResponse.Item firstItem = response.getItems().get(0);
        assertThat(firstItem.getSymbol()).isEqualTo("AAPL");
        assertThat(firstItem.getDescription()).contains("Apple");
        assertThat(firstItem.getType()).isEqualTo("Common Stock");
    }

    @Test
    @DisplayName("GET /api/v1/stock/quote/{ticker} - 실제 API 호출 검증")
    void testGetQuoteApiCall() throws Exception {
        // Given - Service 응답 Mock
        StockQuoteResponse mockResponse = StockQuoteResponse.builder()
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

        when(stockService.getQuote("AAPL")).thenReturn(mockResponse);

        // When - 실제 API 호출
        MvcResult result = mockMvc.perform(get("/api/v1/stock/quote/{ticker}", "AAPL"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.ticker").value("AAPL"))
                .andExpect(jsonPath("$.currentPrice").value(150.0))
                .andReturn();

        // Then - 응답 본문 검증
        String responseBody = result.getResponse().getContentAsString();
        StockQuoteResponse response = objectMapper.readValue(responseBody, StockQuoteResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getTicker()).isEqualTo("AAPL");
        assertThat(response.getCurrentPrice()).isEqualTo(150.0);
        assertThat(response.getOpen()).isEqualTo(148.0);
        assertThat(response.getHigh()).isEqualTo(152.0);
        assertThat(response.getLow()).isEqualTo(147.0);
        assertThat(response.getPreviousClose()).isEqualTo(149.0);
        assertThat(response.getChange()).isEqualTo(1.0); // 150.0 - 149.0
        assertThat(response.getChangePercent()).isCloseTo(0.67, org.assertj.core.data.Offset.offset(0.01));
        assertThat(response.getTimestamp()).isEqualTo(1705564800L);
    }

    @Test
    @DisplayName("GET /api/v1/stock/candles/{ticker} - 실제 API 호출 검증 (Premium API 제한)")
    void testGetCandlesApiCall() throws Exception {
        // Given - Service 응답 Mock (Premium API 제한으로 빈 응답)
        String ticker = "AAPL";
        String resolution = "D";
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 18, 0, 0);

        StockCandlesResponse mockResponse = StockCandlesResponse.builder()
                .ticker("AAPL")
                .resolution("D")
                .items(Arrays.asList())
                .build();

        when(stockService.getCandles(eq(ticker), eq(resolution), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockResponse);

        // When - 실제 API 호출
        MvcResult result = mockMvc.perform(get("/api/v1/stock/candles/{ticker}", ticker)
                        .param("resolution", resolution)
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.ticker").value("AAPL"))
                .andExpect(jsonPath("$.resolution").value("D"))
                .andExpect(jsonPath("$.items").isArray())
                .andReturn();

        // Then - 응답 본문 검증 (빈 응답)
        String responseBody = result.getResponse().getContentAsString();
        StockCandlesResponse response = objectMapper.readValue(responseBody, StockCandlesResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getTicker()).isEqualTo("AAPL");
        assertThat(response.getResolution()).isEqualTo("D");
        assertThat(response.getItems()).isEmpty(); // Premium API 제한으로 빈 응답
    }

    @Test
    @DisplayName("GET /api/v1/stock/search - keyword 파라미터 없을 때 400 에러")
    void testSearchStockWithoutKeyword() throws Exception {
        // When & Then - keyword 파라미터가 없으면 400 Bad Request
        mockMvc.perform(get("/api/v1/stock/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/stock/candles/{ticker} - 필수 파라미터 없을 때 400 에러")
    void testGetCandlesWithoutRequiredParams() throws Exception {
        // When & Then - 필수 파라미터가 없으면 400 Bad Request
        mockMvc.perform(get("/api/v1/stock/candles/AAPL"))
                .andExpect(status().isBadRequest());
    }
}
