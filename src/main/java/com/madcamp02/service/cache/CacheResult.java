package com.madcamp02.service.cache;

//======================================
// CacheResult - 캐시 결과 래퍼 클래스
//======================================
// Redis 캐싱 결과를 캐시 상태, Age, Freshness와 함께 반환하는 래퍼 클래스
//
// 사용 목적:
// - 캐시 Hit/Miss/Stale 상태를 Controller에 전달
// - 응답 헤더(X-Cache-Status, X-Cache-Age, X-Data-Freshness) 생성에 사용
// - API 실패 시 Stale 데이터 Fallback 처리
//======================================

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 캐시 조회 결과를 래핑하는 제네릭 클래스
 * 
 * @param <T> 캐시된 데이터 타입 (예: MarketIndicesResponse, MarketNewsResponse 등)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheResult<T> {
    
    /**
     * 실제 응답 데이터
     */
    private T data;
    
    /**
     * 캐시 상태 (HIT, MISS, STALE)
     */
    private CacheStatus cacheStatus;
    
    /**
     * 캐시 생성 후 경과 시간 (초 단위)
     */
    private long cacheAge;
    
    /**
     * 데이터 신선도 (FRESH, STALE, EXPIRED)
     */
    private DataFreshness dataFreshness;
    
    /**
     * 캐시 상태 열거형
     */
    public enum CacheStatus {
        HIT,    // 캐시에서 조회 성공 (유효한 TTL 내)
        MISS,   // 캐시 미스 (API 호출 필요)
        STALE   // Stale 캐시에서 조회 (TTL 만료되었지만 사용 가능)
    }
    
    /**
     * 데이터 신선도 열거형
     */
    public enum DataFreshness {
        FRESH,      // 신선한 데이터 (TTL 유효)
        STALE,      // 오래된 데이터 (TTL 만료되었지만 Stale 키에 보관)
        EXPIRED     // 만료된 데이터 (사용 불가)
    }
    
    //------------------------------------------
    // 정적 팩토리 메서드
    //------------------------------------------
    
    /**
     * 캐시 Hit 결과 생성
     * 
     * @param data 캐시된 데이터
     * @param cacheAge 캐시 생성 후 경과 시간 (초)
     * @return CacheResult (HIT, FRESH)
     */
    public static <T> CacheResult<T> hit(T data, long cacheAge) {
        return CacheResult.<T>builder()
                .data(data)
                .cacheStatus(CacheStatus.HIT)
                .cacheAge(cacheAge)
                .dataFreshness(DataFreshness.FRESH)
                .build();
    }
    
    /**
     * 캐시 Miss 결과 생성 (API 호출 후)
     * 
     * @param data API에서 조회한 데이터
     * @return CacheResult (MISS, FRESH)
     */
    public static <T> CacheResult<T> miss(T data) {
        return CacheResult.<T>builder()
                .data(data)
                .cacheStatus(CacheStatus.MISS)
                .cacheAge(0)
                .dataFreshness(DataFreshness.FRESH)
                .build();
    }
    
    /**
     * Stale 캐시 결과 생성
     * 
     * @param data Stale 캐시에서 조회한 데이터
     * @param cacheAge 캐시 생성 후 경과 시간 (초)
     * @return CacheResult (STALE, STALE)
     */
    public static <T> CacheResult<T> stale(T data, long cacheAge) {
        return CacheResult.<T>builder()
                .data(data)
                .cacheStatus(CacheStatus.STALE)
                .cacheAge(cacheAge)
                .dataFreshness(DataFreshness.STALE)
                .build();
    }
}
