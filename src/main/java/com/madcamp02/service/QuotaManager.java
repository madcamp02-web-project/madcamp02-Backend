package com.madcamp02.service;

import com.madcamp02.domain.log.ApiUsageLog;
import com.madcamp02.domain.log.ApiUsageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaManager {

    private final ApiUsageLogRepository apiUsageLogRepository;

    private static final int EODHD_DAILY_LIMIT = 20;

    /**
     * 특정 Provider의 오늘 Quota가 남아있는지 확인
     */
    @Transactional(readOnly = true)
    public boolean checkQuota(String provider) {
        LocalDate today = LocalDate.now();
        Optional<ApiUsageLog> logOpt = apiUsageLogRepository.findByProviderAndCallDate(provider, today);

        if (logOpt.isEmpty()) {
            return true; // 아직 기록 없으면 OK
        }

        int currentCount = logOpt.get().getCallCount();
        boolean isAvailable = currentCount < EODHD_DAILY_LIMIT;

        if (!isAvailable) {
            log.warn("Quota Exceeded for {}: count={}/{}", provider, currentCount, EODHD_DAILY_LIMIT);
        }

        return isAvailable;
    }

    /**
     * 사용량 증가 (호출 성공 시 실행)
     */
    @Transactional
    public void incrementUsage(String provider) {
        LocalDate today = LocalDate.now();
        ApiUsageLog usageLog = apiUsageLogRepository.findByProviderAndCallDate(provider, today)
                .orElseGet(() -> ApiUsageLog.builder()
                        .provider(provider)
                        .callDate(today)
                        .callCount(0)
                        .build());

        usageLog.incrementCount();
        apiUsageLogRepository.save(usageLog);
        log.info("Quota Updated for {}: count={}", provider, usageLog.getCallCount());
    }
}
