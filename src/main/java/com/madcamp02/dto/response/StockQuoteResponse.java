package com.madcamp02.dto.response;

import lombok.Builder;
import lombok.Getter;

// GET /api/v1/stock/quote/{ticker} 응답 DTO
@Getter
@Builder
public class StockQuoteResponse {
    private String ticker;              // 종목 심볼
    private Double currentPrice;        // 현재가 (c)
    private Double open;                 // 당일 시가 (o)
    private Double high;                 // 당일 최고가 (h)
    private Double low;                  // 당일 최저가 (l)
    private Double previousClose;        // 전일 종가 (pc)
    private Double change;               // 변동액 (d) - API에서 제공
    private Double changePercent;        // 변동률 (dp) - API에서 제공, %
}
