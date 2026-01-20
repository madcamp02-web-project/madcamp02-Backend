package com.madcamp02.config;

//======================================
// CacheConfig - Redis 캐시 설정
//======================================
// Spring Cache를 사용하여 Redis에 캐시를 설정합니다.
//
// 주의: Phase 3.6부터 MarketService는 수동 캐싱을 사용합니다.
// - @Cacheable 어노테이션 대신 RedisTemplate을 직접 사용
// - Stale 데이터 처리 및 응답 헤더 추가를 위해 수동 캐싱 필요
// - 이 설정은 다른 서비스에서 @Cacheable을 사용할 경우를 위한 기본 설정
//
// MarketService 캐시 전략 (수동 구현):
// - market:indices: Fresh 60초, Stale 3600초 (1시간)
// - market:news: Fresh 300초 (5분), Stale 3600초 (1시간)
// - market:movers: Fresh 60-300초 (동적), Stale 3600초 (1시간)
//
// 사용법 (다른 서비스):
// - Service 메서드에 @Cacheable("cache-name") 어노테이션 추가
// - Redis에 자동으로 캐시 저장/조회
//======================================

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    //------------------------------------------
    // Redis CacheManager 설정
    //------------------------------------------
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // 기본 캐시 설정 (JSON 직렬화)
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(60))  // 기본 TTL: 60초
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();  // null 값은 캐시하지 않음

        // 캐시별 TTL 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // market:indices - 60초 TTL
        cacheConfigurations.put("market:indices", defaultConfig.entryTtl(Duration.ofSeconds(60)));
        
        // market:news - 300초 TTL (5분)
        cacheConfigurations.put("market:news", defaultConfig.entryTtl(Duration.ofSeconds(300)));
        
        // market:movers - 60초 TTL
        cacheConfigurations.put("market:movers", defaultConfig.entryTtl(Duration.ofSeconds(60)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
