package com.madcamp02.controller;

//======================================
// StockController - 주식 데이터 API 컨트롤러
//======================================
// Phase 3에서 구현하는 /api/v1/stock/* 요청을 처리하는 REST 컨트롤러
//
// 역할:
// - 클라이언트 요청을 받고
// - Service에 일을 시킨 뒤
// - 결과 DTO를 JSON으로 반환합니다.
//
// 주의:
// - SecurityConfig에서 PUBLIC_ENDPOINTS에 포함되므로
//   인증 없이 접근 가능합니다.
//======================================

import com.madcamp02.dto.response.StockCandlesResponse;
import com.madcamp02.dto.response.StockQuoteResponse;
import com.madcamp02.dto.response.StockSearchResponse;
import com.madcamp02.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "Stock", description = "주식 데이터 API")
@RestController
@RequestMapping("/api/v1/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    //------------------------------------------
    // 종목 검색
    //------------------------------------------
    // 요청: GET /api/v1/stock/search?keyword={keyword}
    // 인증: 불필요 (Public API)
    //------------------------------------------
    @Operation(summary = "종목 검색", description = "종목명, 심볼, ISIN, CUSIP으로 종목 검색")
    @GetMapping("/search")
    public ResponseEntity<StockSearchResponse> searchStock(
            @Parameter(description = "검색어", required = true)
            @RequestParam String keyword
    ) {
        StockSearchResponse response = stockService.searchStock(keyword);
        return ResponseEntity.ok(response);
    }

    //------------------------------------------
    // 현재가 조회
    //------------------------------------------
    // 요청: GET /api/v1/stock/quote/{ticker}
    // 인증: 불필요 (Public API)
    //------------------------------------------
    @Operation(summary = "현재가 조회", description = "특정 종목의 현재가 및 호가 정보 조회")
    @GetMapping("/quote/{ticker}")
    public ResponseEntity<StockQuoteResponse> getQuote(
            @Parameter(description = "종목 심볼", required = true)
            @PathVariable String ticker
    ) {
        StockQuoteResponse response = stockService.getQuote(ticker);
        return ResponseEntity.ok(response);
    }

    //------------------------------------------
    // 캔들 차트 데이터 조회
    //------------------------------------------
    // 요청: GET /api/v1/stock/candles/{ticker}?resolution={d|w|m}&from={ISO-8601}&to={ISO-8601}
    // 인증: 불필요 (Public API)
    // 파라미터:
    //   - ticker (path): 종목 심볼
    //   - resolution (query): period (d=daily, w=weekly, m=monthly)
    //   - from (query): 시작 시간 (ISO-8601 형식)
    //   - to (query): 종료 시간 (ISO-8601 형식)
    //------------------------------------------
    @Operation(summary = "캔들 차트 데이터 조회", description = "특정 종목의 캔들 차트 데이터 조회 (EODHD API 사용)")
    @GetMapping("/candles/{ticker}")
    public ResponseEntity<StockCandlesResponse> getCandles(
            @Parameter(description = "종목 심볼", required = true)
            @PathVariable String ticker,
            @Parameter(description = "시간 간격: d (daily), w (weekly), m (monthly)", required = true)
            @RequestParam String resolution,
            @Parameter(description = "시작 시간 (ISO-8601)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "종료 시간 (ISO-8601)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        StockCandlesResponse response = stockService.getCandles(ticker, resolution, from, to);
        return ResponseEntity.ok(response);
    }
}
