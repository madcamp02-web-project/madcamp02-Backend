package com.madcamp02.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 문서기준으로 GET /api/v1/game/inventory으로 설정함

@Getter
@Builder
public class InventoryResponse {
    // 응답 기준 시점 (프론트에서 메타데이터로 사용)
    private String asOf;
    private List<Item> items;

    @Getter
    @Builder
    public static class Item {
        private Long itemId;
        private String name;
        private String category; // NAMEPLATE | AVATAR | THEME (Phase 1에서 Entity 정합화)
        private String rarity;   // COMMON | RARE | EPIC | LEGENDARY
        private String imageUrl;
        private Boolean equipped;
    }
}

