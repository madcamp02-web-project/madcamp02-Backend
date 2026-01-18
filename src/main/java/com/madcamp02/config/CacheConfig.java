package com.madcamp02.config;

//======================================
// CacheConfig - Redis 캐시 설정
//======================================
// Spring Cache를 사용하여 Redis에 캐시를 설정합니다.
//
// 캐시 전략:
// - market:indices: 60초 TTL (주요 지수는 자주 변하지 않음)
// - market:news: 300초 TTL (뉴스는 5분마다 갱신)
// - market:movers: 60초 TTL (급등/급락 종목은 자주 변함)
//
// 사용법:
// - Service 메서드에 @Cacheable("market:indices") 어노테이션 추가
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
