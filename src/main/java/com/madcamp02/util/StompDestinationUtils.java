package com.madcamp02.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * StompDestinationUtils
 *
 * STOMP destination 생성 및 안전성 처리를 위한 유틸리티 클래스
 * 
 * 참고: 현재 구현은 ticker를 그대로 사용하지만,
 * 향후 URL 인코딩이 필요한 경우를 대비하여 유틸리티 메서드 제공
 */
public class StompDestinationUtils {

    private static final String TOPIC_PREFIX = "/topic/stock.ticker.";

    /**
     * STOMP destination 생성
     * 
     * @param ticker 종목 심볼 (예: "AAPL", "BINANCE:BTCUSDT", "IC MARKETS:1")
     * @return STOMP destination (예: "/topic/stock.ticker.AAPL")
     */
    public static String createDestination(String ticker) {
        if (ticker == null || ticker.isEmpty()) {
            throw new IllegalArgumentException("Ticker cannot be null or empty");
        }
        return TOPIC_PREFIX + ticker;
    }

    /**
     * STOMP destination에서 ticker 추출
     * 
     * @param destination STOMP destination (예: "/topic/stock.ticker.AAPL")
     * @return ticker (예: "AAPL")
     */
    public static String extractTicker(String destination) {
        if (destination == null || !destination.startsWith(TOPIC_PREFIX)) {
            throw new IllegalArgumentException("Invalid destination format: " + destination);
        }
        return destination.substring(TOPIC_PREFIX.length());
    }

    /**
     * URL 인코딩된 destination 생성 (향후 필요 시 사용)
     * 
     * 공백/특수문자가 포함된 ticker의 경우 URL 인코딩을 적용
     * 예: "IC MARKETS:1" -> "/topic/stock.ticker.IC%20MARKETS%3A1"
     * 
     * @param ticker 종목 심볼
     * @return URL 인코딩된 STOMP destination
     */
    public static String createEncodedDestination(String ticker) {
        if (ticker == null || ticker.isEmpty()) {
            throw new IllegalArgumentException("Ticker cannot be null or empty");
        }
        String encodedTicker = URLEncoder.encode(ticker, StandardCharsets.UTF_8);
        return TOPIC_PREFIX + encodedTicker;
    }

    /**
     * URL 인코딩된 destination에서 ticker 디코딩 (향후 필요 시 사용)
     * 
     * @param destination URL 인코딩된 STOMP destination
     * @return 디코딩된 ticker
     */
    public static String extractTickerFromEncoded(String destination) {
        if (destination == null || !destination.startsWith(TOPIC_PREFIX)) {
            throw new IllegalArgumentException("Invalid destination format: " + destination);
        }
        String encodedTicker = destination.substring(TOPIC_PREFIX.length());
        return java.net.URLDecoder.decode(encodedTicker, StandardCharsets.UTF_8);
    }

    /**
     * Ticker가 안전한 형식인지 확인
     * 
     * 현재 정책: 공백/특수문자 허용 (그대로 사용)
     * 향후 제한이 필요한 경우 이 메서드를 사용하여 검증
     * 
     * @param ticker 종목 심볼
     * @return 안전한 형식이면 true
     */
    public static boolean isSafeTicker(String ticker) {
        if (ticker == null || ticker.isEmpty()) {
            return false;
        }
        // 현재는 모든 ticker를 허용 (공백/특수문자 포함)
        // 향후 제한이 필요한 경우 여기에 검증 로직 추가
        return true;
    }
}
