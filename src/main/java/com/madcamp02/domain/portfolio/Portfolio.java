package com.madcamp02.domain.portfolio;

import com.madcamp02.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "portfolio",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_portfolio_user_ticker",
                columnNames = {"user_id", "ticker"}
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pf_id")
    private Long pfId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "avg_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal avgPrice;

    // ========== 생성자 ==========

    @Builder
    public Portfolio(User user, String ticker, Integer quantity, BigDecimal avgPrice) {
        this.user = user;
        this.ticker = ticker;
        this.quantity = quantity;
        this.avgPrice = avgPrice;
    }

    // ========== 비즈니스 메서드 ==========

    /**
     * 매수 시 평단가 재계산
     * 새 평단가 = (기존수량 × 기존평단가 + 신규수량 × 체결가) / (기존수량 + 신규수량)
     */
    public void addQuantity(Integer newQuantity, BigDecimal newPrice) {
        BigDecimal currentValue = this.avgPrice.multiply(BigDecimal.valueOf(this.quantity));
        BigDecimal addedValue = newPrice.multiply(BigDecimal.valueOf(newQuantity));
        BigDecimal totalValue = currentValue.add(addedValue);

        this.quantity += newQuantity;
        this.avgPrice = totalValue.divide(
                BigDecimal.valueOf(this.quantity),
                4,
                RoundingMode.HALF_UP
        );
    }

    /**
     * 매도 시 수량 차감
     */
    public void subtractQuantity(Integer sellQuantity) {
        if (this.quantity < sellQuantity) {
            throw new IllegalArgumentException("보유 수량이 부족합니다.");
        }
        this.quantity -= sellQuantity;
    }

    /**
     * 수량이 0인지 확인
     */
    public boolean isEmpty() {
        return this.quantity == 0;
    }
}