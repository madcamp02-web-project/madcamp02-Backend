package com.madcamp02.domain.wallet;

//여기 코드 관련된 궁금한 점 있을 경우 User.java로 가서 하나하나 배우면 된다. JPA Entity 설정하는건 user.java에 짜는 방법 기록해둠

import com.madcamp02.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long walletId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "cash_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal cashBalance;

    @Column(name = "realized_profit", nullable = false, precision = 19, scale = 4)
    private BigDecimal realizedProfit;

    @Column(name = "total_assets", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAssets;

    @Column(name = "game_coin", nullable = false)
    private Integer gameCoin;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ========== 생성자 ==========

    @Builder
    public Wallet(User user) {
        this.user = user;
        this.cashBalance = new BigDecimal("10000.0000");
        this.realizedProfit = BigDecimal.ZERO;
        this.totalAssets = new BigDecimal("10000.0000");
        this.gameCoin = 0;
        this.updatedAt = LocalDateTime.now();
    }

    // ========== 비즈니스 메서드 ==========

    public void deductCash(BigDecimal amount) {
        if (this.cashBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("잔고가 부족합니다.");
        }
        this.cashBalance = this.cashBalance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }

    public void addCash(BigDecimal amount) {
        this.cashBalance = this.cashBalance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    public void addRealizedProfit(BigDecimal profit) {
        this.realizedProfit = this.realizedProfit.add(profit);
        this.updatedAt = LocalDateTime.now();
    }

    public void updateTotalAssets(BigDecimal totalAssets) {
        this.totalAssets = totalAssets;
        this.updatedAt = LocalDateTime.now();
    }

    public void deductGameCoin(int amount) {
        if (this.gameCoin < amount) {
            throw new IllegalArgumentException("게임 코인이 부족합니다.");
        }
        this.gameCoin -= amount;
        this.updatedAt = LocalDateTime.now();
    }

    public void addGameCoin(int amount) {
        this.gameCoin += amount;
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}