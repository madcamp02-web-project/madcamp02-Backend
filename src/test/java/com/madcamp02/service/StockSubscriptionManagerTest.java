package com.madcamp02.service;

import com.madcamp02.external.FinnhubClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockSubscriptionManagerTest {

    @Mock
    private FinnhubClient finnhubClient;

    @InjectMocks
    private StockSubscriptionManager subscriptionManager;

    @BeforeEach
    void setUp() {
        // Mock FinnhubClient behavior if needed
    }

    @Test
    @DisplayName("구독 추가 및 Finnhub 구독 호출 확인")
    void testAddSubscription() {
        String sessionId = "session1";
        String subId = "sub1";
        String ticker = "AAPL";

        subscriptionManager.addSubscription(sessionId, subId, ticker);

        verify(finnhubClient, times(1)).subscribe(ticker);
        assertEquals(1, subscriptionManager.getFinnhubSubscriptionCount());
        assertEquals(1, subscriptionManager.getActiveSessionCount());
    }

    @Test
    @DisplayName("중복 구독 시 Finnhub 호출은 한 번만 발생해야 함")
    void testDuplicateSubscription() {
        String sessionId1 = "session1";
        String sessionId2 = "session2";
        String ticker = "AAPL";

        subscriptionManager.addSubscription(sessionId1, "sub1", ticker);
        subscriptionManager.addSubscription(sessionId2, "sub2", ticker);

        // Finnhub subscribe는 최초 1회만 호출되어야 함 (LRU 갱신만 발생)
        verify(finnhubClient, times(1)).subscribe(ticker);
        assertEquals(1, subscriptionManager.getFinnhubSubscriptionCount());
    }

    @Test
    @DisplayName("구독 해제 시 Finnhub 구독은 유지되어야 함 (LRU 정책)")
    void testUnsubscribeKeepsFinnhubSubscription() {
        String sessionId = "session1";
        String subId = "sub1";
        String ticker = "AAPL";

        subscriptionManager.addSubscription(sessionId, subId, ticker);
        subscriptionManager.removeSubscription(sessionId, subId);

        // 세션 구독은 해제되었지만, Finnhub 구독 풀에는 남아있어야 함
        verify(finnhubClient, never()).unsubscribe(ticker);
        assertEquals(1, subscriptionManager.getFinnhubSubscriptionCount());
        assertEquals(0, subscriptionManager.getActiveSessionCount());
    }

    @Test
    @DisplayName("LRU 알고리즘: 50개 초과 시 비활성 종목 해제 확인")
    void testLruEviction() {
        // 1. 50개 채우기
        for (int i = 0; i < 50; i++) {
            subscriptionManager.addSubscription("session" + i, "sub" + i, "TICKER_" + i);
        }
        
        verify(finnhubClient, times(50)).subscribe(anyString());
        assertEquals(50, subscriptionManager.getFinnhubSubscriptionCount());

        // 2. 0번 종목의 구독자 제거 (비활성 상태로 만듦)
        subscriptionManager.removeSubscription("session0", "sub0");

        // 3. 51번째 종목 구독 요청
        subscriptionManager.addSubscription("newSession", "newSub", "TICKER_NEW");

        // 4. 검증
        // - 0번 종목이 해제되었는지 확인
        verify(finnhubClient, times(1)).unsubscribe("TICKER_0");
        // - 새 종목이 구독되었는지 확인
        verify(finnhubClient, times(1)).subscribe("TICKER_NEW");
        // - 총 개수는 50개 유지
        assertEquals(50, subscriptionManager.getFinnhubSubscriptionCount());
    }

    @Test
    @DisplayName("LRU 알고리즘: 모든 종목이 활성 상태면 신규 구독 거부(Skip) 확인")
    void testLruRejectIfAllActive() {
        // 1. 50개 채우기 (모두 활성 상태 유지)
        for (int i = 0; i < 50; i++) {
            subscriptionManager.addSubscription("session" + i, "sub" + i, "TICKER_" + i);
        }

        // 2. 51번째 종목 구독 요청
        subscriptionManager.addSubscription("newSession", "newSub", "TICKER_NEW");

        // 3. 검증
        // - 어떤 종목도 해제되지 않아야 함
        verify(finnhubClient, never()).unsubscribe(anyString());
        // - 새 종목은 구독되지 않아야 함 (Skip)
        verify(finnhubClient, never()).subscribe("TICKER_NEW");
        // - 총 개수는 50개 유지
        assertEquals(50, subscriptionManager.getFinnhubSubscriptionCount());
    }

    @Test
    @DisplayName("Disconnect 시 구독 해제 처리 확인")
    void testHandleDisconnect() {
        String sessionId = "session1";
        subscriptionManager.addSubscription(sessionId, "sub1", "AAPL");
        subscriptionManager.addSubscription(sessionId, "sub2", "GOOGL");

        subscriptionManager.handleDisconnect(sessionId);

        assertEquals(0, subscriptionManager.getActiveSessionCount());
        // Finnhub 구독은 유지됨
        assertEquals(2, subscriptionManager.getFinnhubSubscriptionCount());
    }
}
