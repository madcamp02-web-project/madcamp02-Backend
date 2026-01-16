package com.madcamp02.domain.wallet;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    // 사용자 ID로 지갑 조회
    Optional<Wallet> findByUserUserId(Long userId);

    // 사용자 ID로 지갑 조회 (비관적 락 - 동시성 제어)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.user.userId = :userId")
    Optional<Wallet> findByUserIdWithLock(@Param("userId") Long userId);

    // 총 자산 기준 상위 N명 조회 (랭킹용)
    @Query("SELECT w FROM Wallet w ORDER BY w.totalAssets DESC")
    List<Wallet> findTopByOrderByTotalAssetsDesc();

    // 실현 수익 기준 상위 N명 조회
    @Query("SELECT w FROM Wallet w WHERE w.realizedProfit > 0 ORDER BY w.realizedProfit DESC")
    List<Wallet> findTopByRealizedProfit();

    // 특정 금액 이상 보유자 조회
    List<Wallet> findByCashBalanceGreaterThanEqual(BigDecimal amount);

    // 게임 코인 보유량 기준 조회
    List<Wallet> findByGameCoinGreaterThanOrderByGameCoinDesc(Integer minCoin);
}