package com.madcamp02.service;

import com.madcamp02.domain.wallet.Wallet;
import com.madcamp02.domain.wallet.WalletRepository;
import com.madcamp02.dto.response.RankingResponse;
import com.madcamp02.exception.BusinessException;
import com.madcamp02.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 랭킹 서비스.
 * - 랭킹 참여(`is_ranking_joined = true`) 사용자만 대상으로 총자산 기준 Top N을 계산한다.
 * - 초기 자산 10,000을 기준으로 수익률(%)을 계산한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private static final int DEFAULT_LIMIT = 50;
    private static final BigDecimal INITIAL_ASSETS = new BigDecimal("10000.0000");

    private final WalletRepository walletRepository;

    /**
     * 랭킹 조회
     */
    @Transactional(readOnly = true)
    public RankingResponse getRanking(Long userId) {
        List<Wallet> wallets = walletRepository.findRankingWallets(PageRequest.of(0, DEFAULT_LIMIT));

        List<RankingResponse.Item> items = IntStream.range(0, wallets.size())
                .mapToObj(idx -> toItem(idx, wallets.get(idx)))
                .toList();

        RankingResponse.My my = null;
        if (userId != null) {
            Wallet myWallet = walletRepository.findByUserUserId(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            my = RankingResponse.My.builder()
                    .rank(calculateMyRank(items, userId, myWallet.getTotalAssets()))
                    .totalEquity(myWallet.getTotalAssets().doubleValue())
                    .returnPercent(calcReturnPercent(myWallet.getTotalAssets()))
                    .build();
        }

        return RankingResponse.builder()
                .asOf(LocalDateTime.now().toString())
                .items(items)
                .my(my)
                .build();
    }

    private RankingResponse.Item toItem(int index, Wallet wallet) {
        BigDecimal totalAssets = wallet.getTotalAssets();
        return RankingResponse.Item.builder()
                .rank(index + 1)
                .userId(wallet.getUser().getUserId())
                .nickname(wallet.getUser().getNickname())
                .avatarUrl(wallet.getUser().getAvatarUrl())
                .totalEquity(totalAssets.doubleValue())
                .returnPercent(calcReturnPercent(totalAssets))
                .build();
    }

    private Integer calculateMyRank(List<RankingResponse.Item> items, Long userId, BigDecimal myTotalAssets) {
        return items.stream()
                .filter(item -> item.getUserId().equals(userId))
                .findFirst()
                .map(RankingResponse.Item::getRank)
                .orElseGet(() -> {
                    long higher = items.stream()
                            .filter(item -> BigDecimal.valueOf(item.getTotalEquity()).compareTo(myTotalAssets) > 0)
                            .count();
                    return (int) higher + 1;
                });
    }

    private double calcReturnPercent(BigDecimal totalAssets) {
        return totalAssets.subtract(INITIAL_ASSETS)
                .divide(INITIAL_ASSETS, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }
}

