package com.madcamp02.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// GET /api/v1/trade/portfolio으로 함

@Getter
@Builder
public class PortfolioResponse {
    private String asOf; // ISO-8601 string
    private Summary summary;
    private List<Position> positions;

    @Getter
    @Builder
    public static class Summary {
        private Double totalEquity;
        private Double cashBalance;
        private Double totalPnl;
        private Double totalPnlPercent; // percent (-100~100)
        private String currency;        // ISO-4217
    }

    @Getter
    @Builder
    public static class Position {
        private String ticker;
        private Long quantity;
        private Double avgPrice;
        private Double currentPrice;
        private Double marketValue;
        private Double pnl;
        private Double pnlPercent; // percent (-100~100)
    }
}

