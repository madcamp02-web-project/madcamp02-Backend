package com.madcamp02.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// GET /api/v1/market/news으로 함
@Getter
@Builder
public class MarketNewsResponse {
    private String asOf; // ISO-8601 string
    private List<Item> items;

    @Getter
    @Builder
    public static class Item {
        private String id;          // stable id (e.g. provider:id)
        private String headline;
        private String summary;
        private String source;
        private String url;
        private String imageUrl;
        private String publishedAt; // ISO-8601 string
    }
}

