package com.madcamp02.service;

import com.madcamp02.domain.stock.StockCandle;
import com.madcamp02.domain.stock.StockCandleRepository;
import com.madcamp02.dto.response.StockCandlesResponse;
import com.madcamp02.external.EodhdClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockService 캔들 데이터 조회 테스트")
class StockServiceCandlesTest {

    @Mock
    private FinnhubClient finnhubClient;

    @Mock
    private EodhdClient eodhdClient;

    @Mock
    private StockCandleRepository stockCandleRepository;

    @Mock
    private QuotaManager quotaManager;

    @InjectMocks
    private StockService stockService;

    private String testTicker = "AAPL";
    private LocalDateTime from;
    private LocalDateTime to;
    private LocalDate fromDate;
    private LocalDate toDate;

    @BeforeEach
    void setUp() {
        from = LocalDateTime.of(2024, 1, 1, 0, 0);
        to = LocalDateTime.of(2024, 1, 31, 23, 59);
        fromDate = from.toLocalDate();
        toDate = to.toLocalDate();
    }

    @Test
    @DisplayName("전체 배치 로드: d 데이터가 없을 때 d, w, m 모두 가져오기")
    void getCandles_batchLoadAllResolutions_whenNoDData() {
        // Given: d 데이터가 없음
        when(stockCandleRepository.existsBySymbolAndPeriod(testTicker, "d")).thenReturn(false);
        when(stockCandleRepository.existsBySymbolAndPeriod(testTicker, "w")).thenReturn(false);
        when(stockCandleRepository.existsBySymbolAndPeriod(testTicker, "m")).thenReturn(false);
        when(quotaManager.checkQuota("EODHD")).thenReturn(true);

        // EODHD API 응답 모킹
        List<EodhdClient.EodhdCandle> mockCandles = createMockCandles(5);
        when(eodhdClient.getHistoricalData(eq(testTicker), eq(fromDate), eq(toDate), anyString(), eq("a")))
                .thenReturn(mockCandles);

        when(stockCandleRepository.findAllBySymbolAndPeriodAndDateBetweenOrderByDateAsc(
                eq(testTicker), eq("d"), eq(fromDate), eq(toDate)))
                .thenReturn(Collections.emptyList());

        // When
        StockCandlesResponse response = stockService.getCandles(testTicker, "d", from, to);

        // Then: d, w, m 각각 1회씩 호출되어야 함 (총 3회)
        verify(eodhdClient, times(3)).getHistoricalData(anyString(), any(), any(), anyString(), anyString());
        // Quota는 1회만 카운트
        verify(quotaManager, times(1)).incrementUsage("EODHD");
        assertNotNull(response);
    }

    @Test
    @DisplayName("부분 배치 로드: d는 있지만 w, m이 없을 때 w, m만 가져오기")
    void getCandles_partialBatchLoad_whenDExistsButWMissing() {
        // Given: d는 있지만 w, m이 없음
        when(stockCandleRepository.existsBySymbolAndPeriod(testTicker, "d")).thenReturn(true);
        when(stockCandleRepository.existsBySymbolAndPeriod(testTicker, "w")).thenReturn(false);
        when(stockCandleRepository.existsBySymbolAndPeriod(testTicker, "m")).thenReturn(false);
        when(quotaManager.checkQuota("EODHD")).thenReturn(true);

        // d 데이터는 이미 있음
        List<StockCandle> existingDCandles = createStockCandles("d", 5);
        when(stockCandleRepository.findAllBySymbolAndPeriodAndDateBetweenOrderByDateAsc(
                eq(testTicker), eq("d"), eq(fromDate), eq(toDate)))
                .thenReturn(existingDCandles);

        // EODHD API 응답 모킹 (w, m만 호출됨)
        List<EodhdClient.EodhdCandle> mockCandles = createMockCandles(5);
        when(eodhdClient.getHistoricalData(eq(testTicker), eq(fromDate), eq(toDate), anyString(), eq("a")))
                .thenReturn(mockCandles);

        // When
        StockCandlesResponse response = stockService.getCandles(testTicker, "d", from, to);

        // Then: w, m만 호출되어야 함 (총 2회)
        verify(eodhdClient, times(2)).getHistoricalData(anyString(), any(), any(), anyString(), anyString());
        // Quota는 1회만 카운트
        verify(quotaManager, times(1)).incrementUsage("EODHD");
        assertNotNull(response);
        assertEquals("d", response.getResolution());
    }

    @Test
    @DisplayName("개별 resolution 보완: 요청된 w resolution만 없을 때")
    void getCandles_singleResolutionLoad_whenRequestedResolutionMissing() {
        // Given: d는 있지만 w가 없음
        when(stockCandleRepository.existsBySymbolAndPeriod(testTicker, "d")).thenReturn(true);
        when(stockCandleRepository.existsBySymbolAndPeriod(testTicker, "w")).thenReturn(false);
        when(stockCandleRepository.existsBySymbolAndPeriod(testTicker, "m")).thenReturn(true);
        when(quotaManager.checkQuota("EODHD")).thenReturn(true);

        // w 데이터가 없음
        when(stockCandleRepository.findAllBySymbolAndPeriodAndDateBetweenOrderByDateAsc(
                eq(testTicker), eq("w"), eq(fromDate), eq(toDate)))
                .thenReturn(Collections.emptyList());

        // EODHD API 응답 모킹
        List<EodhdClient.EodhdCandle> mockCandles = createMockCandles(5);
        when(eodhdClient.getHistoricalData(eq(testTicker), eq(fromDate), eq(toDate), eq("w"), eq("a")))
                .thenReturn(mockCandles);

        // 부분 배치 로드에서 w를 가져온 후, 다시 조회 시 데이터 반환
        List<StockCandle> loadedWCandles = createStockCandles("w", 5);
        when(stockCandleRepository.findAllBySymbolAndPeriodAndDateBetweenOrderByDateAsc(
                eq(testTicker), eq("w"), eq(fromDate), eq(toDate)))
                .thenReturn(Collections.emptyList())  // 첫 조회: 없음
                .thenReturn(loadedWCandles);  // 개별 보완 후 조회: 있음

        // When
        StockCandlesResponse response = stockService.getCandles(testTicker, "w", from, to);

        // Then: w만 호출되어야 함
        verify(eodhdClient, atLeastOnce()).getHistoricalData(eq(testTicker), eq(fromDate), eq(toDate), eq("w"), eq("a"));
        assertNotNull(response);
        assertEquals("w", response.getResolution());
    }

    @Test
    @DisplayName("Quota 초과 시 기존 데이터 반환 (Stale 표시)")
    void getCandles_quotaExceeded_returnsStaleData() {
        // Given: d 데이터는 있지만 Quota 초과
        when(stockCandleRepository.existsBySymbolAndPeriod(testTicker, "d")).thenReturn(true);
        when(stockCandleRepository.existsBySymbolAndPeriod(testTicker, "w")).thenReturn(true);
        when(stockCandleRepository.existsBySymbolAndPeriod(testTicker, "m")).thenReturn(true);
        when(quotaManager.checkQuota("EODHD")).thenReturn(false);

        List<StockCandle> existingCandles = createStockCandles("d", 5);
        when(stockCandleRepository.findAllBySymbolAndPeriodAndDateBetweenOrderByDateAsc(
                eq(testTicker), eq("d"), eq(fromDate), eq(toDate)))
                .thenReturn(existingCandles);

        // When
        StockCandlesResponse response = stockService.getCandles(testTicker, "d", from, to);

        // Then: Stale 표시되어야 함
        assertNotNull(response);
        assertTrue(response.getStale());
        assertEquals("d", response.getResolution());
        // API 호출 없어야 함
        verify(eodhdClient, never()).getHistoricalData(anyString(), any(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Quota 초과 시 기존 데이터도 없으면 예외 발생")
    void getCandles_quotaExceededNoData_throwsException() {
        // Given: 데이터도 없고 Quota도 초과
        when(stockCandleRepository.existsBySymbolAndPeriod(testTicker, "d")).thenReturn(false);
        when(quotaManager.checkQuota("EODHD")).thenReturn(false);

        when(stockCandleRepository.findAllBySymbolAndPeriodAndDateBetweenOrderByDateAsc(
                eq(testTicker), eq("d"), eq(fromDate), eq(toDate)))
                .thenReturn(Collections.emptyList());

        // When & Then: 예외 발생해야 함
        assertThrows(Exception.class, () -> {
            stockService.getCandles(testTicker, "d", from, to);
        });
    }

    @Test
    @DisplayName("중복 호출 방지: 개별 보완 후 Step 6에서 다시 호출하지 않음")
    void getCandles_noDuplicateCall_afterSingleResolutionLoad() {
        // Given: w만 없고, 개별 보완이 실행됨
        when(stockCandleRepository.existsBySymbolAndPeriod(testTicker, "d")).thenReturn(true);
        when(stockCandleRepository.existsBySymbolAndPeriod(testTicker, "w")).thenReturn(false);
        when(stockCandleRepository.existsBySymbolAndPeriod(testTicker, "m")).thenReturn(true);
        when(quotaManager.checkQuota("EODHD")).thenReturn(true);

        // 첫 조회: 없음
        when(stockCandleRepository.findAllBySymbolAndPeriodAndDateBetweenOrderByDateAsc(
                eq(testTicker), eq("w"), eq(fromDate), eq(toDate)))
                .thenReturn(Collections.emptyList())
                .thenReturn(createStockCandles("w", 5));  // 개별 보완 후

        List<EodhdClient.EodhdCandle> mockCandles = createMockCandles(5);
        when(eodhdClient.getHistoricalData(eq(testTicker), eq(fromDate), eq(toDate), eq("w"), eq("a")))
                .thenReturn(mockCandles);

        // When
        StockCandlesResponse response = stockService.getCandles(testTicker, "w", from, to);

        // Then: w는 1회만 호출되어야 함 (개별 보완에서만)
        verify(eodhdClient, times(2)).getHistoricalData(anyString(), any(), any(), eq("w"), anyString()); // 부분 배치(1) + 개별 보완(1)
        assertNotNull(response);
    }

    // Helper methods
    private List<EodhdClient.EodhdCandle> createMockCandles(int count) {
        List<EodhdClient.EodhdCandle> candles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            EodhdClient.EodhdCandle candle = EodhdClient.EodhdCandle.builder()
                    .date(fromDate.plusDays(i))
                    .open(100.0 + i)
                    .high(105.0 + i)
                    .low(95.0 + i)
                    .close(102.0 + i)
                    .volume(1000000L + i)
                    .build();
            candles.add(candle);
        }
        return candles;
    }

    private List<StockCandle> createStockCandles(String period, int count) {
        List<StockCandle> candles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            StockCandle candle = StockCandle.builder()
                    .symbol(testTicker)
                    .date(fromDate.plusDays(i))
                    .period(period)
                    .open(BigDecimal.valueOf(100.0 + i))
                    .high(BigDecimal.valueOf(105.0 + i))
                    .low(BigDecimal.valueOf(95.0 + i))
                    .close(BigDecimal.valueOf(102.0 + i))
                    .volume(1000000L + i)
                    .build();
            candles.add(candle);
        }
        return candles;
    }
}
