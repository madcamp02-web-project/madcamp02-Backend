package com.madcamp02.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 가챠 실행 응답 DTO
 * POST /api/v1/game/gacha
 */
@Getter
@Builder
public class GachaResponse {
    private Long itemId;
    private String name;
    private String category; // NAMEPLATE | AVATAR | THEME
    private String rarity;   // COMMON | RARE | EPIC | LEGENDARY
    private String imageUrl;
    private Integer remainingCoin; // 가챠 후 남은 게임 코인
}

