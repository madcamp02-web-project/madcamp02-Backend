package com.madcamp02.domain.fx;

//======================================
// ExchangeRate - 한국수출입은행 환율 엔티티
//======================================
// - asOfDate: 환율 기준일 (KRX 기준, yyyy-MM-dd)
// - curUnit : 통화 코드 (예: USD, JPY(100))
// - dealBasR: 매매 기준율
//======================================

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_rates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "as_of_date", nullable = false)
    private LocalDate asOfDate;

    @Column(name = "cur_unit", nullable = false, length = 20)
    private String curUnit;

    @Column(name = "cur_nm", length = 100)
    private String curNm;

    @Column(name = "deal_bas_r", precision = 18, scale = 6)
    private BigDecimal dealBasR;

    @Column(name = "ttb", precision = 18, scale = 6)
    private BigDecimal ttb;

    @Column(name = "tts", precision = 18, scale = 6)
    private BigDecimal tts;

    @Column(name = "bkpr", precision = 18, scale = 6)
    private BigDecimal bkpr;

    @Column(name = "kftc_deal_bas_r", precision = 18, scale = 6)
    private BigDecimal kftcDealBasR;

    @Column(name = "kftc_bkpr", precision = 18, scale = 6)
    private BigDecimal kftcBkpr;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public ExchangeRate(
            LocalDate asOfDate,
            String curUnit,
            String curNm,
            BigDecimal dealBasR,
            BigDecimal ttb,
            BigDecimal tts,
            BigDecimal bkpr,
            BigDecimal kftcDealBasR,
            BigDecimal kftcBkpr
    ) {
        this.asOfDate = asOfDate;
        this.curUnit = curUnit;
        this.curNm = curNm;
        this.dealBasR = dealBasR;
        this.ttb = ttb;
        this.tts = tts;
        this.bkpr = bkpr;
        this.kftcDealBasR = kftcDealBasR;
        this.kftcBkpr = kftcBkpr;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateFrom(ExchangeRate other) {
        this.dealBasR = other.dealBasR;
        this.ttb = other.ttb;
        this.tts = other.tts;
        this.bkpr = other.bkpr;
        this.kftcDealBasR = other.kftcDealBasR;
        this.kftcBkpr = other.kftcBkpr;
        this.curNm = other.curNm;
        this.updatedAt = LocalDateTime.now();
    }
}

