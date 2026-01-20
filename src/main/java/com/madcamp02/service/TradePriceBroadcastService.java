package com.madcamp02.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.madcamp02.util.StompDestinationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

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
    @Qualifier("tradeBroadcastExecutor")
    private final ExecutorService executorService;

    // ObjectMapper는 Spring Boot가 자동으로 Bean으로 등록하므로 생성자 주입 가능
    // 만약 Bean이 없다면 AppConfig에 @Bean으로 등록 필요

    /**
     * Trade 메시지를 정규화하여 브로드캐스트
     * 
     * Redis 캐시 업데이트와 STOMP 브로드캐스트를 비동기로 병렬 처리하여
     * 웹소켓 메시지 처리 속도를 향상시킵니다.
     *
     * @param symbol 종목 심볼
     * @param price 최신 체결가
     * @param timestamp UNIX 타임스탬프 (밀리초)
     * @param volume 거래량 (trade 미지원 시 0)
     * @param conditions 거래 조건 코드 배열 (optional)
     */
    public void broadcastTrade(String symbol, double price, long timestamp, double volume, String[] conditions) {
        if (symbol == null || symbol.isEmpty()) {
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

        // Redis 캐시 업데이트와 STOMP 브로드캐스트를 비동기로 병렬 처리
        CompletableFuture<Void> redisFuture = CompletableFuture.runAsync(
            () -> updateRedisCache(symbol, payload),
            executorService
        );
        
        CompletableFuture<Void> stompFuture = CompletableFuture.runAsync(
            () -> broadcastToStomp(symbol, payload),
            executorService
        );

        // 에러 처리 (비동기 실행이므로 예외가 발생해도 메인 스레드에 영향 없음)
        redisFuture.exceptionally(ex -> {
            log.error("Redis 캐시 업데이트 비동기 처리 실패: symbol={}", symbol, ex);
            return null;
        });
        
        stompFuture.exceptionally(ex -> {
            log.error("STOMP 브로드캐스트 비동기 처리 실패: symbol={}", symbol, ex);
            return null;
        });
    }

    /**
     * Redis 캐시 업데이트
     */
    private void updateRedisCache(String symbol, Map<String, Object> payload) {
        try {
            String key = REDIS_KEY_PREFIX + symbol;
            String value = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(key, value, REDIS_TTL);
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
        } catch (Exception e) {
            log.error("STOMP 브로드캐스트 실패: symbol={}", symbol, e);
        }
    }

    /**
     * Quote 데이터(OHLC 포함)를 정규화하여 브로드캐스트
     * 
     * Finnhub Quote API에서 받은 시가/고가/저가/종가/전일가 데이터를
     * 웹소켓으로 브로드캐스트하여 프론트엔드에서 실시간으로 업데이트할 수 있도록 합니다.
     * 
     * Redis 캐시 업데이트와 STOMP 브로드캐스트를 비동기로 병렬 처리합니다.
     *
     * @param symbol 종목 심볼
     * @param currentPrice 현재가 (종가)
     * @param open 시가
     * @param high 고가
     * @param low 저가
     * @param previousClose 전일 종가
     * @param change 변동액
     * @param changePercent 변동률 (%)
     */
    public void broadcastQuote(String symbol, Double currentPrice, Double open, Double high, 
                               Double low, Double previousClose, Double change, Double changePercent) {
        if (symbol == null || symbol.isEmpty()) {
            return;
        }

        // 정규화된 payload 생성 (OHLC 포함)
        Map<String, Object> payload = new HashMap<>();
        payload.put("ticker", symbol);
        payload.put("price", currentPrice != null ? currentPrice : 0.0); // 현재가 (종가)
        payload.put("ts", System.currentTimeMillis()); // 현재 시간
        payload.put("volume", 0.0); // Quote API에는 거래량이 없으므로 0
        payload.put("source", "FINNHUB");
        payload.put("rawType", "quote");
        
        // OHLC 데이터 추가
        if (open != null) {
            payload.put("open", open);
        }
        if (high != null) {
            payload.put("high", high);
        }
        if (low != null) {
            payload.put("low", low);
        }
        if (currentPrice != null) {
            payload.put("close", currentPrice); // 현재가가 종가
        }
        if (previousClose != null) {
            payload.put("previousClose", previousClose);
        }
        if (change != null) {
            payload.put("change", change);
        }
        if (changePercent != null) {
            payload.put("changePercent", changePercent);
        }

        // Redis 캐시 업데이트와 STOMP 브로드캐스트를 비동기로 병렬 처리
        CompletableFuture<Void> redisFuture = CompletableFuture.runAsync(
            () -> updateRedisCache(symbol, payload),
            executorService
        );
        
        CompletableFuture<Void> stompFuture = CompletableFuture.runAsync(
            () -> broadcastToStomp(symbol, payload),
            executorService
        );

        // 에러 처리 (비동기 실행이므로 예외가 발생해도 메인 스레드에 영향 없음)
        redisFuture.exceptionally(ex -> {
            log.error("Redis 캐시 업데이트 비동기 처리 실패: symbol={}", symbol, ex);
            return null;
        });
        
        stompFuture.exceptionally(ex -> {
            log.error("STOMP 브로드캐스트 비동기 처리 실패: symbol={}", symbol, ex);
            return null;
        });
    }

    /**
     * Redis에서 최신가 조회
     */
    public Map<String, Object> getLatestPrice(String symbol) {
        try {
            String key = REDIS_KEY_PREFIX + symbol;
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                return objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.error("Redis에서 최신가 조회 실패: symbol={}", symbol, e);
        }
        return null;
    }
}
