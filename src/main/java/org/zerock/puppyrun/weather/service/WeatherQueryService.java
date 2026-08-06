package org.zerock.puppyrun.weather.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.zerock.puppyrun.common.config.CacheType;
import org.zerock.puppyrun.common.exception.InvalidValueException;
import org.zerock.puppyrun.weather.DTO.GridPoint;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.enity.WeatherForecastEntity;
import org.zerock.puppyrun.weather.exception.WeatherNotFoundException;
import org.zerock.puppyrun.weather.repository.WeatherForecastRepository;
import org.zerock.puppyrun.weather.utils.WeatherForecast.ForecastType;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherQueryService {

    private static final int MIN_FORECAST_HOURS = 1;
    private static final int MAX_FORECAST_HOURS = 24;

    private static final DateTimeFormatter FORECAST_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final CacheManager cacheManager;
    private final WeatherForecastRepository weatherForecastRepository;

    public WeatherDTO getNearestTimeWeather(
            WeatherDTO weather,
            LocalDateTime now
    ) {
        validateWeather(weather);

        if (now == null) {
            throw new InvalidValueException("조회 기준 시간은 필수입니다.");
        }

        WeatherDTO.WeatherList nearestForecast = weather.weatherList().stream()
                .filter(this::hasValidDateTime)
                .filter(forecast -> !toDateTime(forecast).isBefore(now))
                .min(Comparator.comparing(this::toDateTime))
                .orElseThrow(() ->
                        new WeatherNotFoundException(
                                "현재 이후의 날씨 정보를 찾을 수 없습니다."
                        )
                );

        return new WeatherDTO(List.of(nearestForecast));
    }

    public WeatherDTO getFcstWeather(
            GridPoint gridPoint,
            LocalDateTime now,
            int limit
    ) {
        validateRequest(gridPoint, now, limit);

        LocalDateTime startTime = now.truncatedTo(ChronoUnit.HOURS); // 시간 내림 10:26 -> 10:00
        LocalDateTime endTime = startTime.plusHours(limit - 1L);

        Map<LocalDateTime, WeatherDTO.WeatherList> forecasts = new TreeMap<>();

        // 1. 초단기예보 캐시 조회
        addCacheForecasts(
                CacheType.ULTRA_SHORT_WEATHER,
                gridPoint,
                startTime,
                endTime,
                forecasts
        );

        log.info(
                "초단기예보 캐시 조회. nx={}, ny={}, 수집={}/{}",
                gridPoint.nx(),
                gridPoint.ny(),
                forecasts.size(),
                limit
        );

        // 2. 부족하면 단기예보 캐시 조회 후 추가
        if (isMissing(forecasts, startTime, limit)) {
            addCacheForecasts(
                    CacheType.SHORT_TERM_WEATHER,
                    gridPoint,
                    startTime,
                    endTime,
                    forecasts
            );

            log.info(
                    "초단기예보 누락으로 단기예보 캐시 조회. nx={}, ny={}, 수집={}/{}",
                    gridPoint.nx(),
                    gridPoint.ny(),
                    forecasts.size(),
                    limit
            );
        }

        // 3. 부족하면 DB 조회 후 추가
        if (isMissing(forecasts, startTime, limit)) {

            addDbForecasts(
                    ForecastType.SHORT_TERM,
                    gridPoint,
                    startTime,
                    endTime,
                    forecasts
            );

            log.info(
                    "단기 예보 누락으로 단기예보 DB 조회. nx={}, ny={}, 수집={}/{}",
                    gridPoint.nx(),
                    gridPoint.ny(),
                    forecasts.size(),
                    limit
            );
        }

        // 4. 시간 순서대로 최종 응답 생성
        List<WeatherDTO.WeatherList> result =
                createForecastList(
                        forecasts,
                        startTime,
                        limit
                );

        // 조회한 시간중 없는 시간대가 있으면 예외 처리
        if (result.stream().anyMatch(Objects::isNull)) {
            log.warn(
                    "모든 데이터 출처 조회 후에도 날씨 정보가 누락되었습니다. nx={}, ny={}, start={}, end={}, availableTimes={}",
                    gridPoint.nx(),
                    gridPoint.ny(),
                    startTime,
                    endTime,
                    forecasts.keySet()
            );

            throw new WeatherNotFoundException("요청한 시간대의 날씨 정보가 일부 누락되었습니다.");
        }

        return new WeatherDTO(result);
    }

    public List<WeatherForecastEntity> findAllByBaseDateTimeAndType(LocalDateTime baseDateTime, ForecastType type) {
        return weatherForecastRepository.findAllByBaseDateTimeAndType(baseDateTime, type);
    }

    /**
     * 지정된 캐시의 날씨를 조회하여 기존 예보에 추가합니다.
     */
    private void addCacheForecasts(
            CacheType cacheType,
            GridPoint gridPoint,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Map<LocalDateTime, WeatherDTO.WeatherList> forecasts
    ) {
        Cache cache = cacheManager.getCache(cacheType.getCacheName());

        if (cache == null) {
            log.error(
                    "등록되지 않은 캐시입니다. cacheName={}",
                    cacheType.getCacheName()
            );
            return;
        }

        WeatherDTO cachedWeather = cache.get(
                gridPoint,
                WeatherDTO.class
        );

        if (cachedWeather == null) {
            log.debug(
                    "날씨 캐시 미스. cacheName={}, nx={}, ny={}",
                    cacheType.getCacheName(),
                    gridPoint.nx(),
                    gridPoint.ny()
            );
            return;
        }

        addForecasts(
                cachedWeather.weatherList(),
                startTime,
                endTime,
                forecasts
        );
    }

    /**
     * DB의 최신 단기예보를 조회하여 기존 예보에 추가합니다.
     */
    private void addDbForecasts(
            ForecastType forecastType,
            GridPoint gridPoint,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Map<LocalDateTime, WeatherDTO.WeatherList> forecasts
    ) {
        weatherForecastRepository
                .findFirstByNxAndNyAndTypeOrderByBaseDateTimeDesc(
                        gridPoint.nx(),
                        gridPoint.ny(),
                        forecastType
                )
                .map(WeatherForecastEntity::getWeather)
                .ifPresentOrElse(
                        weather -> addForecasts(
                                weather.weatherList(),
                                startTime,
                                endTime,
                                forecasts
                        ),
                        () -> log.debug(
                                "DB 백업 날씨가 없습니다. nx={}, ny={}",
                                gridPoint.nx(),
                                gridPoint.ny()
                        )
                );
    }

    /**
     * 조회 범위에 포함되는 날씨만 기존 예보에 추가합니다.
     *
     * <p>이미 같은 시간대의 예보가 있으면 기존 값을 유지합니다.</p>
     */
    private void addForecasts(
            List<WeatherDTO.WeatherList> weatherList,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Map<LocalDateTime, WeatherDTO.WeatherList> forecasts
    ) {
        if (weatherList == null || weatherList.isEmpty()) {
            return;
        }

        for (WeatherDTO.WeatherList weather : weatherList) {
            if (!hasValidDateTime(weather)) {
                continue;
            }

            LocalDateTime forecastTime = toDateTime(weather);

            if (forecastTime.isBefore(startTime) || forecastTime.isAfter(endTime)) {
                continue;
            }

            // 조회 후 없으면 추가
            forecasts.putIfAbsent(
                    forecastTime,
                    weather
            );
        }
    }

    /**
     * 요청한 시간대 중 누락된 날씨가 있는지 확인합니다.
     */
    private boolean isMissing(
            Map<LocalDateTime, WeatherDTO.WeatherList> forecasts,
            LocalDateTime startTime,
            int limit
    ) {
        for (int hour = 0; hour < limit; hour++) {
            LocalDateTime requiredTime = startTime.plusHours(hour);

            if (!forecasts.containsKey(requiredTime)) {
                return true;
            }
        }

        return false;
    }


    /**
     * 요청한 시간 순서대로 날씨 목록을 생성합니다.
     */
    private List<WeatherDTO.WeatherList> createForecastList(
            Map<LocalDateTime, WeatherDTO.WeatherList> forecasts,
            LocalDateTime startTime,
            int limit
    ) {
        List<WeatherDTO.WeatherList> result = new ArrayList<>(limit);

        for (int hour = 0; hour < limit; hour++) {
            LocalDateTime forecastTime = startTime.plusHours(hour);
            result.add(forecasts.get(forecastTime));
        }

        return result;
    }

    private boolean hasValidDateTime(
            WeatherDTO.WeatherList weather
    ) {
        if (weather == null || weather.date() == null || weather.time() == null) {
            return false;
        }

        try {
            toDateTime(weather);
            return true;

        } catch (RuntimeException exception) {
            log.warn(
                    "잘못된 예보 시간 형식입니다. date={}, time={}",
                    weather.date(),
                    weather.time()
            );
            return false;
        }
    }

    private LocalDateTime toDateTime(
            WeatherDTO.WeatherList weather
    ) {
        return LocalDateTime.parse(
                weather.date() + weather.time(),
                FORECAST_DATE_TIME_FORMATTER
        );
    }

    private void validateRequest(
            GridPoint gridPoint,
            LocalDateTime now,
            int limit
    ) {
        if (gridPoint == null) {
            throw new InvalidValueException(
                    "격자 좌표는 필수입니다."
            );
        }

        if (now == null) {
            throw new InvalidValueException(
                    "조회 기준 시간은 필수입니다."
            );
        }

        if (limit < MIN_FORECAST_HOURS
                || limit > MAX_FORECAST_HOURS) {
            throw new InvalidValueException(
                    "날씨 조회 개수는 "
                            + MIN_FORECAST_HOURS
                            + " 이상 "
                            + MAX_FORECAST_HOURS
                            + " 이하여야 합니다."
            );
        }
    }

    private void validateWeather(WeatherDTO weather) {
        if (weather == null
                || weather.weatherList() == null
                || weather.weatherList().isEmpty()) {
            throw new WeatherNotFoundException(
                    "조회할 날씨 정보가 존재하지 않습니다."
            );
        }
    }
}
