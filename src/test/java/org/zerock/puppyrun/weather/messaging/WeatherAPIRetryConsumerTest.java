package org.zerock.puppyrun.weather.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.zerock.puppyrun.weather.DTO.GridPoint;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.DTO.WeatherUpdateResult;
import org.zerock.puppyrun.weather.messaging.APIRetry.WeatherAPIRetryConsumer;
import org.zerock.puppyrun.weather.messaging.APIRetry.WeatherAPIRetryMessage;
import org.zerock.puppyrun.weather.service.WeatherForecastCollector;
import org.zerock.puppyrun.weather.service.WeatherUpdateResultHandler;
import org.zerock.puppyrun.weather.utils.WeatherForecast;
import org.zerock.puppyrun.weather.utils.WeatherForecast.ForecastType;
import reactor.core.publisher.Mono;

class WeatherAPIRetryConsumerTest {

    private final WeatherForecastCollector weatherForecastCollector = mock(WeatherForecastCollector.class);
    private final WeatherUpdateResultHandler resultHandler = mock(WeatherUpdateResultHandler.class);

    private final WeatherAPIRetryConsumer consumer = new WeatherAPIRetryConsumer(
            weatherForecastCollector,
            resultHandler
    );

    @Test
    @DisplayName("RabbitMQ 메시지의 예보 종류와 요청 시각으로 실패한 격자만 재수집하고 핸들러에 전달한다")
    void processRetriesSingleGridPointAndDelegatesResult() {
        // given
        GridPoint gridPoint = new GridPoint(60, 127);
        LocalDateTime requestTime = LocalDateTime.of(2026, 8, 5, 8, 30);
        WeatherForecast forecast = new WeatherForecast.ShortTerm(requestTime);
        WeatherAPIRetryMessage message = new WeatherAPIRetryMessage(
                forecast,
                gridPoint,
                requestTime
        );
        WeatherUpdateResult result = WeatherUpdateResult.success(
                forecast,
                gridPoint,
                new WeatherDTO(List.of())
        );

        when(weatherForecastCollector.collectOne(forecast, gridPoint))
                .thenReturn(Mono.just(result));

        // when
        consumer.process(message);

        // then
        verify(weatherForecastCollector).collectOne(forecast, gridPoint);
        verify(resultHandler).processRetry(result);
    }
}
