package com.madcamp02.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 문서 기준을 GET /api/v1/market/movers으로 설정함

@Getter
@Builder
public class MarketMoversResponse {
    private String asOf; // ISO-8601 string
    private List<Item> items;

    @Getter
    @Builder
    public static class Item {
        private String ticker;
        private String name;
        private Double price;
        private Double changePercent; // percent (-100~100)
        private Long volume;
        private Direction direction;  // UP | DOWN
    }

    public enum Direction {
        UP,
        DOWN
    }
}

