package com.madcamp02.dto.response;

//======================================
// CalcDividendResponse - 배당금/세금 계산 응답 DTO
//======================================
// /api/v1/calc/dividend 응답용 DTO
//======================================

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CalcDividendResponse {

    // 총 예상 배당금
    private final Double totalDividend;

    // 배당소득세(원천징수) 예상 금액
    private final Double withholdingTax;

    // 세후 수령액
    private final Double netDividend;

    // 통화 단위 (예: USD)
    private final String currency;
}

