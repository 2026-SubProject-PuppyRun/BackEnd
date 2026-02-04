package org.zerock.puppyrun.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        // CacheType Enum에 정의된 설정들을 가져와서 CaffeineCache로 변환
        List<CaffeineCache> caches = Arrays.stream(CacheType.values())
                .map(cache -> new CaffeineCache(
                        cache.getCacheName(),
                        Caffeine.newBuilder()
                                .recordStats() // 통계 기록
                                .expireAfterWrite(cache.getExpiredAfterWrite(), TimeUnit.SECONDS) // 만료 시간
                                .maximumSize(cache.getMaximumSize()) // 최대 크기
                                .build()
                ))
                .collect(Collectors.toList());

        cacheManager.setCaches(caches);
        return cacheManager;
    }
}
