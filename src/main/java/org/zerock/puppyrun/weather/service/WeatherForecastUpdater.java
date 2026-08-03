package org.zerock.puppyrun.weather.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.zerock.puppyrun.common.config.CacheType;
import org.zerock.puppyrun.common.exception.CacheNotFoundException;
import org.zerock.puppyrun.weather.DTO.GridPoint;
import org.zerock.puppyrun.weather.DTO.WeatherApiPara;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.utils.WeatherForecast;
import org.zerock.puppyrun.weather.utils.WeatherRegionCatalog;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 예보 전략을 API 요청, 공통 DTO 변환, 캐시 저장 순서로 실행합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherForecastUpdater {

    private static final int API_CALL_INTERVAL_MILLIS = 1000;

    private final WeatherApiClient weatherApiClient;
    private final WeatherMapper weatherMapper;
    private final CacheManager cacheManager;
    private final WeatherRegionCatalog regionCatalog;

    public void update(WeatherForecast forecast, CacheType cacheType, LocalDateTime requestTime) {
        log.info(
                "날씨 데이터 정기 업데이트 시작: strategy={}, cache={}",
                forecast.getClass().getSimpleName(),
                cacheType.getCacheName()
        );

        Flux.fromIterable(getGridPoints())
                .delayElements(Duration.ofMillis(API_CALL_INTERVAL_MILLIS))
                .concatMap(gridPoint -> update(forecast, cacheType, requestTime, gridPoint))
                .subscribe();
    }

    Mono<WeatherDTO> update(
            WeatherForecast forecast,
            CacheType cacheType,
            LocalDateTime requestTime,
            GridPoint gridPoint
    ) {
        WeatherApiPara para = forecast.getPara(requestTime, gridPoint);

        return weatherApiClient.fetchWeather(para)
                .map(response -> weatherMapper.toWeatherDTO(
                        response,
                        forecast.getFilterCategory()
                ))
                .doOnNext(weather -> putWeatherToCache(cacheType, gridPoint, weather))
                .doOnNext(weather -> log.info(
                        "캐시 갱신 완료: cache={}, nx={}, ny={}",
                        cacheType.getCacheName(),
                        gridPoint.nx(),
                        gridPoint.ny()
                ))
                .onErrorResume(exception -> {
                    log.error(
                            "날씨 갱신 실패 (strategy={}, nx={}, ny={}): {}",
                            forecast.getClass().getSimpleName(),
                            gridPoint.nx(),
                            gridPoint.ny(),
                            exception.getMessage()
                    );
                    return Mono.empty();
                });
    }

    private void putWeatherToCache(
            CacheType cacheType,
            GridPoint gridPoint,
            WeatherDTO weather
    ) {
        Cache cache = cacheManager.getCache(cacheType.getCacheName());
        if (cache == null) {
            throw new CacheNotFoundException("날씨 캐시를 찾을 수 없습니다.");
        }
        cache.put(gridPoint, weather);
    }

    private List<GridPoint> getGridPoints() {
        return regionCatalog.getRegions().stream()
                .map(region -> new GridPoint(region.nx(), region.ny()))
                .distinct()
                .toList();
    }
}
