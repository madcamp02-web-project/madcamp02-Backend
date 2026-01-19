package com.madcamp02.domain.stock;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Market Movers를 위한 Top 20 Market Cap 종목 관리 Entity
 * 
 * Phase 3.5: 하드코딩된 종목 리스트를 DB로 관리하도록 확장
 */
@Entity
@Table(name = "market_cap_stocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MarketCapStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "market_cap_rank", nullable = false)
    private Integer marketCapRank;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onPersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
