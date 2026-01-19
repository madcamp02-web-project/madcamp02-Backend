package com.madcamp02.controller;

import com.madcamp02.dto.request.TradeOrderRequest;
import com.madcamp02.dto.response.AvailableBalanceResponse;
import com.madcamp02.dto.response.PortfolioResponse;
import com.madcamp02.dto.response.TradeHistoryResponse;
import com.madcamp02.dto.response.TradeResponse;
import com.madcamp02.security.CustomUserDetails;
import com.madcamp02.service.PortfolioService;
import com.madcamp02.service.TradeService;
import com.madcamp02.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 거래 관련 API 컨트롤러.
 * Phase 4: Trade/Portfolio Engine.
 *
 * 엔드포인트:
 * - GET  /api/v1/trade/available-balance - 매수 가능 금액 조회
 * - POST /api/v1/trade/order             - 매수/매도 주문 실행
 * - GET  /api/v1/trade/portfolio         - 보유 종목 및 수익률 조회
 * - GET  /api/v1/trade/history           - 거래 내역 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/trade")
@RequiredArgsConstructor
@Tag(name = "Trade API", description = "거래/포트폴리오 관련 API")
@SecurityRequirement(name = "bearer-key")
public class TradeController {

    private final TradeService tradeService;
    private final PortfolioService portfolioService;
    private final WalletService walletService;

    /**
     * 매수 가능 금액 조회.
     * GET /api/v1/trade/available-balance
     */
    @Operation(summary = "매수 가능 금액 조회", description = "현재 지갑 기준으로 매수 가능한 최대 금액을 조회합니다.")
    @GetMapping("/available-balance")
    public ResponseEntity<AvailableBalanceResponse> getAvailableBalance(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("매수 가능 금액 조회 요청: userId={}", userDetails.getUserId());
        AvailableBalanceResponse response = walletService
                .getAvailableBalance(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * 거래 주문 실행.
     * POST /api/v1/trade/order
     */
    @Operation(summary = "거래 주문 실행", description = "지정한 종목에 대해 매수/매도 주문을 실행합니다.")
    @PostMapping("/order")
    public ResponseEntity<TradeResponse> submitOrder(
            @Valid @RequestBody TradeOrderRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("거래 주문 요청: userId={}, ticker={}, type={}, quantity={}",
                userDetails.getUserId(), request.getTicker(), request.getType(), request.getQuantity());
        TradeResponse response = tradeService.executeOrder(
                userDetails.getUserId(),
                request
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 포트폴리오 조회.
     * GET /api/v1/trade/portfolio
     */
    @Operation(summary = "포트폴리오 조회", description = "보유 종목, 평가 금액, 손익률을 포함한 포트폴리오 정보를 조회합니다.")
    @GetMapping("/portfolio")
    public ResponseEntity<PortfolioResponse> getPortfolio(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("포트폴리오 조회 요청: userId={}", userDetails.getUserId());
        PortfolioResponse response = portfolioService
                .getPortfolio(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * 거래 내역 조회.
     * GET /api/v1/trade/history
     */
    @Operation(summary = "거래 내역 조회", description = "기간과 페이지 정보를 기준으로 거래 내역을 조회합니다.")
    @GetMapping("/history")
    public ResponseEntity<TradeHistoryResponse> getTradeHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 20, sort = "tradeDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.debug("거래 내역 조회 요청: userId={}, startDate={}, endDate={}",
                userDetails.getUserId(), startDate, endDate);
        TradeHistoryResponse response = tradeService.getTradeHistory(
                userDetails.getUserId(),
                startDate,
                endDate,
                pageable
        );
        return ResponseEntity.ok(response);
    }
}
