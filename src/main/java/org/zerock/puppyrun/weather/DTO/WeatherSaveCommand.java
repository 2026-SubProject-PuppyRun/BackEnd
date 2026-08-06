package org.zerock.puppyrun.weather.DTO;

import java.time.LocalDateTime;
import java.util.Objects;
import org.zerock.puppyrun.weather.utils.WeatherForecast.ForecastType;

/**
 * 날씨 예보 DB 저장에 필요한 값만 전달하는 불변 커맨드입니다.
 */
public record WeatherSaveCommand(
        ForecastType forecastType,
        GridPoint gridPoint,
        LocalDateTime forecastStartTime,
        WeatherDTO weather
) {

    public WeatherSaveCommand {
        Objects.requireNonNull(gridPoint, "격자 좌표는 필수입니다.");
        Objects.requireNonNull(forecastStartTime, "예보 시작 시각은 필수입니다.");
        Objects.requireNonNull(weather, "날씨 정보는 필수입니다.");
        Objects.requireNonNull(forecastType, "예보 타입은 필수입니다.");
    }

    public static WeatherSaveCommand from(WeatherUpdateResult result) {
        if (result == null || !result.success()) {
            throw new IllegalArgumentException("성공한 날씨 수집 결과만 DB 저장 명령으로 변환할 수 있습니다.");
        }

        return new WeatherSaveCommand(
                result.forecast().getType(),
                result.gridPoint(),
                result.forecast().getBaseDateTime(),
                result.weather()
        );
    }
}
