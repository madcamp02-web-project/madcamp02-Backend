package com.madcamp02.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 거래 체결 알림 DTO
 * 
 * `/user/queue/trade` STOMP 토픽으로 전송되는 체결 알림 메시지
 */
@Getter
@Builder
public class TradeNotificationDto {
    private Long orderId;
    private String ticker;
    private String type; // "BUY" | "SELL"
    private Integer quantity;
    private Double executedPrice;
    private Double totalAmount;
    private Double realizedPnl; // 매도 시만 (매수 시는 null)
    private String executedAt; // ISO-8601 문자열
    private String status; // "FILLED" (향후 확장: PARTIALLY_FILLED 등)
}
