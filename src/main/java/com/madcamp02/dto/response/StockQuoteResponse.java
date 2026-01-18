package com.madcamp02.dto.response;

import lombok.Builder;
import lombok.Getter;

// GET /api/v1/stock/quote/{ticker} 응답 DTO
@Getter
@Builder
public class StockQuoteResponse {
    private String ticker;              // 종목 심볼
    private Double currentPrice;        // 현재가
    private Double open;                 // 당일 시가
    private Double high;                 // 당일 최고가
    private Double low;                  // 당일 최저가
    private Double previousClose;        // 전일 종가
    private Double change;               // 변동액 (currentPrice - previousClose)
    private Double changePercent;        // 변동률 (%)
    private Long timestamp;              // UNIX timestamp (초)
}
