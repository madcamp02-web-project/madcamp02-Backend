package com.madcamp02.service;

import com.madcamp02.domain.wallet.Wallet;
import com.madcamp02.domain.wallet.WalletRepository;
import com.madcamp02.dto.response.AvailableBalanceResponse;
import com.madcamp02.exception.BusinessException;
import com.madcamp02.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지갑 관련 비즈니스 로직 서비스
 * Phase 4: Trade/Portfolio Engine
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    /**
     * 매수 가능 금액 조회
     * GET /api/v1/trade/available-balance
     */
    @Transactional(readOnly = true)
    public AvailableBalanceResponse getAvailableBalance(Long userId) {
        log.debug("매수 가능 금액 조회: userId={}", userId);
        
        Wallet wallet = walletRepository.findByUserUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        return AvailableBalanceResponse.builder()
                .availableBalance(wallet.getCashBalance().doubleValue())
                .cashBalance(wallet.getCashBalance().doubleValue())
                .currency("USD")
                .build();
    }
}
