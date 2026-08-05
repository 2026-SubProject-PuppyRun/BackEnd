package org.zerock.puppyrun.weather.messaging.APIRetry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.zerock.puppyrun.weather.DTO.GridPoint;
import org.zerock.puppyrun.weather.DTO.WeatherUpdateResult;
import org.zerock.puppyrun.weather.service.WeatherForecastCollector;
import org.zerock.puppyrun.weather.service.WeatherUpdateResultHandler;
import org.zerock.puppyrun.weather.utils.WeatherForecast;

/**
 * 10분 지연 큐({@code weather.retry.api.queue})로부터 API 호출 실패 메시지를 수신하여 1:1 개별 API 호출 재시도 및 보상 처리를 수행하는 리스너 컨슈머입니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WeatherAPIRetryConsumer {

    private final WeatherForecastCollector weatherForecastCollector;
    private final WeatherUpdateResultHandler resultHandler;

    /**
     * RabbitMQ 큐로부터 10분 지연 보상 메시지를 수신하여 개별 격자 단위 API 재시도를 수행합니다.
     *
     * @param message API 재시도 메시지
     */
    @RabbitListener(queues = "weather.retry.api.queue")
    public void process(WeatherAPIRetryMessage message) {
        if (message == null
                || message.gridPoint() == null
                || message.forecastType() == null
                || message.requestTime() == null) {
            log.error("[API 재시도 메시지 오류] 필수 값이 누락되어 재시도를 종료합니다.");
            return;
        }

        log.info("[API 재시도 큐 수신] 10분 지연 후 소비 시작 | nx={}, ny={}, 예보종류={}",
                message.gridPoint().nx(), message.gridPoint().ny(), message.forecastType());

        WeatherForecast forecast = message.toForecast();
        GridPoint gridPoint = message.gridPoint();

        WeatherUpdateResult retryResult = weatherForecastCollector.collectOne(
                forecast,
                gridPoint
        ).block();

        resultHandler.processRetry(retryResult);
    }
}
