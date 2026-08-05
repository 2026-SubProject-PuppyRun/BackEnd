package org.zerock.puppyrun.weather.DTO;

import java.util.Objects;
import org.zerock.puppyrun.weather.utils.WeatherForecast;

public record WeatherUpdateResult(
        WeatherForecast forecast,
        GridPoint gridPoint,
        WeatherDTO weather,
        boolean success
) {
    public WeatherUpdateResult {
        Objects.requireNonNull(forecast, "예보 정보는 필수입니다.");
        Objects.requireNonNull(gridPoint, "격자 좌표는 필수입니다.");

        if (success && weather == null) {
            throw new IllegalArgumentException("성공 결과에는 날씨 정보가 필요합니다.");
        }

        if (!success && weather != null) {
            throw new IllegalArgumentException("실패 결과에는 날씨 정보를 포함할 수 없습니다.");
        }
    }

    public static WeatherUpdateResult success(
            WeatherForecast forecast,
            GridPoint gridPoint,
            WeatherDTO weather
    ) {
        return new WeatherUpdateResult(forecast, gridPoint, weather, true);
    }

    public static WeatherUpdateResult failure(
            WeatherForecast forecast,
            GridPoint gridPoint
    ) {
        return new WeatherUpdateResult(forecast, gridPoint, null, false);
    }
}
