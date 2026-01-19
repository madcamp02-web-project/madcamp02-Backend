package com.madcamp02.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.madcamp02.util.StompDestinationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * TradePriceBroadcastService
 *
 * Finnhub에서 수신한 trade 메시지를 정규화하여
 * Redis 캐시 및 STOMP 브로커로 전파하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradePriceBroadcastService {

    private static final String REDIS_KEY_PREFIX = "stock:price:";
    private static final Duration REDIS_TTL = Duration.ofHours(24); // 24시간 TTL

    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // ObjectMapper는 Spring Boot가 자동으로 Bean으로 등록하므로 생성자 주입 가능
    // 만약 Bean이 없다면 AppConfig에 @Bean으로 등록 필요

    /**
     * Trade 메시지를 정규화하여 브로드캐스트
     *
     * @param symbol 종목 심볼
     * @param price 최신 체결가
     * @param timestamp UNIX 타임스탬프 (밀리초)
     * @param volume 거래량 (trade 미지원 시 0)
     * @param conditions 거래 조건 코드 배열 (optional)
     */
    public void broadcastTrade(String symbol, double price, long timestamp, double volume, String[] conditions) {
        if (symbol == null || symbol.isEmpty()) {
            log.debug("빈 심볼로 브로드캐스트 무시");
            return;
        }

        // 정규화된 payload 생성
        Map<String, Object> payload = new HashMap<>();
        payload.put("ticker", symbol);
        payload.put("price", price);
        payload.put("ts", timestamp);
        payload.put("volume", volume);
        payload.put("source", "FINNHUB");
        payload.put("rawType", "trade");
        if (conditions != null && conditions.length > 0) {
            payload.put("conditions", conditions);
        }

        // Redis 캐시 업데이트
        updateRedisCache(symbol, payload);

        // STOMP 브로드캐스트
        broadcastToStomp(symbol, payload);
    }

    /**
     * Redis 캐시 업데이트
     */
    private void updateRedisCache(String symbol, Map<String, Object> payload) {
        try {
            String key = REDIS_KEY_PREFIX + symbol;
            String value = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(key, value, REDIS_TTL);
            log.debug("Redis 캐시 업데이트: {} = {}", key, value);
        } catch (JsonProcessingException e) {
            log.error("Redis 캐시 업데이트 실패: symbol={}", symbol, e);
        }
    }

    /**
     * STOMP 브로드캐스트
     */
    private void broadcastToStomp(String symbol, Map<String, Object> payload) {
        try {
            // STOMP destination: /topic/stock.ticker.{ticker}
            // 현재 정책: ticker를 그대로 사용 (공백/특수문자 허용)
            // 예: "IC MARKETS:1" -> "/topic/stock.ticker.IC MARKETS:1"
            // 향후 URL 인코딩이 필요한 경우 StompDestinationUtils.createEncodedDestination() 사용
            String destination = StompDestinationUtils.createDestination(symbol);
            messagingTemplate.convertAndSend(destination, payload);
            log.debug("STOMP 브로드캐스트: {} -> {}", destination, payload);
        } catch (Exception e) {
            log.error("STOMP 브로드캐스트 실패: symbol={}", symbol, e);
        }
    }

    /**
     * Redis에서 최신가 조회
     */
    public Map<String, Object> getLatestPrice(String symbol) {
        try {
            String key = REDIS_KEY_PREFIX + symbol;
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                return objectMapper.readValue(value, Map.class);
            }
        } catch (Exception e) {
            log.error("Redis에서 최신가 조회 실패: symbol={}", symbol, e);
        }
        return null;
    }
}
