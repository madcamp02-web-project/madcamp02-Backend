package com.madcamp02.controller;

//======================================
// CalcController - 배당/세금 계산 API 컨트롤러
//======================================
// 프론트엔드 /calculator 페이지에서 사용하는 계산기용 API를 제공
// - GET /api/v1/calc/dividend
// - GET /api/v1/calc/tax
//======================================

import com.madcamp02.dto.response.CalcDividendResponse;
import com.madcamp02.dto.response.CalcTaxResponse;
import com.madcamp02.security.CustomUserDetails;
import com.madcamp02.service.CalcService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Calc", description = "배당/세금 계산 API")
@RestController
@RequestMapping("/api/v1/calc")
@RequiredArgsConstructor
public class CalcController {

    private final CalcService calcService;

    //------------------------------------------
    // 배당금 및 세금 계산
    //------------------------------------------
    // 요청: GET /api/v1/calc/dividend
    // 헤더: Authorization: Bearer {accessToken}
    //------------------------------------------
    @Operation(summary = "배당금/세금 계산", description = "보유 종목 기준 예상 배당금 및 세금 계산", security = @SecurityRequirement(name = "bearer-key"))
    @GetMapping("/dividend")
    public ResponseEntity<CalcDividendResponse> calculateDividend(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Double assumedDividendYield,
            @RequestParam(required = false) Double dividendPerShare,
            @RequestParam(required = false) Double taxRate
    ) {
        CalcDividendResponse response = calcService.calculateDividend(
                userDetails.getUserId(),
                assumedDividendYield,
                dividendPerShare,
                taxRate
        );
        return ResponseEntity.ok(response);
    }

    //------------------------------------------
    // 양도소득세 계산
    //------------------------------------------
    // 요청: GET /api/v1/calc/tax
    // 헤더: Authorization: Bearer {accessToken}
    //------------------------------------------
    @Operation(summary = "양도소득세 계산", description = "실현 수익 기준 예상 양도소득세 계산", security = @SecurityRequirement(name = "bearer-key"))
    @GetMapping("/tax")
    public ResponseEntity<CalcTaxResponse> calculateTax(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Double taxRate
    ) {
        CalcTaxResponse response = calcService.calculateTax(
                userDetails.getUserId(),
                taxRate
        );
        return ResponseEntity.ok(response);
    }
}

