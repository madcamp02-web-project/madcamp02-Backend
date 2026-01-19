package com.madcamp02.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 거래 주문 요청 DTO
 * 매수/매도 주문 시 사용
 */
@Getter
@Setter
@NoArgsConstructor
public class TradeOrderRequest {

    @NotBlank(message = "종목 코드는 필수입니다.")
    private String ticker;        // 종목 코드 (예: "AAPL")

    @NotNull(message = "거래 타입은 필수입니다.")
    private com.madcamp02.domain.trade.TradeLog.TradeType type;      // BUY 또는 SELL

    @NotNull(message = "주문 수량은 필수입니다.")
    @Min(value = 1, message = "주문 수량은 1 이상이어야 합니다.")
    private Integer quantity;    // 주문 수량
}
