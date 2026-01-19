package com.madcamp02.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.madcamp02.service.TradePriceBroadcastService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FinnhubTradesWebSocketClient
 *
 * Finnhub Trades WebSocket을 단일 연결로 관리하는 싱글톤 클라이언트
 * - API 키당 1개 연결 보장
 * - 지수 백오프 재연결 전략
 * - 구독 버퍼링 및 재구독 지원
 * - 메시지 수신 및 파싱
 */
@Slf4j
@Component
public class FinnhubTradesWebSocketClient {

    private static final String WEBSOCKET_URL = "wss://ws.finnhub.io";
    private static final int MAX_RECONNECT_DELAY_SECONDS = 30;
    private static final int INITIAL_RECONNECT_DELAY_SECONDS = 1;

    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final Set<String> activeSubscriptions = ConcurrentHashMap.<String>newKeySet();
    private final Set<String> pendingSubscriptions = ConcurrentHashMap.<String>newKeySet();
    
    private OkHttpClient httpClient;
    private WebSocket webSocket;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    
    // 메시지 수신을 위한 콜백 인터페이스
    private TradeMessageHandler messageHandler;
    
    // TradePriceBroadcastService (순환 참조 방지를 위해 setter injection 사용)
    private TradePriceBroadcastService broadcastService;
    
    // 재연결 스케줄러
    private ScheduledExecutorService reconnectScheduler;

    public FinnhubTradesWebSocketClient(
            @Value("${finnhub.api-key}") String apiKey,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    /**
     * 메시지 핸들러 인터페이스
     */
    public interface TradeMessageHandler {
        void handleTrade(String symbol, double price, long timestamp, double volume, String[] conditions);
    }

    /**
     * 메시지 핸들러 설정
     */
    public void setMessageHandler(TradeMessageHandler handler) {
        this.messageHandler = handler;
    }

    /**
     * 브로드캐스트 서비스 설정 (순환 참조 방지)
     */
    public void setBroadcastService(TradePriceBroadcastService broadcastService) {
        this.broadcastService = broadcastService;
    }

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isEmpty() || "sandbox_api_key".equals(apiKey)) {
            log.warn("Finnhub API 키가 설정되지 않았거나 sandbox 모드입니다. WebSocket 연결을 건너뜁니다.");
            return;
        }

        httpClient = new OkHttpClient.Builder()
                .pingInterval(20, TimeUnit.SECONDS) // Keep-alive ping
                .build();

        reconnectScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "finnhub-websocket-reconnect");
            t.setDaemon(true);
            return t;
        });

        // 비동기로 초기 연결 시도
        connectAsync();
    }

    @PreDestroy
    public void destroy() {
        disconnect();
        if (reconnectScheduler != null) {
            reconnectScheduler.shutdown();
        }
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
        }
    }

    /**
     * 비동기 연결 시도
     */
    private void connectAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                connect();
            } catch (Exception e) {
                log.error("Finnhub WebSocket 초기 연결 실패", e);
                scheduleReconnect();
            }
        });
    }

    /**
     * WebSocket 연결
     */
    private void connect() {
        if (isConnected.get()) {
            log.debug("이미 연결되어 있습니다.");
            return;
        }

        String url = WEBSOCKET_URL + "?token=" + apiKey;
        log.info("Finnhub WebSocket 연결 시도: {}", url.replace(apiKey, "***"));

        Request request = new Request.Builder()
                .url(url)
                .build();

        webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("Finnhub WebSocket 연결 성공");
                isConnected.set(true);
                reconnectAttempts.set(0);
                
                // 연결 성공 시 pending 구독들을 flush
                flushPendingSubscriptions();
                
                // 기존 활성 구독들을 재구독
                resubscribeActiveSubscriptions();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleMessage(text);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                log.warn("Finnhub WebSocket 연결 종료 중: code={}, reason={}", code, reason);
                isConnected.set(false);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                log.warn("Finnhub WebSocket 연결 종료됨: code={}, reason={}", code, reason);
                isConnected.set(false);
                scheduleReconnect();
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("Finnhub WebSocket 연결 실패", t);
                isConnected.set(false);
                scheduleReconnect();
            }
        });
    }

    /**
     * 메시지 처리
     */
    private void handleMessage(String text) {
        try {
            JsonNode root = objectMapper.readTree(text);
            String type = root.path("type").asText();
            
            if (!"trade".equals(type)) {
                log.debug("알 수 없는 메시지 타입: {}", type);
                return;
            }

            JsonNode dataArray = root.path("data");
            if (!dataArray.isArray() || dataArray.size() == 0) {
                log.debug("빈 data 배열 또는 data 필드 없음");
                return;
            }

            // data 배열의 각 trade 처리
            for (JsonNode trade : dataArray) {
                String symbol = trade.path("s").asText();
                double price = trade.path("p").asDouble(0.0);
                long timestamp = trade.path("t").asLong(0);
                double volume = trade.path("v").asDouble(0.0);
                
                // conditions 배열 파싱
                JsonNode conditionsNode = trade.path("c");
                String[] conditions = null;
                if (conditionsNode.isArray()) {
                    conditions = new String[conditionsNode.size()];
                    for (int i = 0; i < conditionsNode.size(); i++) {
                        conditions[i] = conditionsNode.get(i).asText();
                    }
                }

                if (symbol == null || symbol.isEmpty()) {
                    log.debug("심볼이 없는 trade 메시지 무시");
                    continue;
                }

                // 브로드캐스트 서비스에 전달 (우선)
                if (broadcastService != null) {
                    broadcastService.broadcastTrade(symbol, price, timestamp, volume, conditions);
                }
                
                // 커스텀 핸들러에도 전달 (선택적)
                if (messageHandler != null) {
                    messageHandler.handleTrade(symbol, price, timestamp, volume, conditions);
                }
            }
        } catch (Exception e) {
            log.error("Finnhub WebSocket 메시지 파싱 실패: {}", text, e);
        }
    }

    /**
     * 구독 요청
     */
    public void subscribe(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            log.warn("빈 심볼로 구독 요청 무시");
            return;
        }

        activeSubscriptions.add(symbol);

        if (isConnected.get() && webSocket != null) {
            sendSubscribeMessage(symbol);
        } else {
            // 연결 전이거나 재연결 중이면 pending에 추가
            pendingSubscriptions.add(symbol);
            log.debug("연결되지 않아 구독 요청을 pending에 추가: {}", symbol);
        }
    }

    /**
     * 구독 해제 요청
     */
    public void unsubscribe(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            return;
        }

        activeSubscriptions.remove(symbol);
        pendingSubscriptions.remove(symbol);

        if (isConnected.get() && webSocket != null) {
            sendUnsubscribeMessage(symbol);
        }
    }

    /**
     * 구독 메시지 전송
     */
    private void sendSubscribeMessage(String symbol) {
        try {
            String message = String.format("{\"type\":\"subscribe\",\"symbol\":\"%s\"}", symbol);
            if (webSocket != null) {
                webSocket.send(message);
                log.debug("Finnhub 구독 메시지 전송: {}", symbol);
            }
        } catch (Exception e) {
            log.error("구독 메시지 전송 실패: {}", symbol, e);
        }
    }

    /**
     * 구독 해제 메시지 전송
     */
    private void sendUnsubscribeMessage(String symbol) {
        try {
            String message = String.format("{\"type\":\"unsubscribe\",\"symbol\":\"%s\"}", symbol);
            if (webSocket != null) {
                webSocket.send(message);
                log.debug("Finnhub 구독 해제 메시지 전송: {}", symbol);
            }
        } catch (Exception e) {
            log.error("구독 해제 메시지 전송 실패: {}", symbol, e);
        }
    }

    /**
     * Pending 구독들을 flush
     */
    private void flushPendingSubscriptions() {
        if (pendingSubscriptions.isEmpty()) {
            return;
        }

        log.info("Pending 구독 {}개를 flush합니다.", pendingSubscriptions.size());
        // 현재 pending 목록을 복사해둔 뒤, 원본은 비움
        Set<String> toSubscribe = new HashSet<>(pendingSubscriptions);
        pendingSubscriptions.clear();

        for (String symbol : toSubscribe) {
            sendSubscribeMessage(symbol);
        }
    }

    /**
     * 활성 구독들을 재구독
     */
    private void resubscribeActiveSubscriptions() {
        if (activeSubscriptions.isEmpty()) {
            return;
        }

        log.info("활성 구독 {}개를 재구독합니다.", activeSubscriptions.size());
        for (String symbol : activeSubscriptions) {
            sendSubscribeMessage(symbol);
        }
    }

    /**
     * 재연결 스케줄링 (지수 백오프)
     */
    private void scheduleReconnect() {
        if (!isConnected.get()) {
            int attempts = reconnectAttempts.getAndIncrement();
            int delaySeconds = Math.min(
                    INITIAL_RECONNECT_DELAY_SECONDS * (1 << attempts),
                    MAX_RECONNECT_DELAY_SECONDS
            );

            log.info("{}초 후 재연결 시도 (시도 횟수: {})", delaySeconds, attempts + 1);

            reconnectScheduler.schedule(() -> {
                if (!isConnected.get()) {
                    connect();
                }
            }, delaySeconds, TimeUnit.SECONDS);
        }
    }

    /**
     * 연결 종료
     */
    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Normal closure");
            webSocket = null;
        }
        isConnected.set(false);
    }

    /**
     * 연결 상태 확인
     */
    public boolean isConnected() {
        return isConnected.get();
    }

    /**
     * 현재 활성 구독 수
     */
    public int getActiveSubscriptionCount() {
        return activeSubscriptions.size();
    }
}
