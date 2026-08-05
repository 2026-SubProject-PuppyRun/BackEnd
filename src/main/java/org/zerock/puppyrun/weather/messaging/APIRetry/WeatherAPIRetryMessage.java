package org.zerock.puppyrun.weather.messaging.APIRetry;

import java.time.LocalDateTime;
import org.zerock.puppyrun.weather.DTO.GridPoint;
import org.zerock.puppyrun.weather.DTO.WeatherUpdateResult;
import org.zerock.puppyrun.weather.utils.WeatherForecast;
import org.zerock.puppyrun.weather.utils.WeatherForecast.ForecastType;

/**
 * 기상청 API 10분 지연 보상 재시도 전용 RabbitMQ 메시지 DTO 레코드입니다.
 *
 * @param forecastType 예보 종류 (초단기/단기)
 * @param gridPoint    수집 대상 격자 좌표
 * @param requestTime  최초 수집 요청 시각
 */
public record WeatherAPIRetryMessage(
        ForecastType forecastType,
        GridPoint gridPoint,
        LocalDateTime requestTime
) {

    public static WeatherAPIRetryMessage from(WeatherUpdateResult failedResult) {
        if (failedResult == null || failedResult.success()) {
            throw new IllegalArgumentException("API 실패 결과만 재시도 메시지로 변환할 수 있습니다.");
        }

        return new WeatherAPIRetryMessage(
                failedResult.forecast().getType(),
                failedResult.gridPoint(),
                failedResult.forecast().requestTime()
        );
    }

    public WeatherForecast toForecast() {
        if (forecastType == null) {
            throw new IllegalArgumentException("예보 종류는 필수입니다.");
        }

        return forecastType.create(requestTime);
    }
}
