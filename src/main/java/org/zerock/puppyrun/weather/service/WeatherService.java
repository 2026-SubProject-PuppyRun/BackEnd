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

    private static final int MAX_FORECAST_HOURS = 24;
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

        WeatherDTO.WeatherList forecast = weather.weatherList().stream()
                .filter(weatherList -> weatherList.date().equals(targetDate))
                .filter(weatherList -> weatherList.time().equals(targetTime))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("타겟 시간 : {} {}, 날씨 정보를 찾을 수 없습니다.", targetDate, targetTime);
                    return new WeatherNotFoundException("해당 시간대의 날씨 정보를 찾을 수 없습니다.");
                });

        return new WeatherDTO(List.of(forecast));
    }

    /**
     * 초단기예보를 우선 조회하고 요청 개수를 채울 수 없으면 단기예보로 전환합니다.
     */
    public WeatherDTO getFcstWeather(
            GridPoint gridPoint,
            LocalDateTime now,
            int limit
    ) {
        validateLimit(limit);

        try {
            WeatherDTO ultraShortWeather = getRegionalWeather(
                    CacheType.ULTRA_SHORT_WEATHER,
                    gridPoint,
                    now,
                    limit
            );

            if (ultraShortWeather.weatherList().size() == limit) {
                return ultraShortWeather;
            }

            log.info(
                    "초단기예보 부족으로 단기예보 캐시 조회: nx={}, ny={}, requested={}, available={}",
                    gridPoint.nx(),
                    gridPoint.ny(),
                    limit,
                    ultraShortWeather.weatherList().size()
            );
        } catch (WeatherNotFoundException exception) {
            log.info(
                    "초단기예보 캐시 조회 실패로 단기예보 캐시 조회: nx={}, ny={}, reason={}",
                    gridPoint.nx(),
                    gridPoint.ny(),
                    exception.getMessage()
            );
        }

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

        Cache cache = cacheManager.getCache(cacheType.getCacheName());
        if (cache == null) {
            throw new CacheNotFoundException("날씨 캐시를 찾을 수 없습니다.");
        }

        WeatherDTO cachedWeather = cache.get(gridPoint, WeatherDTO.class);
        if (cachedWeather == null) {
            throw new WeatherNotFoundException("해당 지역의 날씨 정보가 존재하지 않습니다.");
        }

        LocalDateTime startTime = now.truncatedTo(ChronoUnit.HOURS);

        List<WeatherDTO.WeatherList> weatherForecasts = cachedWeather.weatherList().stream()
                .filter(forecast -> !toDateTime(forecast).isBefore(startTime))
                .sorted(Comparator.comparing(this::toDateTime))
                .limit(limit)
                .toList();

        if (weatherForecasts.isEmpty()) {
            throw new WeatherNotFoundException("현재 시간 이후의 날씨 정보가 존재하지 않습니다.");
        }

        return new WeatherDTO(weatherForecasts);
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > MAX_FORECAST_HOURS) {
            throw new InvalidValueException("날씨 조회 개수는 1 이상 24 이하여야 합니다.");
        }
    }

    private LocalDateTime toDateTime(WeatherDTO.WeatherList forecast) {
        return LocalDateTime.parse(
                forecast.date() + forecast.time(),
                FORECAST_DATE_TIME_FORMATTER
        );
    }
}
