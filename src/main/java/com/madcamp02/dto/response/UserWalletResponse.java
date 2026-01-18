package com.madcamp02.dto.response;

//======================================
// UserWalletResponse - 지갑 정보 응답 DTO
//======================================
// Phase 2에서 구현하는 GET /api/v1/user/wallet 응답 데이터 구조
//
// 문서 기준:
// - "지갑 정보 (예수금, 코인 등)"
//
// 포함 필드:
// - cashBalance: 예수금(현금 잔고)
// - realizedProfit: 실현 수익
// - totalAssets: 총 자산
// - gameCoin: 게임 코인
//======================================

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class UserWalletResponse {
    private BigDecimal cashBalance;
    private BigDecimal realizedProfit;
    private BigDecimal totalAssets;
    private Integer gameCoin;
}

