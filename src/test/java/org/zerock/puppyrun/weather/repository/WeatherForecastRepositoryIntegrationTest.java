package org.zerock.puppyrun.weather.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zerock.puppyrun.a_config.TestContainerConfig;
import org.zerock.puppyrun.weather.DTO.GridPoint;
import org.zerock.puppyrun.weather.DTO.PrecipitationType;
import org.zerock.puppyrun.weather.DTO.SkyType;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.enity.WeatherForecastEntity;
import org.zerock.puppyrun.weather.utils.WeatherForecast.ForecastType;

class WeatherForecastRepositoryIntegrationTest extends TestContainerConfig {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private WeatherForecastRepository weatherForecastRepository;

    @Test
    @DisplayName("격자와 예보 타입으로 최신 발표본과 직전 발표본만 최신순으로 조회한다")
    void findLatestAndPreviousByGridPoint() {
        // given
        GridPoint gridPoint = new GridPoint(63, 103);
        weatherForecastRepository.saveAll(List.of(
                forecast(gridPoint, LocalDateTime.of(2026, 8, 18, 8, 0), ForecastType.SHORT_TERM),
                forecast(gridPoint, LocalDateTime.of(2026, 8, 18, 11, 0), ForecastType.SHORT_TERM),
                forecast(gridPoint, LocalDateTime.of(2026, 8, 18, 14, 0), ForecastType.SHORT_TERM),
                forecast(new GridPoint(64, 103), LocalDateTime.of(2026, 8, 18, 17, 0), ForecastType.SHORT_TERM),
                forecast(gridPoint, LocalDateTime.of(2026, 8, 18, 17, 0), ForecastType.ULTRA_SHORT)
        ));
        entityManager.flush();
        entityManager.clear();

        // when
        List<WeatherForecastEntity> result = weatherForecastRepository
                .findLatestAndPreviousByGridPoint(gridPoint, ForecastType.SHORT_TERM);

        // then
        assertThat(result)
                .extracting(WeatherForecastEntity::getBaseDateTime)
                .containsExactly(
                        LocalDateTime.of(2026, 8, 18, 14, 0),
                        LocalDateTime.of(2026, 8, 18, 11, 0)
                );
    }

    private WeatherForecastEntity forecast(
            GridPoint gridPoint,
            LocalDateTime baseDateTime,
            ForecastType forecastType
    ) {
        return WeatherForecastEntity.builder()
                .baseDateTime(baseDateTime)
                .nx(gridPoint.nx())
                .ny(gridPoint.ny())
                .type(forecastType)
                .weather(new WeatherDTO(List.of(
                        new WeatherDTO.WeatherList(
                                "20260818",
                                "1800",
                                "25",
                                SkyType.SUNNY,
                                PrecipitationType.NONE,
                                0.0
                        )
                )))
                .build();
    }
}
