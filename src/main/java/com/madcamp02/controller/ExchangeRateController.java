package com.madcamp02.controller;

//======================================
// ExchangeRateController - 환율 조회 API
//======================================
// - GET /api/v1/exchange-rates?date=yyyy-MM-dd
// - GET /api/v1/exchange-rates/latest
//======================================

import com.madcamp02.service.ExchangeRateService;
import com.madcamp02.service.ExchangeRateService.ExchangeRateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "ExchangeRate", description = "환율 조회 API")
@RestController
@RequestMapping("/api/v1/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @Operation(summary = "특정 일자의 환율 조회", description = "한국수출입은행 기준 환율을 조회합니다. date가 없으면 오늘 날짜 기준으로 조회합니다.")
    @GetMapping
    public ResponseEntity<ExchangeRateResponse> getRatesByDate(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        ExchangeRateResponse response = exchangeRateService.getRatesByDate(date);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "가장 최근 환율 조회", description = "DB에 저장된 가장 최근 기준일의 환율을 조회합니다.")
    @GetMapping("/latest")
    public ResponseEntity<ExchangeRateResponse> getLatestRates() {
        ExchangeRateResponse response = exchangeRateService.getLatestRates();
        return ResponseEntity.ok(response);
    }
}

