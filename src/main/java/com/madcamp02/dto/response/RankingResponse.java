package com.madcamp02.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// GET /api/v1/game/ranking으로 함

@Getter
@Builder
public class RankingResponse {
    private String asOf; // ISO-8601 string
    private List<Item> items;
    private My my;       // nullable

    @Getter
    @Builder
    public static class Item {
        private Integer rank;
        private Long userId;
        private String nickname;
        private String avatarUrl;
        private Double totalEquity;
        private Double returnPercent; // percent (-100~100)
    }

    @Getter
    @Builder
    public static class My {
        private Integer rank;
        private Double totalEquity;
        private Double returnPercent; // percent (-100~100)
    }
}

