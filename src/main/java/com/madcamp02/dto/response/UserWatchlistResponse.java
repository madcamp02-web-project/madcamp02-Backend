package com.madcamp02.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 관심종목 조회 응답 DTO
 * 
 * Phase 0 공통 규약 준수: { items: [...] } 패턴
 */
@Getter
@Builder
public class UserWatchlistResponse {
    private List<Item> items;

    @Getter
    @Builder
    public static class Item {
        private String ticker;
        private String addedAt; // ISO-8601 문자열
    }
}
