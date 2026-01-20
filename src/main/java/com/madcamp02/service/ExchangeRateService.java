package com.madcamp02.service;

//======================================
// ExchangeRateService - 환율 수집/조회 서비스
//======================================
// - 한국수출입은행 Open API에서 환율을 가져와 DB에 저장
// - 프론트에서 사용할 환율 조회 API에 데이터 제공
//======================================

import com.fasterxml.jackson.databind.JsonNode;
import com.madcamp02.domain.fx.ExchangeRate;
import com.madcamp02.domain.fx.ExchangeRateRepository;
import com.madcamp02.external.ExchangeRateClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateClient exchangeRateClient;
    private final ExchangeRateRepository exchangeRateRepository;

    //------------------------------------------
    // 환율 수집 및 저장 (배치에서 호출)
    //------------------------------------------
    @Transactional
    public void collectAndSaveRates(LocalDate asOfDate) {
        LocalDate targetDate = asOfDate != null ? asOfDate : LocalDate.now();

        List<JsonNode> rawRates = exchangeRateClient.fetchRawRates(targetDate);
        if (rawRates.isEmpty()) {
            log.warn("수집된 환율 데이터가 없습니다. asOfDate={}", targetDate);
            return;
        }

        LocalDate resolvedDate = targetDate; // API 요청 시 주말 보정이 들어가므로, 여기서는 단순히 targetDate 사용

        int upsertCount = 0;
        for (JsonNode node : rawRates) {
            String curUnit = node.path("cur_unit").asText(null);
            if (curUnit == null || curUnit.isBlank()) {
                continue;
            }

            ExchangeRate newRate = ExchangeRate.builder()
                    .asOfDate(resolvedDate)
                    .curUnit(curUnit)
                    .curNm(node.path("cur_nm").asText(null))
                    .dealBasR(parseNumber(node.path("deal_bas_r").asText(null)))
                    .ttb(parseNumber(node.path("ttb").asText(null)))
                    .tts(parseNumber(node.path("tts").asText(null)))
                    .bkpr(parseNumber(node.path("bkpr").asText(null)))
                    .kftcDealBasR(parseNumber(node.path("kftc_deal_bas_r").asText(null)))
                    .kftcBkpr(parseNumber(node.path("kftc_bkpr").asText(null)))
                    .build();

            exchangeRateRepository.findByAsOfDateAndCurUnit(resolvedDate, curUnit)
                    .ifPresentOrElse(
                            existing -> existing.updateFrom(newRate),
                            () -> exchangeRateRepository.save(newRate)
                    );
            upsertCount++;
        }

        log.info("환율 데이터 수집/저장 완료 - asOfDate={}, count={}", resolvedDate, upsertCount);
    }

    private BigDecimal parseNumber(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.replaceAll(",", "").trim();
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            log.warn("숫자 파싱 실패 - raw={}", raw);
            return null;
        }
    }

    //------------------------------------------
    // 조회용 DTO
    //------------------------------------------
    @Transactional(readOnly = true)
    public ExchangeRateResponse getRatesByDate(LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        List<ExchangeRate> rates = exchangeRateRepository.findByAsOfDate(target);
        return toResponse(target, rates);
    }

    @Transactional(readOnly = true)
    public ExchangeRateResponse getLatestRates() {
        List<ExchangeRate> rates = exchangeRateRepository.findLatestRates();
        LocalDate asOf = rates.stream()
                .map(ExchangeRate::getAsOfDate)
                .findFirst()
                .orElse(null);
        return toResponse(asOf, rates);
    }

    private ExchangeRateResponse toResponse(LocalDate asOfDate, List<ExchangeRate> rates) {
        List<ExchangeRateItem> items = rates.stream()
                .map(rate -> new ExchangeRateItem(
                        rate.getCurUnit(),
                        rate.getCurNm(),
                        toDouble(rate.getDealBasR()),
                        toDouble(rate.getTtb()),
                        toDouble(rate.getTts())
                ))
                .collect(Collectors.toList());

        return new ExchangeRateResponse(
                asOfDate != null ? asOfDate.toString() : null,
                items
        );
    }

    private Double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

    // 응답용 내부 DTO들
    public record ExchangeRateResponse(
            String asOf,
            List<ExchangeRateItem> items
    ) {
    }

    public record ExchangeRateItem(
            String curUnit,
            String curNm,
            Double dealBasR,
            Double ttb,
            Double tts
    ) {
    }
}

