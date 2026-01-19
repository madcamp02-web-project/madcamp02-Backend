package com.madcamp02.dto.response;

import com.madcamp02.domain.trade.TradeLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 거래 주문 실행 응답 DTO
 * POST /api/v1/trade/order
 */
@Getter
@Builder
public class TradeResponse {
    private Long orderId;           // 거래 ID (TradeLog.logId)
    private String ticker;          // 종목 코드
    private TradeLog.TradeType type;         // BUY 또는 SELL
    private Integer quantity;       // 체결 수량
    private Double executedPrice;   // 체결 가격
    private Double totalAmount;     // 총 거래 금액
    private LocalDateTime executedAt; // 체결 시간
}
