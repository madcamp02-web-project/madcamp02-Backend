package com.madcamp02.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// GET /api/v1/stock/candles/{ticker} 응답 DTO
@Getter
@Builder
public class StockCandlesResponse {
    private String ticker;              // 종목 심볼
    private String resolution;           // 시간 간격 (1, 5, 15, 30, 60, D, W, M)
    private List<Candle> items;          // 캔들 데이터 리스트

    @Getter
    @Builder
    public static class Candle {
        private Long timestamp;          // UNIX timestamp (초)
        private Double open;             // 시가
        private Double high;             // 고가
        private Double low;              // 저가
        private Double close;            // 종가
        private Long volume;              // 거래량
    }
}
