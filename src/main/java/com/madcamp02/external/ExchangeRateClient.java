package com.madcamp02.external;

//======================================
// ExchangeRateClient - 한국수출입은행 환율 Open API 클라이언트
//======================================
// - AP01 (현재 환율) 엔드포인트를 호출하여 환율 리스트를 가져온다.
//======================================

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${exchange.api.auth-key:}")
    private String authKey;

    @Value("${exchange.api.data-code:AP01}")
    private String dataCode;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public List<JsonNode> fetchRawRates(LocalDate date) {
        String searchDate = resolveSearchDate(date);

        URI uri = UriComponentsBuilder.fromUriString("https://www.koreaexim.go.kr")
                .path("/site/program/financial/exchangeJSON")
                .queryParam("authkey", authKey)
                .queryParam("searchdate", searchDate)
                .queryParam("data", dataCode)
                .build(true) // 인코딩 유지
                .toUri();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("환율 API 호출 실패 - status: {}, body: {}", response.getStatusCode(), response.getBody());
                return Collections.emptyList();
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            if (!root.isArray()) {
                log.warn("환율 API 응답이 배열이 아님: {}", root);
                return Collections.emptyList();
            }

            List<JsonNode> result = new ArrayList<>();
            root.forEach(result::add);
            return result;
        } catch (IOException e) {
            log.error("환율 API 응답 파싱 실패", e);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("환율 API 호출 중 예외 발생", e);
            return Collections.emptyList();
        }
    }

    /**
     * 토요일/일요일에는 금요일 날짜를 사용하고, 그 외에는 해당 날짜를 그대로 사용한다.
     */
    private String resolveSearchDate(LocalDate date) {
        LocalDate currentDate = date != null ? date : LocalDate.now();
        DayOfWeek dayOfWeek = currentDate.getDayOfWeek();

        if (dayOfWeek == DayOfWeek.SATURDAY) {
            return currentDate.minusDays(1).format(DATE_FORMAT);
        }
        if (dayOfWeek == DayOfWeek.SUNDAY) {
            return currentDate.minusDays(2).format(DATE_FORMAT);
        }
        return currentDate.format(DATE_FORMAT);
    }
}

