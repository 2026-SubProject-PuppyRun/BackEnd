package org.zerock.puppyrun.weather.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.zerock.puppyrun.common.config.CacheConfig;
import org.zerock.puppyrun.weather.DTO.GridPoint;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.DTO.WeatherUpdateResult;
import org.zerock.puppyrun.weather.utils.WeatherForecast;

class WeatherCacheProcessTest {

    private static final LocalDateTime REQUEST_TIME = LocalDateTime.of(2026, 8, 5, 8, 30);
    private static final GridPoint GRID_POINT = new GridPoint(60, 127);

    private final CacheManager cacheManager = new CacheConfig().cacheManager();
    private final WeatherCacheProcess cacheProcess = new WeatherCacheProcess(cacheManager);

    @BeforeEach
    void setUp() {
        cacheProcess.clearFailedResults();
    }

    @Test
    @DisplayName("같은 격자의 초단기와 단기 API 실패를 별도 캐시 항목으로 보관한다")
    void putFailedKeepsForecastTypesSeparately() {
        // given
        WeatherUpdateResult ultraShortFailure = WeatherUpdateResult.failure(
                new WeatherForecast.UltraShort(REQUEST_TIME),
                GRID_POINT
        );
        WeatherUpdateResult shortTermFailure = WeatherUpdateResult.failure(
                new WeatherForecast.ShortTerm(REQUEST_TIME),
                GRID_POINT
        );

        // when
        cacheProcess.putFailed(ultraShortFailure);
        cacheProcess.putFailed(shortTermFailure);

        // then
        assertThat(cacheProcess.getFailedResults())
                .containsExactlyInAnyOrder(ultraShortFailure, shortTermFailure);
    }

    @Test
    @DisplayName("재시도 성공 시 같은 예보 종류와 격자의 실패 기록만 삭제한다")
    void removeFailedEvictsOnlyMatchingForecastType() {
        // given
        WeatherForecast ultraShort = new WeatherForecast.UltraShort(REQUEST_TIME);
        WeatherUpdateResult ultraShortFailure = WeatherUpdateResult.failure(ultraShort, GRID_POINT);
        WeatherUpdateResult shortTermFailure = WeatherUpdateResult.failure(
                new WeatherForecast.ShortTerm(REQUEST_TIME),
                GRID_POINT
        );
        cacheProcess.putFailed(ultraShortFailure);
        cacheProcess.putFailed(shortTermFailure);
        WeatherUpdateResult ultraShortSuccess = WeatherUpdateResult.success(
                ultraShort,
                GRID_POINT,
                new WeatherDTO(List.of())
        );

        // when
        cacheProcess.removeFailed(ultraShortSuccess);

        // then
        assertThat(cacheProcess.getFailedResults())
                .containsExactly(shortTermFailure);
    }
}
