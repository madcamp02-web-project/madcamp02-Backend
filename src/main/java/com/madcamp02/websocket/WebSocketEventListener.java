package com.madcamp02.websocket;

import com.madcamp02.service.StockSubscriptionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WebSocketEventListener
 *
 * STOMP 이벤트(구독, 구독해제, 연결종료)를 감지하여
 * StockSubscriptionManager에 세션 상태를 동기화합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final StockSubscriptionManager subscriptionManager;

    // 토픽 패턴: /topic/stock.ticker.{ticker}
    private static final Pattern TICKER_TOPIC_PATTERN = Pattern.compile("^/topic/stock\\.ticker\\.(.+)$");

    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String subscriptionId = headerAccessor.getSubscriptionId();
        String destination = headerAccessor.getDestination();

        if (sessionId == null || destination == null) {
            return;
        }

        Matcher matcher = TICKER_TOPIC_PATTERN.matcher(destination);
        if (matcher.matches()) {
            String ticker = matcher.group(1);
            log.debug("User subscribed to ticker: {} (Session: {}, SubId: {})", ticker, sessionId, subscriptionId);
            subscriptionManager.addSubscription(sessionId, subscriptionId, ticker);
        }
    }

    @EventListener
    public void handleSessionUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String subscriptionId = headerAccessor.getSubscriptionId();

        if (sessionId == null || subscriptionId == null) {
            return;
        }

        log.debug("User unsubscribed (Session: {}, SubId: {})", sessionId, subscriptionId);
        subscriptionManager.removeSubscription(sessionId, subscriptionId);
    }

    @EventListener
    public void handleSessionDisconnectEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        if (sessionId == null) {
            return;
        }

        log.debug("User disconnected (Session: {})", sessionId);
        subscriptionManager.handleDisconnect(sessionId);
    }
}
