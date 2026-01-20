package com.madcamp02.dto.response;

//======================================
// CalcTaxResponse - 양도소득세 계산 응답 DTO
//======================================
// /api/v1/calc/tax 응답용 DTO
//======================================

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CalcTaxResponse {

    // 실현 손익 (양도 차익)
    private final Double realizedProfit;

    // 과세 표준 (필요시 공제 등을 반영)
    private final Double taxBase;

    // 예상 양도소득세
    private final Double estimatedTax;

    // 통화 단위 (예: USD)
    private final String currency;
}

