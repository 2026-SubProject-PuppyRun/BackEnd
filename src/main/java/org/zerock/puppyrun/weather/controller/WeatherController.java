package org.zerock.puppyrun.weather.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zerock.puppyrun.weather.DTO.RegionType;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.controller.response.WeatherForecastResponse;
import org.zerock.puppyrun.weather.controller.response.WeatherResponse;
import org.zerock.puppyrun.weather.service.WeatherService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/weather")
public class WeatherController {
    private final WeatherService weatherService;

    /**
     * 현재 시간 기준 날씨 조회
     */
    @GetMapping("/current")
    public ResponseEntity<WeatherResponse> getCurrentWeather(@RequestParam int lat,
                                                             @RequestParam int lon
    ) {
        RegionType regionType = RegionType.findNearest(lat, lon);

        List<WeatherDTO> weatherDTOList = weatherService.getRegionalWeather(regionType);

        WeatherDTO weatherDTO = weatherService.getNearestTimeWeather(weatherDTOList);

        WeatherResponse response = WeatherResponse.of(weatherDTO, regionType);

        return ResponseEntity.ok().body(response);
    }

    /**
     * 날씨 예보 조회 (전체 리스트)
     */
    @GetMapping("/forecast")
    public ResponseEntity<WeatherForecastResponse> getWeatherForecast(@RequestParam int lat, @RequestParam int lon) {
        RegionType regionType = RegionType.findNearest(lat, lon);

        List<WeatherDTO> weatherDTOList = weatherService.getRegionalWeather(regionType);

        WeatherForecastResponse response = WeatherForecastResponse.of(weatherDTOList, regionType);

        return ResponseEntity.ok().body(response);
    }
}
