package com.madcamp02.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madcamp02.dto.response.MarketIndicesResponse;
import com.madcamp02.dto.response.MarketMoversResponse;
import com.madcamp02.dto.response.MarketNewsResponse;
import com.madcamp02.external.FinnhubClient;
import com.madcamp02.service.MarketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MarketController 테스트
 * Phase 3 요구사항 검증:
 * - GET /api/v1/market/indices
 * - GET /api/v1/market/news
 * - GET /api/v1/market/movers
 */
@WebMvcTest(controllers = MarketController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration.class,
                org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = com.madcamp02.config.CacheConfig.class
        ))
@TestPropertySource(properties = {
        "spring.cache.type=none"
})
@DisplayName("Market API 테스트")
class MarketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MarketService marketService;

    @MockBean
    private FinnhubClient finnhubClient;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private org.springframework.web.client.RestTemplate restTemplate;

    private MarketIndicesResponse mockIndicesResponse;
    private MarketNewsResponse mockNewsResponse;
    private MarketMoversResponse mockMoversResponse;

    @BeforeEach
    void setUp() {
        // Mock Indices Response
        MarketIndicesResponse.Item indexItem = MarketIndicesResponse.Item.builder()
                .code("NASDAQ")
                .name("NASDAQ")
                .value(15000.0)
                .change(100.0)
                .changePercent(0.67)
                .currency("USD")
                .build();

        mockIndicesResponse = MarketIndicesResponse.builder()
                .asOf("2026-01-18T10:00:00")
                .items(Arrays.asList(indexItem))
                .build();

        // Mock News Response
        MarketNewsResponse.Item newsItem = MarketNewsResponse.Item.builder()
                .id("finnhub:12345")
                .headline("Test News Headline")
                .summary("Test News Summary")
                .source("Test Source")
                .url("https://example.com/news")
                .imageUrl("https://example.com/image.jpg")
                .publishedAt("2026-01-18T09:00:00")
                .build();

        mockNewsResponse = MarketNewsResponse.builder()
                .asOf("2026-01-18T10:00:00")
                .items(Arrays.asList(newsItem))
                .build();

        // Mock Movers Response
        MarketMoversResponse.Item moverItem = MarketMoversResponse.Item.builder()
                .ticker("AAPL")
                .name("Apple Inc.")
                .price(150.0)
                .changePercent(2.5)
                .volume(1000000L)
                .direction(MarketMoversResponse.Direction.UP)
                .build();

        mockMoversResponse = MarketMoversResponse.builder()
                .asOf("2026-01-18T10:00:00")
                .items(Arrays.asList(moverItem))
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/market/indices - 주요 지수 조회 성공")
    void testGetIndices() throws Exception {
        // Given
        when(marketService.getIndices()).thenReturn(mockIndicesResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/market/indices"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.asOf").exists())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].code").value("NASDAQ"))
                .andExpect(jsonPath("$.items[0].value").value(15000.0))
                .andExpect(jsonPath("$.items[0].changePercent").value(0.67))
                .andExpect(jsonPath("$.items[0].currency").value("USD"));
    }

    @Test
    @DisplayName("GET /api/v1/market/news - 시장 뉴스 조회 성공")
    void testGetNews() throws Exception {
        // Given
        when(marketService.getNews()).thenReturn(mockNewsResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/market/news"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.asOf").exists())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].id").value("finnhub:12345"))
                .andExpect(jsonPath("$.items[0].headline").value("Test News Headline"))
                .andExpect(jsonPath("$.items[0].summary").value("Test News Summary"))
                .andExpect(jsonPath("$.items[0].source").value("Test Source"));
    }

    @Test
    @DisplayName("GET /api/v1/market/movers - 급등/급락 종목 조회 성공")
    void testGetMovers() throws Exception {
        // Given
        when(marketService.getMovers()).thenReturn(mockMoversResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/market/movers"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.asOf").exists())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$.items[0].name").value("Apple Inc."))
                .andExpect(jsonPath("$.items[0].price").value(150.0))
                .andExpect(jsonPath("$.items[0].changePercent").value(2.5))
                .andExpect(jsonPath("$.items[0].direction").value("UP"));
    }

    @Test
    @DisplayName("GET /api/v1/market/indices - 인증 없이 접근 가능 (Public API)")
    void testGetIndicesWithoutAuth() throws Exception {
        // Given
        when(marketService.getIndices()).thenReturn(mockIndicesResponse);

        // When & Then - 인증 헤더 없이도 접근 가능해야 함
        mockMvc.perform(get("/api/v1/market/indices"))
                .andExpect(status().isOk());
    }
}
