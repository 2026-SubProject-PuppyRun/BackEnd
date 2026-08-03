package org.zerock.puppyrun.weather.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.zerock.puppyrun.common.config.CacheType;
import org.zerock.puppyrun.common.exception.CacheNotFoundException;
import org.zerock.puppyrun.common.exception.InvalidValueException;
import org.zerock.puppyrun.weather.DTO.GridPoint;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.exception.WeatherNotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private static final DateTimeFormatter FORECAST_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final CacheManager cacheManager;

    /**
     * 조회된 예보 리스트 중 현재 시간(30분 단위 반올림)에 가장 적합한 데이터를 필터링
     */
    public WeatherDTO getNearestTimeWeather(WeatherDTO weather, LocalDateTime now) {
        LocalDateTime nearestHour = now.plusMinutes(30).truncatedTo(ChronoUnit.HOURS);
        String targetDate = nearestHour.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String targetTime = nearestHour.format(DateTimeFormatter.ofPattern("HHmm"));

        if (!weather.date().equals(targetDate)) {
            throw new WeatherNotFoundException("해당 시간대의 날씨 정보를 찾을 수 없습니다.");
        }

        WeatherDTO.Detail detail = weather.detail().stream()
                .filter(weatherDetail -> weatherDetail.time().equals(targetTime))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("타겟 시간 : {}, 날씨 정보를 찾을 수 없습니다.", targetTime);
                    return new WeatherNotFoundException("해당 시간대의 날씨 정보를 찾을 수 없습니다.");
                });

        return new WeatherDTO(targetDate, List.of(detail));
    }

    /**
     * 초단기예보 캐시에서 현재 정시 이후의 날씨를 조회합니다.
     */
    public WeatherDTO getUltraShortWeather(
            GridPoint gridPoint,
            LocalDateTime now,
            int limit
    ) {
        return getRegionalWeather(CacheType.ULTRA_SHORT_WEATHER, gridPoint, now, limit);
    }

    /**
     * 단기예보 캐시에서 현재 정시 이후의 날씨를 조회합니다.
     */
    public WeatherDTO getShortTermWeather(
            GridPoint gridPoint,
            LocalDateTime now,
            int limit
    ) {
        return getRegionalWeather(CacheType.SHORT_TERM_WEATHER, gridPoint, now, limit);
    }

    private WeatherDTO getRegionalWeather(
            CacheType cacheType,
            GridPoint gridPoint,
            LocalDateTime now,
            int limit
    ) {
        log.info(
                "지역 날씨 예보 호출 진행: cache={}, nx={}, ny={}, time={}",
                cacheType.getCacheName(),
                gridPoint.nx(),
                gridPoint.ny(),
                now
        );

        if (limit < 1) {
            throw new InvalidValueException("날씨 조회 개수는 1 이상이어야 합니다.");
        }

        Cache cache = cacheManager.getCache(cacheType.getCacheName());
        if (cache == null) {
            throw new CacheNotFoundException("날씨 캐시를 찾을 수 없습니다.");
        }

        WeatherDTO cachedWeather = cache.get(gridPoint, WeatherDTO.class);
        if (cachedWeather == null) {
            throw new WeatherNotFoundException("해당 지역의 날씨 정보가 존재하지 않습니다.");
        }

        LocalDateTime startTime = now.truncatedTo(ChronoUnit.HOURS);

        List<WeatherDTO.Detail> weatherForecasts = cachedWeather.detail().stream()
                .filter(detail -> !toDateTime(cachedWeather.date(), detail).isBefore(startTime))
                .sorted(Comparator.comparing(detail -> toDateTime(cachedWeather.date(), detail)))
                .limit(limit)
                .toList();

        if (weatherForecasts.isEmpty()) {
            throw new WeatherNotFoundException("현재 시간 이후의 날씨 정보가 존재하지 않습니다.");
        }

        return new WeatherDTO(cachedWeather.date(), weatherForecasts);
    }

    private LocalDateTime toDateTime(String date, WeatherDTO.Detail detail) {
        return LocalDateTime.parse(
                date + detail.time(),
                FORECAST_DATE_TIME_FORMATTER
        );
    }
}
