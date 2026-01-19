package com.madcamp02.config;

import com.madcamp02.external.FinnhubTradesWebSocketClient;
import com.madcamp02.service.TradePriceBroadcastService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * FinnhubWebSocketConfig
 *
 * Finnhub WebSocket 클라이언트와 브로드캐스트 서비스를 연결하는 설정 클래스
 * 순환 참조 방지를 위해 별도 설정 클래스에서 초기화
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class FinnhubWebSocketConfig {

    private final FinnhubTradesWebSocketClient webSocketClient;
    private final TradePriceBroadcastService broadcastService;

    @PostConstruct
    public void init() {
        // WebSocket 클라이언트에 브로드캐스트 서비스 연결
        webSocketClient.setBroadcastService(broadcastService);
        log.info("Finnhub WebSocket 클라이언트와 브로드캐스트 서비스 연결 완료");
    }
}
