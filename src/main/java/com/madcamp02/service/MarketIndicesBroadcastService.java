package com.madcamp02.service;

import com.madcamp02.dto.response.MarketIndicesResponse;
import com.madcamp02.service.cache.CacheResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * MarketIndicesBroadcastService - 시장 지수 실시간 브로드캐스트 서비스
 * 
 * 10초 주기로 주요 지수(SPY/QQQ/DIA) 데이터를 조회하여
 * STOMP 토픽 `/topic/stock.indices`로 브로드캐스트합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketIndicesBroadcastService {

    private final MarketService marketService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 시장 지수 브로드캐스트
     * 
     * 10초마다 실행되어 지수 데이터를 모든 구독자에게 전송합니다.
     */
    @Scheduled(fixedDelay = 10000) // 10초마다 (밀리초 단위: 10000ms)
    public void broadcastIndices() {
        try {
            log.debug("시장 지수 브로드캐스트 시작");
            
            // 기존 MarketService 로직 재사용
            CacheResult<MarketIndicesResponse> cacheResult = marketService.getIndices();
            MarketIndicesResponse response = cacheResult.getData();
            
            // STOMP 브로드캐스트
            messagingTemplate.convertAndSend("/topic/stock.indices", response);
            
            log.debug("시장 지수 브로드캐스트 완료: items={}", response.getItems().size());
        } catch (Exception e) {
            log.error("시장 지수 브로드캐스트 실패", e);
            // 에러 발생해도 다음 스케줄은 계속 실행되도록 예외를 삼킴
        }
    }
}
