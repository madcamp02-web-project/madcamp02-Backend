package com.madcamp02.service;

import com.madcamp02.domain.user.User;
import com.madcamp02.domain.user.UserRepository;
import com.madcamp02.domain.watchlist.Watchlist;
import com.madcamp02.domain.watchlist.WatchlistRepository;
import com.madcamp02.dto.response.UserWatchlistResponse;
import com.madcamp02.exception.ErrorCode;
import com.madcamp02.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * WatchlistService - 관심종목 관리 서비스
 * 
 * 사용자의 관심종목을 조회/추가/삭제하는 비즈니스 로직을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final UserRepository userRepository;

    /**
     * 내 관심종목 조회
     * 
     * @param userId 사용자 ID
     * @return 관심종목 목록 (최신순)
     */
    @Transactional(readOnly = true)
    public List<Watchlist> getMyWatchlist(Long userId) {
        log.debug("관심종목 조회: userId={}", userId);
        return watchlistRepository.findByUserUserIdOrderByAddedAtDesc(userId);
    }

    /**
     * 관심종목 추가
     * 
     * 이미 존재하는 경우 idempotent하게 무시합니다.
     * 
     * @param userId 사용자 ID
     * @param ticker 종목 코드
     */
    @Transactional
    public void addTicker(Long userId, String ticker) {
        log.debug("관심종목 추가: userId={}, ticker={}", userId, ticker);

        // 중복 확인
        if (watchlistRepository.existsByUserUserIdAndTicker(userId, ticker)) {
            log.debug("이미 관심종목에 등록된 종목: userId={}, ticker={}", userId, ticker);
            return; // idempotent: 이미 있으면 무시
        }

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 관심종목 추가
        Watchlist watchlist = Watchlist.builder()
                .user(user)
                .ticker(ticker)
                .build();
        watchlistRepository.save(watchlist);

        log.info("관심종목 추가 완료: userId={}, ticker={}", userId, ticker);
    }

    /**
     * 관심종목 삭제
     * 
     * 존재하지 않는 경우에도 에러 없이 처리합니다.
     * 
     * @param userId 사용자 ID
     * @param ticker 종목 코드
     */
    @Transactional
    public void removeTicker(Long userId, String ticker) {
        log.debug("관심종목 삭제: userId={}, ticker={}", userId, ticker);
        watchlistRepository.deleteByUserUserIdAndTicker(userId, ticker);
        log.info("관심종목 삭제 완료: userId={}, ticker={}", userId, ticker);
    }

    /**
     * 내 관심종목 조회 (Response DTO 변환)
     * 
     * @param userId 사용자 ID
     * @return UserWatchlistResponse
     */
    @Transactional(readOnly = true)
    public UserWatchlistResponse getMyWatchlistResponse(Long userId) {
        List<Watchlist> watchlists = getMyWatchlist(userId);
        
        List<UserWatchlistResponse.Item> items = watchlists.stream()
                .map(w -> UserWatchlistResponse.Item.builder()
                        .ticker(w.getTicker())
                        .addedAt(w.getAddedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .build())
                .collect(Collectors.toList());

        return UserWatchlistResponse.builder()
                .items(items)
                .build();
    }
}
