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
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.DTO.WeatherRegion;
import org.zerock.puppyrun.weather.controller.response.WeatherForecastResponse;
import org.zerock.puppyrun.weather.controller.response.WeatherResponse;
import org.zerock.puppyrun.weather.service.WeatherService;
import org.zerock.puppyrun.weather.utils.WeatherRegionCatalog;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/weather")
public class WeatherController {

    private static final ZoneId WEATHER_ZONE = ZoneId.of("Asia/Seoul");
    private static final int CURRENT_WEATHER_QUERY_LIMIT = 3;
    private static final String DEFAULT_FORECAST_QUERY_LIMIT = "24";

    private final WeatherRegionCatalog weatherRegionCatalog;
    private final WeatherService weatherService;

    /**
     * 현재 시간 기준 날씨 조회
     */
    @GetMapping("/current")
    public ResponseEntity<WeatherResponse> getCurrentWeather(
            @RequestParam double lat,
            @RequestParam double lon
    ) {
        LocalDateTime now = LocalDateTime.now(WEATHER_ZONE);
        WeatherRegion weatherRegion = weatherRegionCatalog.findNearestRegion(lat, lon);
        GridPoint gridPoint = new GridPoint(weatherRegion.nx(), weatherRegion.ny());

        WeatherDTO weather = weatherService.getFcstWeather(
                gridPoint,
                now,
                CURRENT_WEATHER_QUERY_LIMIT
        );

        WeatherDTO currentWeather = weatherService.getNearestTimeWeather(weather, now);

        WeatherResponse response = WeatherResponse.of(currentWeather, weatherRegion);

        return ResponseEntity.ok().body(response);
    }

    /**
     * 날씨 예보 조회 (전체 리스트)
     */
    @GetMapping("/forecast")
    public ResponseEntity<WeatherForecastResponse> getWeatherForecast(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = DEFAULT_FORECAST_QUERY_LIMIT) int limit
    ) {
        LocalDateTime now = LocalDateTime.now(WEATHER_ZONE);
        WeatherRegion weatherRegion = weatherRegionCatalog.findNearestRegion(lat, lon);
        GridPoint gridPoint = new GridPoint(weatherRegion.nx(), weatherRegion.ny());

        WeatherDTO weather = weatherService.getFcstWeather(
                gridPoint,
                now,
                limit
        );

        WeatherForecastResponse response = WeatherForecastResponse.of(weather, weatherRegion);

        return ResponseEntity.ok().body(response);
    }
}
