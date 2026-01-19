package com.madcamp02.dto.response;

import com.madcamp02.domain.trade.TradeLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 거래 내역 조회 응답 DTO
 * GET /api/v1/trade/history
 */
@Getter
@Builder
public class TradeHistoryResponse {
    private String asOf;            // ISO-8601 문자열
    private List<Item> items;        // 거래 내역 리스트

    @Getter
    @Builder
    public static class Item {
        private Long logId;
        private String ticker;
        private TradeLog.TradeType type;
        private Integer quantity;
        private Double price;
        private Double totalAmount;
        private Double realizedPnl;  // 매도 시 실현 손익
        private LocalDateTime tradeDate;
    }
}
