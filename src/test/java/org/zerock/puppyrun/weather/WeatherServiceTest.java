package org.zerock.puppyrun.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;
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
import org.zerock.puppyrun.weather.exception.WeatherNotFoundException;
import org.zerock.puppyrun.weather.service.WeatherService;

class WeatherServiceTest {

    private final CacheManager cacheManager = new ConcurrentMapCacheManager(
            CacheType.ULTRA_SHORT_WEATHER.getCacheName(),
            CacheType.SHORT_TERM_WEATHER.getCacheName()
    );
    private final WeatherService weatherService = new WeatherService(cacheManager);

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
    @DisplayName("초단기예보 캐시가 없으면 단기예보를 현재 정시부터 시간순으로 조회한다")
    void fallBackToShortTermWeatherWhenUltraShortCacheIsMissing() {
        // given
        GridPoint schedulerKey = new GridPoint(98, 76);
        Cache cache = cacheManager.getCache(CacheType.SHORT_TERM_WEATHER.getCacheName());
        assertThat(cache).isNotNull();
        cache.put(schedulerKey, weather(
                "20260803",
                "1200", "0900", "1100", "1000", "1300"
        ));

        // when
        WeatherDTO result = weatherService.getFcstWeather(
                new GridPoint(98, 76),
                LocalDateTime.of(2026, 8, 3, 10, 37, 25),
                3
        );

        // then
        assertThat(result.weatherList())
                .extracting(WeatherDTO.WeatherList::time)
                .containsExactly("1000", "1100", "1200");
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
        WeatherDTO result = weatherService.getFcstWeather(
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
    @DisplayName("초단기예보가 요청 개수보다 부족하면 단기예보 전체로 전환한다")
    void fallBackToShortTermWeatherWhenUltraShortForecastsAreInsufficient() {
        // given
        GridPoint gridPoint = new GridPoint(98, 76);
        Cache ultraShortCache = cacheManager.getCache(CacheType.ULTRA_SHORT_WEATHER.getCacheName());
        Cache shortTermCache = cacheManager.getCache(CacheType.SHORT_TERM_WEATHER.getCacheName());
        assertThat(ultraShortCache).isNotNull();
        assertThat(shortTermCache).isNotNull();
        ultraShortCache.put(gridPoint, weatherWithTemp(
                "20260803", "ultra", "1000", "1100"
        ));
        shortTermCache.put(gridPoint, weatherWithTemp(
                "20260803", "short", "1000", "1100", "1200"
        ));

        // when
        WeatherDTO result = weatherService.getFcstWeather(
                gridPoint,
                LocalDateTime.of(2026, 8, 3, 10, 15),
                3
        );

        // then
        assertThat(result.weatherList())
                .extracting(WeatherDTO.WeatherList::temp)
                .containsExactly("short", "short", "short");
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
        WeatherDTO result = weatherService.getFcstWeather(
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
    @DisplayName("캐시의 예보 날짜가 지난 경우 현재 이후 날씨가 없다고 판단한다")
    void rejectExpiredRegionalWeather() {
        // given
        GridPoint gridPoint = new GridPoint(60, 127);
        Cache cache = cacheManager.getCache(CacheType.SHORT_TERM_WEATHER.getCacheName());
        assertThat(cache).isNotNull();
        cache.put(gridPoint, weather("20260803", "2200", "2300"));

        // when & then
        assertThatThrownBy(() -> weatherService.getFcstWeather(
                gridPoint,
                LocalDateTime.of(2026, 8, 4, 0, 15),
                3
        ))
                .isInstanceOf(WeatherNotFoundException.class)
                .hasMessage("현재 시간 이후의 날씨 정보가 존재하지 않습니다.")
                .satisfies(exception -> assertThat(
                        ((WeatherNotFoundException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.NOT_FOUND_WEATHER));
    }

    @Test
    @DisplayName("초단기와 단기 캐시에 모두 날씨가 없으면 조회를 거부한다")
    void rejectMissingRegionalWeatherCache() {
        // given
        GridPoint gridPoint = new GridPoint(98, 76);
        LocalDateTime now = LocalDateTime.of(2026, 8, 3, 10, 30);

        // when & then
        assertThatThrownBy(() -> weatherService.getFcstWeather(
                gridPoint,
                now,
                6
        ))
                .isInstanceOf(WeatherNotFoundException.class)
                .hasMessage("해당 지역의 날씨 정보가 존재하지 않습니다.")
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
        assertThatThrownBy(() -> weatherService.getFcstWeather(
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
        assertThatThrownBy(() -> weatherService.getFcstWeather(
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
        WeatherDTO result = weatherService.getNearestTimeWeather(weather, now);

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
        List<WeatherDTO.WeatherList> forecasts = List.of(times).stream()
                .map(time -> new WeatherDTO.WeatherList(
                        date,
                        time,
                        temp,
                        SkyType.SUNNY,
                        PrecipitationType.NONE,
                        "강수없음"
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
                "강수없음"
        );
    }
}
