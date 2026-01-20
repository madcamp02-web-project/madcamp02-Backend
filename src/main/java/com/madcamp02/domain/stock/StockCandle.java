package com.madcamp02.domain.stock;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_candles")
@IdClass(StockCandleId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class StockCandle {

    @Id
    @Column(nullable = false, length = 20)
    private String symbol;

    @Id
    @Column(nullable = false)
    private LocalDate date;

    @Id
    @Column(nullable = false, length = 1)
    private String period; // d (daily), w (weekly), m (monthly)

    @Column(precision = 19, scale = 4)
    private BigDecimal open;

    @Column(precision = 19, scale = 4)
    private BigDecimal high;

    @Column(precision = 19, scale = 4)
    private BigDecimal low;

    @Column(name = "close", precision = 19, scale = 4) // close is reserved keyword in some DBs, safe to quote or use
                                                       // name
    private BigDecimal close;

    private Long volume;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    public void onPersist() {
        this.lastUpdated = LocalDateTime.now();
    }
}
