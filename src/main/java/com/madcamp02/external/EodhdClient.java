package com.madcamp02.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
public class EodhdClient {

    private static final String BASE_URL = "https://eodhd.com/api";

    private final RestTemplate restTemplate;
    private final String apiKey;

    public EodhdClient(
            RestTemplate restTemplate,
            @Value("${eodhd.api-key:}") String apiKey // Default to empty if not set
    ) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("EODHD API 키가 설정되지 않았습니다. Historical Data 조회가 제한됩니다.");
        }
    }

    // ------------------------------------------
    // Historical Data Response DTO
    // ------------------------------------------
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EodhdCandle {
        @JsonProperty("date")
        private LocalDate date;

        @JsonProperty("open")
        private Double open;

        @JsonProperty("high")
        private Double high;

        @JsonProperty("low")
        private Double low;

        @JsonProperty("close")
        private Double close;

        @JsonProperty("adjusted_close")
        private Double adjustedClose;

        @JsonProperty("volume")
        private Long volume;

        // 경고 메시지 필드 (무료 구독 제한 등)
        @JsonProperty("warning")
        private String warning;
    }

    // ------------------------------------------
    // EODHD Exception
    // ------------------------------------------
    public static class EodhdException extends RuntimeException {
        public EodhdException(String message) {
            super(message);
        }

        public EodhdException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // ------------------------------------------
    // Historical Data 조회
    // ------------------------------------------
    // GET /eod/{ticker}?api_token={key}&fmt=json&from={date}&to={date}
    // ------------------------------------------
    public List<EodhdCandle> getHistoricalData(String ticker, LocalDate from, LocalDate to) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new EodhdException("EODHD API Key is missing");
        }

        try {
            // 티커 형식 변환: US 종목은 .US 추가 (EODHD 공식 문서 권장)
            // EODHD API는 {SYMBOL}.{EXCHANGE_ID} 형식을 권장 (예: AAPL.US)
            String formattedTicker = ticker;
            if (!ticker.contains(".")) {
                // 티커에 거래소 코드가 없으면 .US 추가 (미국 주식 가정)
                formattedTicker = ticker + ".US";
                log.debug("EODHD 티커 형식 변환: {} -> {}", ticker, formattedTicker);
            }

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(BASE_URL + "/eod/" + formattedTicker)
                    .queryParam("api_token", apiKey)
                    .queryParam("fmt", "json");

            if (from != null) {
                builder.queryParam("from", from.toString());
            }
            if (to != null) {
                builder.queryParam("to", to.toString());
            }

            String url = builder.toUriString();
            log.debug("EODHD API 호출: ticker={} (formatted={}), from={}, to={}, url={}", ticker, formattedTicker, from, to, url);

            ResponseEntity<String> rawResponse = restTemplate.getForEntity(url, String.class);
            
            if (rawResponse.getStatusCode() == HttpStatus.OK && rawResponse.getBody() != null) {
                String responseBody = rawResponse.getBody();
                log.debug("EODHD API 원시 응답 (처음 500자): {}", responseBody.length() > 500 ? responseBody.substring(0, 500) : responseBody);
                
                // JSON 파싱
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                EodhdCandle[] body;
                try {
                    body = objectMapper.readValue(responseBody, EodhdCandle[].class);
                } catch (JsonProcessingException e) {
                    log.error("EODHD API JSON 파싱 실패: responseBody={}", responseBody.length() > 1000 ? responseBody.substring(0, 1000) : responseBody, e);
                    throw new EodhdException("EODHD API JSON 파싱 오류: " + e.getMessage(), e);
                }
                
                // 경고 메시지 체크 및 유효한 캔들 데이터 필터링
                List<EodhdCandle> validCandles = new java.util.ArrayList<>();
                for (EodhdCandle candle : body) {
                    if (candle.getWarning() != null && !candle.getWarning().isEmpty()) {
                        log.warn("EODHD API 경고: {} (ticker={}, from={}, to={})", candle.getWarning(), formattedTicker, from, to);
                        // 경고 메시지만 있는 경우는 건너뜀
                        continue;
                    }
                    // 유효한 캔들 데이터만 추가 (date가 null이 아닌 경우)
                    if (candle.getDate() != null) {
                        validCandles.add(candle);
                    }
                }
                
                log.debug("EODHD API 응답 본문 길이: {} (유효한 캔들: {})", body.length, validCandles.size());
                if (validCandles.size() > 0) {
                    log.debug("EODHD API 첫 번째 캔들 샘플: date={}, open={}, close={}", validCandles.get(0).getDate(), validCandles.get(0).getOpen(), validCandles.get(0).getClose());
                }
                if (validCandles.size() > 1) {
                    log.debug("EODHD API 마지막 캔들 샘플: date={}, open={}, close={}", validCandles.get(validCandles.size() - 1).getDate(), validCandles.get(validCandles.size() - 1).getOpen(), validCandles.get(validCandles.size() - 1).getClose());
                }
                
                if (validCandles.isEmpty() && body.length > 0) {
                    // 경고 메시지만 있고 실제 데이터가 없는 경우
                    log.warn("EODHD API 응답에 유효한 캔들 데이터가 없습니다. 경고 메시지만 반환됨. (ticker={}, from={}, to={})", formattedTicker, from, to);
                }
                
                log.info("EODHD API 응답 성공: {} candles (ticker={}, from={}, to={})", validCandles.size(), formattedTicker, from, to);
                return validCandles;
            } else {
                log.warn("EODHD API 응답 실패: status={}, body={}", rawResponse.getStatusCode(), rawResponse.getBody());
                throw new EodhdException("EODHD API 호출 실패: " + rawResponse.getStatusCode());
            }

        } catch (Exception e) {
            if (e instanceof EodhdException) {
                throw e;
            }
            log.error("EODHD API 호출 중 오류: ticker={}", ticker, e);
            throw new EodhdException("EODHD API Error: " + e.getMessage(), e);
        }
    }
}
