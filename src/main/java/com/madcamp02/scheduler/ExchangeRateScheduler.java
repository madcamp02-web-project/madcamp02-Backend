package com.madcamp02.scheduler;

//======================================
// ExchangeRateScheduler - 환율 수집 배치 스케줄러
//======================================
// - 매일 일정 시간에 ExchangeRateService.collectAndSaveRates 호출
//======================================

import com.madcamp02.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateScheduler {

    private final ExchangeRateService exchangeRateService;

    /**
     * 평일 오전 06:10에 환율 수집 배치를 실행한다.
     * (운영 환경에 맞게 크론 표현식은 조정 가능)
     */
    @Scheduled(cron = "0 10 6 * * MON-FRI")
    public void collectDailyRates() {
        LocalDate today = LocalDate.now();
        log.info("환율 수집 배치 실행 - date={}", today);
        exchangeRateService.collectAndSaveRates(today);
    }
}

