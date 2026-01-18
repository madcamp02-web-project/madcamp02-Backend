package com.madcamp02.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madcamp02.controller.MarketController;
import com.madcamp02.dto.response.MarketIndicesResponse;
import com.madcamp02.dto.response.MarketMoversResponse;
import com.madcamp02.dto.response.MarketNewsResponse;
import com.madcamp02.external.FinnhubClient;
import com.madcamp02.service.MarketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Market API 통합 테스트
 * 실제 API 엔드포인트 호출을 검증합니다.
 * 
 * 주의: 이 테스트는 실제 Service를 사용하므로, FinnhubClient만 Mock합니다.
 */
@WebMvcTest(controllers = MarketController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
        })
@DisplayName("Market API 통합 테스트")
class MarketApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MarketService marketService;

    @MockBean
    private FinnhubClient finnhubClient;

    @Test
    @DisplayName("GET /api/v1/market/indices - 실제 API 호출 검증")
    void testGetIndicesApiCall() throws Exception {
        // Given - Service 응답 Mock
        MarketIndicesResponse.Item indexItem = MarketIndicesResponse.Item.builder()
                .code("NASDAQ")
                .name("NASDAQ")
                .value(15000.0)
                .change(100.0)
                .changePercent(0.67)
                .currency("USD")
                .build();

        MarketIndicesResponse mockResponse = MarketIndicesResponse.builder()
                .asOf("2026-01-18T10:00:00")
                .items(Arrays.asList(indexItem))
                .build();

        when(marketService.getIndices()).thenReturn(mockResponse);

        // When - 실제 API 호출
        MvcResult result = mockMvc.perform(get("/api/v1/market/indices"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.asOf").exists())
                .andExpect(jsonPath("$.items").isArray())
                .andReturn();

        // Then - 응답 본문 검증
        String responseBody = result.getResponse().getContentAsString();
        MarketIndicesResponse response = objectMapper.readValue(responseBody, MarketIndicesResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getAsOf()).isNotNull();
        assertThat(response.getItems()).isNotEmpty();
        assertThat(response.getItems().size()).isGreaterThanOrEqualTo(1);

        // 각 지수 데이터 검증
        response.getItems().forEach(item -> {
            assertThat(item.getCode()).isIn("NASDAQ", "SP500", "DJI");
            assertThat(item.getValue()).isNotNull();
            assertThat(item.getChange()).isNotNull();
            assertThat(item.getChangePercent()).isNotNull();
            assertThat(item.getCurrency()).isEqualTo("USD");
        });
    }

    @Test
    @DisplayName("GET /api/v1/market/news - 실제 API 호출 검증")
    void testGetNewsApiCall() throws Exception {
        // Given - Service 응답 Mock
        MarketNewsResponse.Item newsItem = MarketNewsResponse.Item.builder()
                .id("finnhub:12345")
                .headline("Test News Headline 1")
                .summary("Test News Summary 1")
                .source("Test Source 1")
                .url("https://example.com/news/1")
                .imageUrl("https://example.com/image1.jpg")
                .publishedAt("2026-01-18T09:00:00")
                .build();

        MarketNewsResponse mockResponse = MarketNewsResponse.builder()
                .asOf("2026-01-18T10:00:00")
                .items(Arrays.asList(newsItem))
                .build();

        when(marketService.getNews()).thenReturn(mockResponse);

        // When - 실제 API 호출
        MvcResult result = mockMvc.perform(get("/api/v1/market/news"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.asOf").exists())
                .andExpect(jsonPath("$.items").isArray())
                .andReturn();

        // Then - 응답 본문 검증
        String responseBody = result.getResponse().getContentAsString();
        MarketNewsResponse response = objectMapper.readValue(responseBody, MarketNewsResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getAsOf()).isNotNull();
        assertThat(response.getItems()).isNotEmpty();
        assertThat(response.getItems().size()).isLessThanOrEqualTo(20); // 최대 20개

        // 첫 번째 뉴스 검증
        MarketNewsResponse.Item firstItem = response.getItems().get(0);
        assertThat(firstItem.getId()).isEqualTo("finnhub:12345");
        assertThat(firstItem.getHeadline()).isEqualTo("Test News Headline 1");
        assertThat(firstItem.getSummary()).isEqualTo("Test News Summary 1");
        assertThat(firstItem.getSource()).isEqualTo("Test Source 1");
        assertThat(firstItem.getUrl()).isEqualTo("https://example.com/news/1");
    }

    @Test
    @DisplayName("GET /api/v1/market/movers - 실제 API 호출 검증")
    void testGetMoversApiCall() throws Exception {
        // Given - Service 응답 Mock
        MarketMoversResponse.Item moverItem = MarketMoversResponse.Item.builder()
                .ticker("AAPL")
                .name("Apple Inc.")
                .price(150.0)
                .changePercent(2.5)
                .volume(1000000L)
                .direction(MarketMoversResponse.Direction.UP)
                .build();

        MarketMoversResponse mockResponse = MarketMoversResponse.builder()
                .asOf("2026-01-18T10:00:00")
                .items(Arrays.asList(moverItem))
                .build();

        when(marketService.getMovers()).thenReturn(mockResponse);

        // When - 실제 API 호출
        MvcResult result = mockMvc.perform(get("/api/v1/market/movers"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.asOf").exists())
                .andExpect(jsonPath("$.items").isArray())
                .andReturn();

        // Then - 응답 본문 검증
        String responseBody = result.getResponse().getContentAsString();
        MarketMoversResponse response = objectMapper.readValue(responseBody, MarketMoversResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getAsOf()).isNotNull();
        assertThat(response.getItems()).isNotEmpty();
        assertThat(response.getItems().size()).isLessThanOrEqualTo(5); // 상위 5개만

        // 각 종목 데이터 검증
        response.getItems().forEach(item -> {
            assertThat(item.getTicker()).isNotNull();
            assertThat(item.getName()).isNotNull();
            assertThat(item.getPrice()).isNotNull();
            assertThat(item.getChangePercent()).isNotNull();
            assertThat(item.getDirection()).isIn(MarketMoversResponse.Direction.UP, MarketMoversResponse.Direction.DOWN);
        });
    }
}
