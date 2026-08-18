package org.zerock.puppyrun.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.zerock.puppyrun.common.config.CacheType;
import org.zerock.puppyrun.common.exception.ErrorCode;
import org.zerock.puppyrun.common.exception.InvalidValueException;
import org.zerock.puppyrun.weather.DTO.GridPoint;
import org.zerock.puppyrun.weather.DTO.PrecipitationType;
import org.zerock.puppyrun.weather.DTO.SkyType;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.enity.WeatherForecastEntity;
import org.zerock.puppyrun.weather.exception.WeatherNotFoundException;
import org.zerock.puppyrun.weather.repository.WeatherForecastRepository;
import org.zerock.puppyrun.weather.service.WeatherQueryService;

class WeatherQueryServiceTest {

    private final CacheManager cacheManager = new ConcurrentMapCacheManager(
            CacheType.ULTRA_SHORT_WEATHER.getCacheName(),
            CacheType.SHORT_TERM_WEATHER.getCacheName()
    );
    private final WeatherForecastRepository weatherForecastRepository = mock(WeatherForecastRepository.class);
    private final WeatherQueryService weatherQueryService = new WeatherQueryService(cacheManager,
            weatherForecastRepository);

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    @Test
    @DisplayName("초단기 캐시에 있는 데이터는 우선 사용하고 부족한 시간대는 단기 캐시에서 추가적으로 보완한다")
    void mergeUltraShortAndShortTermCacheUsingMap() {
        // given
        GridPoint gridPoint = new GridPoint(98, 76);
        Cache ultraShortCache = cacheManager.getCache(CacheType.ULTRA_SHORT_WEATHER.getCacheName());
        Cache shortTermCache = cacheManager.getCache(CacheType.SHORT_TERM_WEATHER.getCacheName());
        assertThat(ultraShortCache).isNotNull();
        assertThat(shortTermCache).isNotNull();

        // 초단기예보: 10:00, 11:00 (2개 존재)
        ultraShortCache.put(gridPoint, weatherWithTemp(
                "20260803", "ultra", "1000", "1100"
        ));
        // 단기예보: 10:00, 11:00, 12:00, 13:00 (4개 존재)
        shortTermCache.put(gridPoint, weatherWithTemp(
                "20260803", "short", "1000", "1100", "1200", "1300"
        ));

        // when
        WeatherDTO result = weatherQueryService.getFcstWeather(
                gridPoint,
                LocalDateTime.of(2026, 8, 3, 10, 15),
                4
        );

        // then: 10:00, 11:00은 초단기(ultra) 데이터 유지, 12:00, 13:00은 단기(short) 데이터로 보완
        assertThat(result.weatherList())
                .extracting(WeatherDTO.WeatherList::temp)
                .containsExactly("ultra", "ultra", "short", "short");

        assertThat(result.weatherList())
                .extracting(WeatherDTO.WeatherList::time)
                .containsExactly("1000", "1100", "1200", "1300");
    }

    @Test
    @DisplayName("초단기 및 단기 캐시 데이터로도 부족하면 DB 데이터로 남은 시간대를 보완 병합한다")
    void mergeCacheAndDbUsingMapWhenLimitNotMet() {
        // given
        GridPoint gridPoint = new GridPoint(98, 76);
        LocalDateTime now = LocalDateTime.of(2026, 8, 3, 10, 15);
        Cache ultraShortCache = cacheManager.getCache(CacheType.ULTRA_SHORT_WEATHER.getCacheName());
        Cache shortTermCache = cacheManager.getCache(CacheType.SHORT_TERM_WEATHER.getCacheName());
        assertThat(ultraShortCache).isNotNull();
        assertThat(shortTermCache).isNotNull();

        // 초단기: 10:00 (1개)
        ultraShortCache.put(gridPoint, weatherWithTemp("20260803", "ultra", "1000"));
        // 단기 캐시: 11:00 (1개)
        shortTermCache.put(gridPoint, weatherWithTemp("20260803", "short", "1100"));
        // DB 백업: 12:00, 13:00 (2개)
        WeatherDTO dbWeatherData = weatherWithTemp("20260803", "db", "1200", "1300");
        WeatherForecastEntity mockEntity = WeatherForecastEntity.builder()
                .baseDateTime(now)
                .nx(98)
                .ny(76)
                .type(org.zerock.puppyrun.weather.utils.WeatherForecast.ForecastType.SHORT_TERM)
                .weather(dbWeatherData)
                .build();
        given(weatherForecastRepository.findLatestAndPreviousByGridPoint(
                gridPoint, org.zerock.puppyrun.weather.utils.WeatherForecast.ForecastType.SHORT_TERM
        )).willReturn(List.of(mockEntity));

        // when (limit = 4)
        WeatherDTO result = weatherQueryService.getFcstWeather(gridPoint, now, 4);

        // then: 초단기(ultra 10시) -> 단기캐시(short 11시) -> DB(db 12시, 13시) 조합
        assertThat(result.weatherList())
                .extracting(WeatherDTO.WeatherList::temp)
                .containsExactly("ultra", "short", "db", "db");
    }

    @Test
    @DisplayName("최신 단기예보에 없는 시간은 직전 단기예보로 보완하고 겹치는 시간은 최신 값을 사용한다")
    void mergeLatestAndPreviousDbForecasts() {
        // given
        GridPoint gridPoint = new GridPoint(63, 103);
        LocalDateTime now = LocalDateTime.of(2026, 8, 18, 17, 35);
        org.zerock.puppyrun.weather.utils.WeatherForecast.ForecastType forecastType =
                org.zerock.puppyrun.weather.utils.WeatherForecast.ForecastType.SHORT_TERM;

        WeatherForecastEntity latestForecast = WeatherForecastEntity.builder()
                .baseDateTime(LocalDateTime.of(2026, 8, 18, 14, 0))
                .nx(gridPoint.nx())
                .ny(gridPoint.ny())
                .type(forecastType)
                .weather(weatherWithTemp("20260818", "latest", "1800", "1900"))
                .build();
        WeatherForecastEntity previousForecast = WeatherForecastEntity.builder()
                .baseDateTime(LocalDateTime.of(2026, 8, 18, 11, 0))
                .nx(gridPoint.nx())
                .ny(gridPoint.ny())
                .type(forecastType)
                .weather(weatherWithTemp("20260818", "previous", "1700", "1800", "1900"))
                .build();
        given(weatherForecastRepository.findLatestAndPreviousByGridPoint(gridPoint, forecastType))
                .willReturn(List.of(latestForecast, previousForecast));

        // when
        WeatherDTO result = weatherQueryService.getFcstWeather(gridPoint, now, 3);

        // then
        assertThat(result.weatherList())
                .extracting(WeatherDTO.WeatherList::time)
                .containsExactly("1700", "1800", "1900");
        assertThat(result.weatherList())
                .extracting(WeatherDTO.WeatherList::temp)
                .containsExactly("previous", "latest", "latest");
    }

    @Test
    @DisplayName("초단기예보가 요청 개수를 충족하면 초단기예보만 반환한다")
    void returnUltraShortWeatherWhenEnoughForecastsExist() {
        // given
        GridPoint gridPoint = new GridPoint(98, 76);
        Cache ultraShortCache = cacheManager.getCache(CacheType.ULTRA_SHORT_WEATHER.getCacheName());
        Cache shortTermCache = cacheManager.getCache(CacheType.SHORT_TERM_WEATHER.getCacheName());
        assertThat(ultraShortCache).isNotNull();
        assertThat(shortTermCache).isNotNull();
        ultraShortCache.put(gridPoint, weatherWithTemp(
                "20260803", "ultra", "1000", "1100", "1200"
        ));
        shortTermCache.put(gridPoint, weatherWithTemp(
                "20260803", "short", "1000", "1100", "1200"
        ));

        // when
        WeatherDTO result = weatherQueryService.getFcstWeather(
                gridPoint,
                LocalDateTime.of(2026, 8, 3, 10, 15),
                3
        );

        // then
        assertThat(result.weatherList())
                .extracting(WeatherDTO.WeatherList::temp)
                .containsExactly("ultra", "ultra", "ultra");
    }

    @Test
    @DisplayName("단기예보를 날짜 경계와 관계없이 현재 정시부터 24시간 조회한다")
    void getTwentyFourHourShortTermWeatherAcrossDateBoundary() {
        // given
        GridPoint gridPoint = new GridPoint(98, 76);
        LocalDateTime firstForecastTime = LocalDateTime.of(2026, 8, 3, 21, 0);
        List<WeatherDTO.WeatherList> forecasts = IntStream.range(0, 26)
                .mapToObj(offset -> weatherAt(firstForecastTime.plusHours(offset), "short"))
                .toList();
        Cache shortTermCache = cacheManager.getCache(CacheType.SHORT_TERM_WEATHER.getCacheName());
        assertThat(shortTermCache).isNotNull();
        shortTermCache.put(gridPoint, new WeatherDTO(forecasts));

        // when
        WeatherDTO result = weatherQueryService.getFcstWeather(
                gridPoint,
                LocalDateTime.of(2026, 8, 3, 22, 37),
                24
        );

        // then
        assertThat(result.weatherList()).hasSize(24);
        assertThat(result.weatherList().getFirst().date()).isEqualTo("20260803");
        assertThat(result.weatherList().getFirst().time()).isEqualTo("2200");
        assertThat(result.weatherList().getLast().date()).isEqualTo("20260804");
        assertThat(result.weatherList().getLast().time()).isEqualTo("2100");
    }

    @Test
    @DisplayName("캐시와 DB 모두 조회가 실패하면 날씨 정보 부재 예외를 발생시킨다")
    void rejectMissingRegionalWeatherCacheAndDb() {
        // given
        GridPoint gridPoint = new GridPoint(98, 76);
        LocalDateTime now = LocalDateTime.of(2026, 8, 3, 10, 30);
        given(weatherForecastRepository.findLatestAndPreviousByGridPoint(
                gridPoint, org.zerock.puppyrun.weather.utils.WeatherForecast.ForecastType.SHORT_TERM
        )).willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> weatherQueryService.getFcstWeather(
                gridPoint,
                now,
                6
        ))
                .isInstanceOf(WeatherNotFoundException.class)
                .satisfies(exception -> assertThat(
                        ((WeatherNotFoundException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.NOT_FOUND_WEATHER));
    }

    @Test
    @DisplayName("날씨 조회 개수가 1보다 작으면 조회를 거부한다")
    void rejectInvalidWeatherLimit() {
        // given
        GridPoint gridPoint = new GridPoint(98, 76);
        LocalDateTime now = LocalDateTime.of(2026, 8, 3, 10, 30);

        // when & then
        assertThatThrownBy(() -> weatherQueryService.getFcstWeather(
                gridPoint,
                now,
                0
        ))
                .isInstanceOf(InvalidValueException.class)
                .hasMessage("날씨 조회 개수는 1 이상 24 이하여야 합니다.");
    }

    @Test
    @DisplayName("날씨 조회 개수가 24보다 크면 조회를 거부한다")
    void rejectWeatherLimitOverTwentyFourHours() {
        // given
        GridPoint gridPoint = new GridPoint(98, 76);
        LocalDateTime now = LocalDateTime.of(2026, 8, 3, 10, 30);

        // when & then
        assertThatThrownBy(() -> weatherQueryService.getFcstWeather(
                gridPoint,
                now,
                25
        ))
                .isInstanceOf(InvalidValueException.class)
                .hasMessage("날씨 조회 개수는 1 이상 24 이하여야 합니다.");
    }

    @Test
    @DisplayName("현재 시간을 30분 기준으로 반올림한 시간의 날씨를 반환한다")
    void getNearestTimeWeather() {
        // given
        WeatherDTO weather = weather("20260803", "1000", "1100");
        LocalDateTime now = LocalDateTime.of(2026, 8, 3, 10, 31);

        // when
        WeatherDTO result = weatherQueryService.getNearestTimeWeather(weather, now);

        // then
        assertThat(result.weatherList().getFirst().date()).isEqualTo("20260803");
        assertThat(result.weatherList())
                .extracting(WeatherDTO.WeatherList::time)
                .containsExactly("1100");
    }

    private WeatherDTO weather(String date, String... times) {
        return weatherWithTemp(date, "25.0", times);
    }

    private WeatherDTO weatherWithTemp(String date, String temp, String... times) {
        List<WeatherDTO.WeatherList> forecasts = Stream.of(times)
                .map(time -> new WeatherDTO.WeatherList(
                        date,
                        time,
                        temp,
                        SkyType.SUNNY,
                        PrecipitationType.NONE,
                        0.0
                ))
                .toList();
        return new WeatherDTO(forecasts);
    }

    private WeatherDTO.WeatherList weatherAt(LocalDateTime dateTime, String temp) {
        return new WeatherDTO.WeatherList(
                dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                dateTime.format(DateTimeFormatter.ofPattern("HHmm")),
                temp,
                SkyType.SUNNY,
                PrecipitationType.NONE,
                0.0
        );
    }
}
