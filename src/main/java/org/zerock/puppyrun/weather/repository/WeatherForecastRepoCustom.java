package org.zerock.puppyrun.weather.repository;

import java.util.List;
import org.zerock.puppyrun.weather.DTO.GridPoint;
import org.zerock.puppyrun.weather.enity.WeatherForecastEntity;
import org.zerock.puppyrun.weather.utils.WeatherForecast.ForecastType;

/**
 * 발표 시각을 기준으로 날씨 예보 후보를 조회합니다.
 */
public interface WeatherForecastRepoCustom {

    /**
     * 지정한 격자의 최신 단기예보와 바로 이전 단기예보를 최신 발표 시각 순으로 조회합니다.
     */
    List<WeatherForecastEntity> findLatestAndPreviousByGridPoint(
            GridPoint gridPoint,
            ForecastType forecastType
    );
}
