package com.madcamp02.controller;

//======================================
// MarketController - 시장 데이터 API 컨트롤러
//======================================
// Phase 3에서 구현하는 /api/v1/market/* 요청을 처리하는 REST 컨트롤러
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

import com.madcamp02.dto.response.MarketIndicesResponse;
import com.madcamp02.dto.response.MarketMoversResponse;
import com.madcamp02.dto.response.MarketNewsResponse;
import com.madcamp02.service.MarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Market", description = "시장 데이터 API")
@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;

    //------------------------------------------
    // 주요 지수 조회
    //------------------------------------------
    // 요청: GET /api/v1/market/indices
    // 인증: 불필요 (Public API)
    //------------------------------------------
    @Operation(summary = "주요 지수 조회", description = "NASDAQ, S&P500, Dow Jones 등 주요 미국 시장 지수 조회")
    @GetMapping("/indices")
    public ResponseEntity<MarketIndicesResponse> getIndices() {
        MarketIndicesResponse response = marketService.getIndices();
        return ResponseEntity.ok(response);
    }

    //------------------------------------------
    // 시장 뉴스 조회
    //------------------------------------------
    // 요청: GET /api/v1/market/news
    // 인증: 불필요 (Public API)
    //------------------------------------------
    @Operation(summary = "시장 뉴스 조회", description = "최신 시장 뉴스 조회")
    @GetMapping("/news")
    public ResponseEntity<MarketNewsResponse> getNews() {
        MarketNewsResponse response = marketService.getNews();
        return ResponseEntity.ok(response);
    }

    //------------------------------------------
    // 급등/급락 종목 조회
    //------------------------------------------
    // 요청: GET /api/v1/market/movers
    // 인증: 불필요 (Public API)
    //------------------------------------------
    @Operation(summary = "급등/급락 종목 조회", description = "급등/급락/거래량 상위 종목 조회")
    @GetMapping("/movers")
    public ResponseEntity<MarketMoversResponse> getMovers() {
        MarketMoversResponse response = marketService.getMovers();
        return ResponseEntity.ok(response);
    }
}
