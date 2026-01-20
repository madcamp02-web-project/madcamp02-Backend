package com.madcamp02.service;

//======================================
// CalcService - 배당/세금 계산 서비스
//======================================
// Trade/Portfolio 엔진에서 계산된 보유 포지션/실현 손익을 이용해
// 프론트 계산기에서 쓸 요약 값을 계산해 주는 서비스
//======================================

import com.madcamp02.domain.wallet.Wallet;
import com.madcamp02.domain.wallet.WalletRepository;
import com.madcamp02.dto.response.CalcDividendResponse;
import com.madcamp02.dto.response.CalcTaxResponse;
import com.madcamp02.exception.BusinessException;
import com.madcamp02.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CalcService {

    private final WalletRepository walletRepository;

    // 향후 필요 시 TradeService/PortfolioService 등을 추가로 주입해
    // 실제 포지션/거래 내역 기반으로 보다 정교한 계산을 수행할 예정

    //------------------------------------------
    // 배당금 및 세금 계산
    //------------------------------------------
    @Transactional(readOnly = true)
    public CalcDividendResponse calculateDividend(
            Long userId,
            Double assumedDividendYield,
            Double dividendPerShare,
            Double taxRate
    ) {
        // 1. 지갑 조회 (총 자산을 배당 계산의 기준값으로 사용)
        Wallet wallet = walletRepository.findByUserUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 총 자산(totalAssets)을 기준으로 간단한 1차 배당 계산을 수행
        // - assumedDividendYield: 연간 배당 수익률 (예: 0.03 = 3%)
        // - dividendPerShare: 주당 배당액 (현재 버전에서는 사용하지 않고 향후 확장 시 사용)
        //   → 1차 버전에서는 수익률 기반 계산만 지원하고, 주당 배당액 기반 계산은 TODO로 남겨둔다.

        double baseAssets = wallet.getTotalAssets() != null
                ? wallet.getTotalAssets().doubleValue()
                : 0.0;

        double totalDividend = 0.0;

        // TODO: dividendPerShare를 활용한 \"종목별 보유 수량 × 주당 배당액\" 계산은
        //       Portfolio/Trade 엔진에서 종목별 포지션 정보를 직접 받아오는 2차 버전에서 구현

        if (assumedDividendYield != null && baseAssets > 0.0) {
            totalDividend = baseAssets * assumedDividendYield;
        }

        double effectiveTaxRate = (taxRate != null && taxRate > 0.0) ? taxRate : 0.0;
        double withholdingTax = totalDividend * effectiveTaxRate;
        double netDividend = totalDividend - withholdingTax;

        return CalcDividendResponse.builder()
                .totalDividend(totalDividend)
                .withholdingTax(withholdingTax)
                .netDividend(netDividend)
                // currency는 아직 통화/환율 전략을 도입하지 않았으므로 null로 유지
                .currency(null)
                .build();
    }

    //------------------------------------------
    // 양도소득세 계산
    //------------------------------------------
    @Transactional(readOnly = true)
    public CalcTaxResponse calculateTax(Long userId, Double taxRate) {
        // 1. 지갑 조회 (실현 손익(realizedProfit)을 과세 표준의 기초로 사용)
        Wallet wallet = walletRepository.findByUserUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        double realizedProfit = wallet.getRealizedProfit() != null
                ? wallet.getRealizedProfit().doubleValue()
                : 0.0;

        // 손실이거나 0이면 과세 표준/세금은 0으로 처리
        double taxBase = realizedProfit > 0.0 ? realizedProfit : 0.0;
        double effectiveTaxRate = (taxRate != null && taxRate > 0.0) ? taxRate : 0.0;
        double estimatedTax = taxBase * effectiveTaxRate;

        return CalcTaxResponse.builder()
                .realizedProfit(realizedProfit)
                .taxBase(taxBase)
                .estimatedTax(estimatedTax)
                // currency는 추후 다통화 전략 도입 시 활성화
                .currency(null)
                .build();
    }
}

