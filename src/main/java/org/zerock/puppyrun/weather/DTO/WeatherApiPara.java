package org.zerock.puppyrun.weather.DTO;

public record WeatherApiPara(
        String baseDate,
        String baseTime,
        int nx,
        int ny
) {
}
