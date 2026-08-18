package org.zerock.puppyrun.weather.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.weather.DTO.GridPoint;
import org.zerock.puppyrun.weather.enity.WeatherForecastEntity;
import org.zerock.puppyrun.weather.utils.WeatherForecast.ForecastType;

import static org.zerock.puppyrun.weather.enity.QWeatherForecastEntity.weatherForecastEntity;

/**
 * 날씨 예보 후보 Querydsl 구현체입니다.
 */
@Repository
@RequiredArgsConstructor
public class WeatherForecastRepoCustomImpl implements WeatherForecastRepoCustom {

    private static final long LATEST_AND_PREVIOUS_FORECAST_COUNT = 2L;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<WeatherForecastEntity> findLatestAndPreviousByGridPoint(
            GridPoint gridPoint,
            ForecastType forecastType
    ) {
        return queryFactory
                .selectFrom(weatherForecastEntity)
                .where(
                        weatherForecastEntity.nx.eq(gridPoint.nx()),
                        weatherForecastEntity.ny.eq(gridPoint.ny()),
                        weatherForecastEntity.type.eq(forecastType)
                )
                .orderBy(weatherForecastEntity.baseDateTime.desc())
                .limit(LATEST_AND_PREVIOUS_FORECAST_COUNT)
                .fetch();
    }
}
