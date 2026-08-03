package org.zerock.puppyrun.weather.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.zerock.puppyrun.weather.DTO.WeatherRegion;
import org.zerock.puppyrun.weather.utils.WeatherRegionCatalog;

@Configuration
public class WeatherRegionConfig {

    private String WEATHER_REGIONS_PATH;
    private static final int EXPECTED_COLUMN_COUNT = 10;

    @Bean
    public WeatherRegionCatalog weatherRegionCatalog(
            @Value("${weather.region-path}") String weatherPath
    ) {
        this.WEATHER_REGIONS_PATH = weatherPath;
        return new WeatherRegionCatalog(loadWeatherRegions());
    }

    private List<WeatherRegion> loadWeatherRegions() {
        ClassPathResource resource = new ClassPathResource(WEATHER_REGIONS_PATH);
        List<WeatherRegion> regions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
        )) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (lineNumber == 1 || line.isBlank()) {
                    continue;
                }

                regions.add(parseWeatherRegion(line, lineNumber));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("날씨 지역 CSV 파일을 읽을 수 없습니다.", exception);
        }

        if (regions.isEmpty()) {
            throw new IllegalStateException("날씨 지역 CSV 파일에 지역 정보가 없습니다.");
        }

        return regions;
    }

    private WeatherRegion parseWeatherRegion(String line, int lineNumber) {
        String[] columns = line.split(",", -1);

        if (columns.length != EXPECTED_COLUMN_COUNT) {
            throw new IllegalStateException("날씨 지역 CSV 형식이 올바르지 않습니다. line=" + lineNumber);
        }

        try {
            String firstLevel = columns[2].trim();
            String secondLevel = columns[3].trim();
            int nx = Integer.parseInt(columns[5].trim());
            int ny = Integer.parseInt(columns[6].trim());

            double longitude = Double.parseDouble(columns[7].trim());
            double latitude = Double.parseDouble(columns[8].trim());

            return new WeatherRegion(
                    List.of(firstLevel, secondLevel),
                    nx,
                    ny,
                    latitude,
                    longitude
            );
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "날씨 지역 CSV의 위경도 형식이 올바르지 않습니다. line=" + lineNumber,
                    exception
            );
        }
    }
}
