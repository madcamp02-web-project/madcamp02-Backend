package com.madcamp02.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 매수 가능 금액 조회 응답 DTO
 * GET /api/v1/trade/available-balance
 */
@Getter
@Builder
public class AvailableBalanceResponse {
    private Double availableBalance;  // 매수 가능 금액
    private Double cashBalance;      // 현재 예수금
    private String currency;         // "USD"
}
