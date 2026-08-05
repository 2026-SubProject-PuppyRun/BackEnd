package org.zerock.puppyrun.weather.service;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.zerock.puppyrun.common.config.CacheType;
import org.zerock.puppyrun.weather.DTO.GridPoint;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.DTO.WeatherSaveCommand;
import org.zerock.puppyrun.weather.DTO.WeatherUpdateResult;
import org.zerock.puppyrun.weather.messaging.APIRetry.WeatherAPIRetryPublisher;
import org.zerock.puppyrun.weather.messaging.DBRetry.WeatherDBRetryPublisher;
import org.zerock.puppyrun.weather.utils.WeatherForecast;

class WeatherUpdateResultHandlerTest {

    private static final LocalDateTime REQUEST_TIME = LocalDateTime.of(2026, 8, 5, 8, 30);
    private static final GridPoint GRID_POINT = new GridPoint(60, 127);
    private static final WeatherDTO WEATHER = new WeatherDTO(List.of());

    private final WeatherCommandService weatherCommandService = mock(WeatherCommandService.class);
    private final WeatherCacheProcess weatherCacheProcess = mock(WeatherCacheProcess.class);
    private final WeatherAPIRetryPublisher apiRetryPublisher = mock(WeatherAPIRetryPublisher.class);
    private final WeatherDBRetryPublisher dbRetryPublisher = mock(WeatherDBRetryPublisher.class);

    private final WeatherUpdateResultHandler resultHandler = new WeatherUpdateResultHandler(
            weatherCommandService,
            weatherCacheProcess,
            apiRetryPublisher,
            dbRetryPublisher
    );

    @Test
    @DisplayName("최초 단기예보 성공 결과는 캐시와 DB에 저장한다")
    void processInitialSavesShortTermResultToCacheAndDb() {
        // given
        WeatherForecast forecast = new WeatherForecast.ShortTerm(REQUEST_TIME);
        WeatherUpdateResult result = WeatherUpdateResult.success(forecast, GRID_POINT, WEATHER);
        WeatherSaveCommand command = WeatherSaveCommand.from(result);

        // when
        resultHandler.processInitial(List.of(result)).block();

        // then
        verify(weatherCacheProcess).putWeather(CacheType.SHORT_TERM_WEATHER, GRID_POINT, WEATHER);
        verify(weatherCacheProcess).removeFailed(result);
        verify(weatherCommandService).save(List.of(command));
        verifyNoInteractions(apiRetryPublisher, dbRetryPublisher);
    }

    @Test
    @DisplayName("최초 초단기예보 성공 결과는 DB 저장 없이 캐시에만 저장한다")
    void processInitialSavesUltraShortResultToCacheOnly() {
        // given
        WeatherForecast forecast = new WeatherForecast.UltraShort(REQUEST_TIME);
        WeatherUpdateResult result = WeatherUpdateResult.success(forecast, GRID_POINT, WEATHER);

        // when
        resultHandler.processInitial(List.of(result)).block();

        // then
        verify(weatherCacheProcess).putWeather(CacheType.ULTRA_SHORT_WEATHER, GRID_POINT, WEATHER);
        verify(weatherCacheProcess).removeFailed(result);
        verifyNoInteractions(weatherCommandService, apiRetryPublisher, dbRetryPublisher);
    }

    @Test
    @DisplayName("최초 API 실패 결과는 실패 캐시에 저장하고 10분 재시도 큐에 발행한다")
    void processInitialCachesAndPublishesApiFailure() {
        // given
        WeatherForecast forecast = new WeatherForecast.ShortTerm(REQUEST_TIME);
        WeatherUpdateResult result = WeatherUpdateResult.failure(forecast, GRID_POINT);

        // when
        resultHandler.processInitial(List.of(result)).block();

        // then
        verify(weatherCacheProcess).putFailed(result);
        verify(apiRetryPublisher).publish(List.of(result));
        verifyNoInteractions(weatherCommandService, dbRetryPublisher);
    }

    @Test
    @DisplayName("단기예보 DB 저장 실패 시 10초 재시도 큐에 저장 명령을 발행한다")
    void processInitialPublishesDbRetryWhenDbSaveFails() {
        // given
        WeatherForecast forecast = new WeatherForecast.ShortTerm(REQUEST_TIME);
        WeatherUpdateResult result = WeatherUpdateResult.success(forecast, GRID_POINT, WEATHER);
        List<WeatherSaveCommand> commands = List.of(WeatherSaveCommand.from(result));
        doThrow(new RuntimeException("DB unavailable"))
                .when(weatherCommandService)
                .save(commands);

        // when
        resultHandler.processInitial(List.of(result)).block();

        // then
        verify(dbRetryPublisher).publish(commands);
    }

    @Test
    @DisplayName("API 재시도에 성공한 단기예보는 실패 캐시를 지우고 캐시와 DB에 저장한다")
    void processRetrySavesShortTermResultAndClearsFailure() {
        // given
        WeatherForecast forecast = new WeatherForecast.ShortTerm(REQUEST_TIME);
        WeatherUpdateResult result = WeatherUpdateResult.success(forecast, GRID_POINT, WEATHER);

        // when
        resultHandler.processRetry(result);

        // then
        verify(weatherCacheProcess).putWeather(CacheType.SHORT_TERM_WEATHER, GRID_POINT, WEATHER);
        verify(weatherCacheProcess).removeFailed(result);
        verify(weatherCommandService).save(List.of(WeatherSaveCommand.from(result)));
        verify(apiRetryPublisher, never()).publish(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("API 재시도 실패 결과는 다시 발행하지 않는다")
    void processRetryDoesNotRepublishFailure() {
        // given
        WeatherForecast forecast = new WeatherForecast.ShortTerm(REQUEST_TIME);
        WeatherUpdateResult result = WeatherUpdateResult.failure(forecast, GRID_POINT);

        // when
        resultHandler.processRetry(result);

        // then
        verifyNoInteractions(
                weatherCommandService,
                weatherCacheProcess,
                apiRetryPublisher,
                dbRetryPublisher
        );
    }
}
