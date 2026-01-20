package com.madcamp02.service.cache;

//======================================
// MarketCacheConstants - Market 캐시 상수
//======================================
// Market API의 Redis 캐시 키와 TTL 상수를 관리하는 클래스
//
// 캐시 키 패턴:
// - Fresh: market:indices, market:news, market:movers
// - Stale: market:indices:stale, market:news:stale, market:movers:stale
//======================================

/**
 * Market API Redis 캐시 관련 상수
 */
public final class MarketCacheConstants {
    
    private MarketCacheConstants() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }
    
    //------------------------------------------
    // 캐시 키
    //------------------------------------------
    
    /**
     * 시장 지수 Fresh 캐시 키
     */
    public static final String CACHE_KEY_INDICES = "market:indices";
    
    /**
     * 시장 뉴스 Fresh 캐시 키
     */
    public static final String CACHE_KEY_NEWS = "market:news";
    
    /**
     * 시장 동향 Fresh 캐시 키
     */
    public static final String CACHE_KEY_MOVERS = "market:movers";
    
    /**
     * Stale 캐시 키 접미사
     */
    public static final String STALE_SUFFIX = ":stale";
    
    //------------------------------------------
    // TTL (초 단위)
    //------------------------------------------
    
    /**
     * 시장 지수 Fresh 캐시 TTL: 60초 (1분)
     */
    public static final long TTL_INDICES_FRESH = 60;
    
    /**
     * 시장 뉴스 Fresh 캐시 TTL: 300초 (5분)
     */
    public static final long TTL_NEWS_FRESH = 300;
    
    /**
     * 시장 동향 Fresh 캐시 최소 TTL: 60초 (1분)
     * 변동성이 높을 때 사용
     */
    public static final long TTL_MOVERS_FRESH_MIN = 60;
    
    /**
     * 시장 동향 Fresh 캐시 최대 TTL: 300초 (5분)
     * 변동성이 낮을 때 사용
     */
    public static final long TTL_MOVERS_FRESH_MAX = 300;
    
    /**
     * Stale 캐시 TTL: 3600초 (1시간)
     * 모든 타입에 공통 적용
     */
    public static final long TTL_STALE = 3600;
    
    //------------------------------------------
    // 유틸리티 메서드
    //------------------------------------------
    
    /**
     * Stale 캐시 키 생성
     * 
     * @param freshKey Fresh 캐시 키
     * @return Stale 캐시 키
     */
    public static String getStaleKey(String freshKey) {
        return freshKey + STALE_SUFFIX;
    }
}
