package com.madcamp02.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradePriceBroadcastServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TradePriceBroadcastService broadcastService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("정상적인 trade 메시지 브로드캐스트")
    void testBroadcastTrade() {
        // Given
        String symbol = "AAPL";
        double price = 195.12;
        long timestamp = 1705672800000L;
        double volume = 1000.0;
        String[] conditions = new String[]{"C", "F"};

        // When
        broadcastService.broadcastTrade(symbol, price, timestamp, volume, conditions);

        // Then
        // Redis 캐시 업데이트 확인
        ArgumentCaptor<String> redisKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> redisValueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations, times(1)).set(
                redisKeyCaptor.capture(),
                redisValueCaptor.capture(),
                any(java.time.Duration.class)
        );

        assertEquals("stock:price:AAPL", redisKeyCaptor.getValue());
        assertNotNull(redisValueCaptor.getValue());

        // STOMP 브로드캐스트 확인
        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate, times(1)).convertAndSend(
                destinationCaptor.capture(),
                payloadCaptor.capture()
        );

        assertEquals("/topic/stock.ticker.AAPL", destinationCaptor.getValue());
        assertTrue(payloadCaptor.getValue() instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertEquals("AAPL", payload.get("ticker"));
        assertEquals(195.12, payload.get("price"));
        assertEquals(1705672800000L, payload.get("ts"));
        assertEquals(1000.0, payload.get("volume"));
        assertEquals("FINNHUB", payload.get("source"));
        assertEquals("trade", payload.get("rawType"));
        assertArrayEquals(conditions, ((java.util.List<?>) payload.get("conditions")).toArray());
    }

    @Test
    @DisplayName("volume=0인 price update 브로드캐스트")
    void testBroadcastPriceUpdateWithZeroVolume() {
        // Given
        String symbol = "BINANCE:BTCUSDT";
        double price = 7296.89;
        long timestamp = 1705672800000L;
        double volume = 0.0; // Trade 미지원 시 volume=0
        String[] conditions = null;

        // When
        broadcastService.broadcastTrade(symbol, price, timestamp, volume, conditions);

        // Then
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/stock.ticker.BINANCE:BTCUSDT"),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("빈 심볼로 브로드캐스트 시 무시")
    void testBroadcastWithEmptySymbol() {
        // When
        broadcastService.broadcastTrade("", 100.0, 1705672800000L, 100.0, null);

        // Then
        verify(messagingTemplate, never()).convertAndSend(anyString(), any());
        verify(valueOperations, never()).set(anyString(), anyString(), any(java.time.Duration.class));
    }

    @Test
    @DisplayName("공백/특수문자가 포함된 ticker 처리")
    void testBroadcastWithSpecialCharacters() {
        // Given
        String symbol = "IC MARKETS:1"; // 공백과 콜론 포함

        // When
        broadcastService.broadcastTrade(symbol, 1.2345, 1705672800000L, 0.0, null);

        // Then
        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate, times(1)).convertAndSend(
                destinationCaptor.capture(),
                any(Map.class)
        );

        // 공백/특수문자가 그대로 포함된 destination 확인
        assertEquals("/topic/stock.ticker.IC MARKETS:1", destinationCaptor.getValue());
    }
}
