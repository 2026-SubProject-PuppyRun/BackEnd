package org.zerock.puppyrun.weather.service;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.zerock.puppyrun.common.config.CacheType;
import org.zerock.puppyrun.common.exception.CacheNotFoundException;
import org.zerock.puppyrun.weather.DTO.GridPoint;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.DTO.WeatherUpdateResult;

/**
 * 날씨 데이터의 캐시 저장, 조회, 삭제를 담당하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherCacheProcess {

    private final CacheManager cacheManager;

    /**
     * 날씨 데이터를 지정된 캐시에 저장합니다.
     *
     * @param cacheType 저장할 캐시 종류
     * @param gridPoint 캐시 키로 사용할 격자 좌표
     * @param weather   저장할 날씨 데이터
     */
    public void putWeather(
            CacheType cacheType,
            GridPoint gridPoint,
            WeatherDTO weather
    ) {
        Objects.requireNonNull(cacheType, "캐시 종류는 필수입니다.");
        Objects.requireNonNull(gridPoint, "격자 좌표는 필수입니다.");
        Objects.requireNonNull(weather, "날씨 정보는 필수입니다.");

        Cache cache = getRequiredCache(cacheType);

        cache.put(gridPoint, weather);

        log.info(
                "[weather 캐시] 성공 날씨 캐시 저장 완료. cache={}, nx={}, ny={}",
                cacheType.getCacheName(),
                gridPoint.nx(),
                gridPoint.ny()
        );
    }

    /**
     * 수집 실패 결과를 실패 전용 캐시에 저장합니다.
     *
     * @param result 실패한 날씨 수집 결과
     */
    public void putFailed(WeatherUpdateResult result) {
        if (result == null || result.success()) {
            throw new IllegalArgumentException("실패한 날씨 수집 결과만 저장할 수 있습니다.");
        }

        Cache cache = getRequiredCache(CacheType.FAILED_WEATHER);
        FailedWeatherKey key = FailedWeatherKey.from(result);

        cache.put(key, result);

        log.info(
                "[weather 캐시] 실패 날씨 캐시 저장 완료. cache={}, type={}, nx={}, ny={}",
                CacheType.FAILED_WEATHER.getCacheName(),
                result.forecast().getType(),
                result.gridPoint().nx(),
                result.gridPoint().ny()
        );
    }

    /**
     * 성공 처리된 예보의 기존 실패 기록을 삭제합니다.
     */
    public void removeFailed(WeatherUpdateResult result) {
        if (result == null) {
            return;
        }

        getRequiredCache(CacheType.FAILED_WEATHER)
                .evict(FailedWeatherKey.from(result));
    }

    /**
     * 실패 전용 캐시에 남아 있는 결과를 조회합니다.
     */
    public List<WeatherUpdateResult> getFailedResults() {
        Object nativeCache = getRequiredCache(CacheType.FAILED_WEATHER).getNativeCache();

        if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache<?, ?> caffeineCache) {
            return caffeineCache.asMap().values().stream()
                    .filter(WeatherUpdateResult.class::isInstance)
                    .map(WeatherUpdateResult.class::cast)
                    .toList();
        }

        if (nativeCache instanceof java.util.Map<?, ?> map) {
            return map.values().stream()
                    .filter(WeatherUpdateResult.class::isInstance)
                    .map(WeatherUpdateResult.class::cast)
                    .toList();
        }

        return List.of();
    }

    /**
     * 실패 전용 캐시를 비웁니다.
     */
    public void clearFailedResults() {
        Cache cache = getRequiredCache(CacheType.FAILED_WEATHER);

        cache.clear();
        log.info("[weather 캐시] 실패 날씨 캐시 초기화 완료");
    }

    private Cache getRequiredCache(CacheType cacheType) {
        Cache cache = cacheManager.getCache(cacheType.getCacheName());

        if (cache == null) {
            throw new CacheNotFoundException(
                    "등록되지 않은 캐시입니다: " + cacheType.getCacheName()
            );
        }

        return cache;
    }

    private record FailedWeatherKey(
            CacheType forecastCacheType,
            GridPoint gridPoint
    ) {
        private static FailedWeatherKey from(WeatherUpdateResult result) {
            return new FailedWeatherKey(
                    result.forecast().getCacheType(),
                    result.gridPoint()
            );
        }
    }
}
