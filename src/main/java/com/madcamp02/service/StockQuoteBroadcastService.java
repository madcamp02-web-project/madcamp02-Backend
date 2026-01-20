package com.madcamp02.service;

import com.madcamp02.external.FinnhubClient;
import com.madcamp02.external.FinnhubClient.QuoteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * StockQuoteBroadcastService - 주식 Quote(OHLC) 실시간 브로드캐스트 서비스
 * 
 * 활성 구독 중인 종목들의 Quote 데이터를 주기적으로 조회하여
 * 시가/고가/저가/종가/전일가를 실시간으로 업데이트하고 브로드캐스트합니다.
 * 
 * 브로드캐스트 주기: 5초마다 (밀리초 단위: 5000ms)
 * 브로드캐스트 토픽: /topic/stock.ticker.{ticker} (기존 trade 토픽과 동일)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockQuoteBroadcastService {

    private final StockSubscriptionManager subscriptionManager;
    private final FinnhubClient finnhubClient;
    private final TradePriceBroadcastService tradePriceBroadcastService;

    /**
     * 활성 구독 중인 종목들의 Quote 데이터를 주기적으로 조회하고 브로드캐스트
     * 
     * 5초마다 실행되어 활성 구독 중인 모든 종목의 OHLC 데이터를 업데이트합니다.
     * 
     * 주의: Finnhub Free Tier의 Rate Limit을 고려하여
     * - 활성 구독 종목이 많을 경우 순차적으로 처리
     * - 에러 발생 시 해당 종목만 스킵하고 계속 진행
     */
    @Scheduled(fixedDelay = 5000) // 5초마다 (밀리초 단위: 5000ms)
    public void broadcastActiveStockQuotes() {
        try {
            // 활성 구독 중인 종목 목록 가져오기
            Set<String> activeTickers = subscriptionManager.getActiveTickers();
            
            if (activeTickers == null || activeTickers.isEmpty()) {
                log.debug("활성 구독 중인 종목이 없습니다. Quote 브로드캐스트 스킵");
                return;
            }

            log.debug("활성 구독 종목 {}개의 Quote 데이터 조회 시작", activeTickers.size());

            // 각 종목의 Quote 데이터 조회 및 브로드캐스트
            for (String ticker : activeTickers) {
                try {
                    // Finnhub Quote API 호출
                    QuoteResponse quote = finnhubClient.getQuote(ticker);
                    
                    if (quote != null) {
                        // Quote 데이터를 브로드캐스트 (OHLC 포함)
                        tradePriceBroadcastService.broadcastQuote(
                            ticker,
                            quote.getCurrentPrice(),
                            quote.getOpen(),
                            quote.getHigh(),
                            quote.getLow(),
                            quote.getPreviousClose(),
                            quote.getChange(),
                            quote.getChangePercent()
                        );
                        
                        log.trace("Quote 브로드캐스트 완료: ticker={}, currentPrice={}, open={}, high={}, low={}, previousClose={}",
                            ticker, quote.getCurrentPrice(), quote.getOpen(), quote.getHigh(), 
                            quote.getLow(), quote.getPreviousClose());
                    }
                } catch (Exception e) {
                    // 개별 종목 에러는 로그만 남기고 계속 진행
                    log.warn("Quote 조회 실패: ticker={}, error={}", ticker, e.getMessage());
                }
            }

            log.debug("활성 구독 종목 {}개의 Quote 브로드캐스트 완료", activeTickers.size());
        } catch (Exception e) {
            log.error("Quote 브로드캐스트 스케줄 실행 중 오류 발생", e);
            // 에러 발생해도 다음 스케줄은 계속 실행되도록 예외를 삼킴
        }
    }
}
