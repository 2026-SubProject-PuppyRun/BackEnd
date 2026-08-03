package org.zerock.puppyrun.weather.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zerock.puppyrun.weather.DTO.GridPoint;
import org.zerock.puppyrun.weather.DTO.RegionType;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.controller.response.WeatherForecastResponse;
import org.zerock.puppyrun.weather.controller.response.WeatherResponse;
import org.zerock.puppyrun.weather.service.WeatherService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/weather")
public class WeatherController {

    private static final ZoneId WEATHER_ZONE = ZoneId.of("Asia/Seoul");
    private static final int CURRENT_WEATHER_QUERY_LIMIT = 2;
    private static final int FORECAST_QUERY_LIMIT = 6;

    private final WeatherService weatherService;

    /**
     * 현재 시간 기준 날씨 조회
     */
    @GetMapping("/current")
    public ResponseEntity<WeatherResponse> getCurrentWeather(@RequestParam int lat,
                                                             @RequestParam int lon
    ) {
        RegionType regionType = RegionType.findNearest(lat, lon);
        GridPoint gridPoint = new GridPoint(regionType.getNx(), regionType.getNy());
        LocalDateTime now = LocalDateTime.now(WEATHER_ZONE);

        WeatherDTO weather = weatherService.getUltraShortWeather(
                gridPoint,
                now,
                CURRENT_WEATHER_QUERY_LIMIT
        );

        WeatherDTO currentWeather = weatherService.getNearestTimeWeather(weather, now);

        WeatherResponse response = WeatherResponse.of(currentWeather, regionType);

        return ResponseEntity.ok().body(response);
    }

    /**
     * 날씨 예보 조회 (전체 리스트)
     */
    @GetMapping("/forecast")
    public ResponseEntity<WeatherForecastResponse> getWeatherForecast(@RequestParam int lat, @RequestParam int lon) {
        RegionType regionType = RegionType.findNearest(lat, lon);
        GridPoint gridPoint = new GridPoint(regionType.getNx(), regionType.getNy());
        LocalDateTime now = LocalDateTime.now(WEATHER_ZONE);

        WeatherDTO weather = weatherService.getShortTermWeather(
                gridPoint,
                now,
                FORECAST_QUERY_LIMIT
        );

        WeatherForecastResponse response = WeatherForecastResponse.of(weather, regionType);

        return ResponseEntity.ok().body(response);
    }
}
