package com.madcamp02.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 문서기준으로 GET /api/v1/market/indices으로 설정함

@Getter
@Builder
public class MarketIndicesResponse {
    private String asOf; // ISO-8601 string
    private List<Item> items;

    @Getter
    @Builder
    public static class Item {
        private String code;         // e.g. KOSPI, NASDAQ, SP500
        private String name;         // display name
        private Double value;        // index value
        private Double change;       // absolute change
        private Double changePercent;// percent (-100~100)
        private String currency;     // ISO-4217 (KRW/USD)
    }
}

