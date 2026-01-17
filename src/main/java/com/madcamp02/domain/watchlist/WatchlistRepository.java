package com.madcamp02.domain.watchlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    // 사용자의 관심종목 목록 (최신순)
    List<Watchlist> findByUserUserIdOrderByAddedAtDesc(Long userId);

    // 특정 종목이 관심종목에 있는지 확인
    Optional<Watchlist> findByUserUserIdAndTicker(Long userId, String ticker);

    // 관심종목 존재 여부
    boolean existsByUserUserIdAndTicker(Long userId, String ticker);

    // 관심종목 삭제
    void deleteByUserUserIdAndTicker(Long userId, String ticker);

    // 사용자의 관심종목 개수
    long countByUserUserId(Long userId);

    // 특정 종목을 관심종목에 등록한 사용자 수
    long countByTicker(String ticker);

    // 인기 관심종목 (많은 사용자가 등록한 순)
    // Native Query 사용
    @org.springframework.data.jpa.repository.Query(
            value = "SELECT ticker, COUNT(*) as cnt FROM watchlist " +
                    "GROUP BY ticker ORDER BY cnt DESC LIMIT :limit",
            nativeQuery = true
    )
    List<Object[]> findPopularTickers(@org.springframework.data.repository.query.Param("limit") int limit);
}