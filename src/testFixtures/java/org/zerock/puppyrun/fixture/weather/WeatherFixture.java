package org.zerock.puppyrun.fixture.weather;

import java.time.LocalDateTime;
import java.util.List;
import org.zerock.puppyrun.weather.DTO.GridPoint;
import org.zerock.puppyrun.weather.DTO.PrecipitationType;
import org.zerock.puppyrun.weather.DTO.SkyType;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.DTO.WeatherDTO.WeatherList;
import org.zerock.puppyrun.weather.DTO.WeatherSaveCommand;
import org.zerock.puppyrun.weather.DTO.WeatherUpdateResult;
import org.zerock.puppyrun.weather.utils.WeatherForecast;

/**
 * 날씨 예보 테스트용 데이터 객체 생성을 담당하는 Fixture 클래스입니다.
 */
public class WeatherFixture {

    public static final GridPoint DEFAULT_GRID_POINT = new GridPoint(60, 127);
    public static final LocalDateTime DEFAULT_REQUEST_TIME = LocalDateTime.of(2026, 8, 6, 12, 0);

    public static WeatherForecast createShortTermForecast() {
        return new WeatherForecast.ShortTerm(DEFAULT_REQUEST_TIME);
    }

    public static WeatherForecast createShortTermForecast(LocalDateTime requestTime) {
        return new WeatherForecast.ShortTerm(requestTime);
    }

    public static WeatherForecast createUltraShortForecast() {
        return new WeatherForecast.UltraShort(DEFAULT_REQUEST_TIME);
    }

    public static WeatherDTO createWeatherDTO() {
        return new WeatherDTO(List.of(
                new WeatherList(
                        "20260806",
                        "1300",
                        "25",
                        SkyType.SUNNY,
                        PrecipitationType.NONE,
                        "강수없음"
                )
        ));
    }

    public static WeatherUpdateResult createSuccessResult(WeatherForecast forecast, GridPoint gridPoint) {
        return WeatherUpdateResult.success(forecast, gridPoint, createWeatherDTO());
    }

    public static WeatherUpdateResult createSuccessResult() {
        return createSuccessResult(createShortTermForecast(), DEFAULT_GRID_POINT);
    }

    public static WeatherUpdateResult createFailureResult(WeatherForecast forecast, GridPoint gridPoint) {
        return WeatherUpdateResult.failure(forecast, gridPoint);
    }

    public static WeatherUpdateResult createFailureResult() {
        return createFailureResult(createShortTermForecast(), DEFAULT_GRID_POINT);
    }

    public static WeatherSaveCommand createSaveCommand(WeatherForecast forecast, GridPoint gridPoint) {
        return WeatherSaveCommand.from(createSuccessResult(forecast, gridPoint));
    }
}
