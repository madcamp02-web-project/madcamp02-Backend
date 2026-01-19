package com.madcamp02.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 상점 아이템 목록 응답 DTO
 * GET /api/v1/game/items
 */
@Getter
@Builder
public class ItemsResponse {
    private String asOf; // ISO-8601 string
    private List<Item> items;

    @Getter
    @Builder
    public static class Item {
        private Long itemId;
        private String name;
        private String category;   // NAMEPLATE | AVATAR | THEME
        private String rarity;     // COMMON | RARE | EPIC | LEGENDARY
        private Float probability; // 뽑기 확률
        private String imageUrl;
        private String description;
    }
}

