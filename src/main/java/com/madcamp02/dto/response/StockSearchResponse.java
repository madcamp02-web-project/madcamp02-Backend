package com.madcamp02.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// GET /api/v1/stock/search 응답 DTO
@Getter
@Builder
public class StockSearchResponse {
    private List<Item> items;

    @Getter
    @Builder
    public static class Item {
        private String symbol;          // 심볼 (예: AAPL)
        private String description;     // 종목 설명 (예: Apple Inc)
        private String displaySymbol;   // 표시 심볼 (예: AAPL)
        private String type;            // 타입 (예: Common Stock, ETF)
    }
}
