package com.madcamp02.service;

import com.madcamp02.external.FinnhubClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * StockSubscriptionManager
 *
 * WebSocket 구독 관리자 (Thread-safe)
 * - Active Sessions Map: 사용자별 현재 보고 있는 종목 추적
 * - Subscription Pool: Finnhub WebSocket 구독 중인 심볼 리스트 (Max 50)
 * - LRU 기반 자동 해제: 50개 초과 시 가장 오래된 비활성 종목 해제
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockSubscriptionManager {

    private static final int MAX_FINNHUB_SUBSCRIPTIONS = 50;

    private final FinnhubClient finnhubClient;

    // SessionId -> Map<SubscriptionId, Ticker>
    // 한 세션이 같은 티커를 여러 번 구독(탭 복제 등)할 수도 있고, 다른 티커를 구독할 수도 있음
    // Unsubscribe 이벤트 시 subscriptionId로 찾아서 제거하기 위해 필요
    private final Map<String, Map<String, String>> sessionSubscriptions = new ConcurrentHashMap<>();

    // Ticker -> Set<SessionId> (Active Viewers)
    private final Map<String, Set<String>> tickerSubscribers = new ConcurrentHashMap<>();

    // Finnhub에 구독 중인 Ticker 목록 (LRU 순서 유지를 위해 LinkedHashSet 사용, 동기화 필요)
    private final Set<String> finnhubSubscriptions = Collections.synchronizedSet(new LinkedHashSet<>());

    /**
     * 사용자가 특정 종목(ticker)을 구독할 때 호출
     */
    public void addSubscription(String sessionId, String subscriptionId, String ticker) {
        if (ticker == null || ticker.isEmpty()) return;

        // 1. 세션별 구독 목록에 추가 (SubscriptionId 매핑)
        sessionSubscriptions.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                .put(subscriptionId, ticker);

        // 2. 티커별 구독자(Active Viewers) 목록에 추가
        tickerSubscribers.computeIfAbsent(ticker, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

        // 3. Finnhub 구독 관리
        manageFinnhubSubscription(ticker);
    }

    /**
     * 사용자가 구독을 해제할 때 호출 (SubscriptionId 기준)
     */
    public void removeSubscription(String sessionId, String subscriptionId) {
        if (sessionId == null || subscriptionId == null) return;

        Map<String, String> subs = sessionSubscriptions.get(sessionId);
        if (subs != null) {
            String ticker = subs.remove(subscriptionId);
            if (ticker != null) {
                // 해당 세션이 이 티커를 더 이상 보고 있지 않은지 확인
                // (한 세션이 같은 티커를 여러 SubscriptionId로 구독했을 수 있으므로 확인 필요)
                boolean stillWatching = subs.containsValue(ticker);
                
                if (!stillWatching) {
                    removeTickerSubscriber(sessionId, ticker);
                }

                if (subs.isEmpty()) {
                    sessionSubscriptions.remove(sessionId);
                }
            }
        }
    }

    /**
     * 세션 연결 종료 시 호출 (모든 구독 해제)
     */
    public void handleDisconnect(String sessionId) {
        Map<String, String> subs = sessionSubscriptions.remove(sessionId);
        if (subs != null) {
            // 중복 제거된 티커 목록 추출
            Set<String> uniqueTickers = new HashSet<>(subs.values());
            for (String ticker : uniqueTickers) {
                removeTickerSubscriber(sessionId, ticker);
            }
        }
    }
    
    /**
     * 티커 구독자 목록에서 세션 제거 및 Finnhub 풀 관리 트리거
     */
    private void removeTickerSubscriber(String sessionId, String ticker) {
        Set<String> subscribers = tickerSubscribers.get(ticker);
        if (subscribers != null) {
            subscribers.remove(sessionId);
            if (subscribers.isEmpty()) {
                tickerSubscribers.remove(ticker);
                log.debug("Ticker {} has no active subscribers now. Kept in pool until eviction.", ticker);
            }
        }
    }

    /**
     * Finnhub 구독 풀 관리 (Thread-safe)
     * - 이미 구독 중이면 LRU 순서만 갱신 (Remove -> Add)
     * - 구독 안 되어 있으면 추가 (공간 부족 시 비활성 종목 해제)
     */
    private synchronized void manageFinnhubSubscription(String ticker) {
        if (finnhubSubscriptions.contains(ticker)) {
            // 이미 구독 중: LRU 갱신 (지웠다 다시 넣어서 가장 최근으로 이동)
            finnhubSubscriptions.remove(ticker);
            finnhubSubscriptions.add(ticker);
            return;
        }

        // 신규 구독 필요
        if (finnhubSubscriptions.size() >= MAX_FINNHUB_SUBSCRIPTIONS) {
            // 공간 부족: 해제할 비활성 종목 탐색
            String victim = findEvictionCandidate();
            if (victim != null) {
                log.info("Finnhub Subscription Pool Full. Evicting inactive ticker: {}", victim);
                finnhubClient.unsubscribe(victim);
                finnhubSubscriptions.remove(victim);
            } else {
                log.warn("Finnhub Subscription Pool Full and ALL tickers have active subscribers. Cannot subscribe to {}", ticker);
                // 정책: 모든 슬롯이 활성 상태라면 신규 구독을 거부하거나(현재 로직), 
                // 가장 오래된 활성 종목을 강제로 끊어야 함.
                // 여기서는 기존 사용자 경험 보호를 위해 신규 구독을 Skip.
                return;
            }
        }

        // Finnhub 구독 요청
        finnhubClient.subscribe(ticker);
        finnhubSubscriptions.add(ticker); // 끝에 추가 (Most Recently Used)
    }

    /**
     * 해제할 후보(비활성 종목) 찾기
     * - LRU 순서대로(앞에서부터) 탐색하여 구독자가 없는(0명) 첫 번째 종목 반환
     */
    private String findEvictionCandidate() {
        for (String candidate : finnhubSubscriptions) {
            // 구독자가 없으면(null or empty) 비활성 종목
            if (!tickerSubscribers.containsKey(candidate) || tickerSubscribers.get(candidate).isEmpty()) {
                return candidate;
            }
        }
        return null; // 모두 활성 상태
    }

    /**
     * 활성 구독 중인 종목 목록 반환
     * 
     * @return 활성 구독 중인 종목 심볼 Set (구독자가 1명 이상인 종목)
     */
    public Set<String> getActiveTickers() {
        // tickerSubscribers의 keySet을 반환 (구독자가 있는 종목만)
        return new HashSet<>(tickerSubscribers.keySet());
    }

    // 모니터링/디버깅용
    public int getActiveSessionCount() {
        return sessionSubscriptions.size();
    }
    
    public int getFinnhubSubscriptionCount() {
        return finnhubSubscriptions.size();
    }
}
