package com.madcamp02.domain.trade;

import com.madcamp02.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type", nullable = false, length = 4)
    private TradeType tradeType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(precision = 19, scale = 4)
    private BigDecimal fee;

    @Column(name = "realized_pnl", precision = 19, scale = 4)
    private BigDecimal realizedPnl;

    @Column(name = "trade_date", nullable = false, updatable = false)
    private LocalDateTime tradeDate;

    // ========== Enum ==========

    public enum TradeType {
        BUY, SELL
    }

    @Builder
    public TradeLog(User user, String ticker, TradeType tradeType,
                    BigDecimal price, Integer quantity, BigDecimal totalAmount,
                    BigDecimal fee, BigDecimal realizedPnl) {
        this.user = user;
        this.ticker = ticker;
        this.tradeType = tradeType;
        this.price = price;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.fee = fee != null ? fee : BigDecimal.ZERO;
        this.realizedPnl = realizedPnl;
        this.tradeDate = LocalDateTime.now();
    }
}