package com.madcamp02.domain.portfolio;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    // 사용자의 전체 포트폴리오 조회
    List<Portfolio> findByUserUserId(Long userId);

    // 사용자의 특정 종목 보유 현황 조회
    Optional<Portfolio> findByUserUserIdAndTicker(Long userId, String ticker);

    // 사용자의 특정 종목 조회 (비관적 락)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Portfolio p WHERE p.user.userId = :userId AND p.ticker = :ticker")
    Optional<Portfolio> findByUserIdAndTickerWithLock(
            @Param("userId") Long userId,
            @Param("ticker") String ticker
    );

    // 특정 종목을 보유한 사용자 목록
    List<Portfolio> findByTicker(String ticker);

    // 특정 종목 보유 여부 확인
    boolean existsByUserUserIdAndTicker(Long userId, String ticker);

    // 사용자의 포트폴리오 삭제
    void deleteByUserUserIdAndTicker(Long userId, String ticker);

    // 수량이 0인 포트폴리오 삭제
    @Query("DELETE FROM Portfolio p WHERE p.quantity = 0")
    void deleteEmptyPortfolios();

    // 사용자의 보유 종목 수 조회
    @Query("SELECT COUNT(p) FROM Portfolio p WHERE p.user.userId = :userId")
    long countByUserId(@Param("userId") Long userId);
}