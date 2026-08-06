package org.zerock.puppyrun.weather.enity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.zerock.puppyrun.common.entity.BaseEntity;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.utils.WeatherForecast.ForecastType;

/**
 * 수집된 기상청 날씨 예보 데이터를 저장하는 DB 백업용 JPA 엔티티입니다.
 *
 * <p>동일한 격자 좌표({@code nx}, {@code ny})와 예보 시작/발표 시각({@code forecastStartTime})의 중복 저장을 방지하기 위한
 * 유니크 제약조건({@code uk_weather_forecast})과 최신 데이터 조회성능 최적화를 위한 복합 인덱스({@code idx_nx_ny_forecast_start_time})가 설정되어
 * 있습니다.</p>
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "weather_forecast",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_weather_forecast",
                columnNames = {
                        "nx",
                        "ny",
                        "base_date_time",
                        "type"
                }
        ),
        indexes = @Index(
                name = "idx_nx_ny_forecast_start_time",
                columnList = "nx, ny, type, base_date_time DESC"
        )
)
public class WeatherForecastEntity extends BaseEntity {

    @Id
    private UUID id;

    /**
     * 기상청 API 요청에 사용한 발표 기준 시각(baseDate + baseTime)
     */
    @Column(name = "base_date_time", nullable = false)
    private LocalDateTime baseDateTime;

    @Column(nullable = false)
    private int nx;

    @Column(nullable = false)
    private int ny;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ForecastType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", nullable = false)
    private WeatherDTO weather;

    @Builder
    private WeatherForecastEntity(
            LocalDateTime baseDateTime,
            int nx,
            int ny,
            WeatherDTO weather,
            ForecastType type
    ) {
        this.id = UUID.randomUUID();
        this.baseDateTime = Objects.requireNonNull(baseDateTime, "예보 발표 기준 시각은 필수입니다.");
        this.nx = nx;
        this.ny = ny;
        this.type = Objects.requireNonNull(type, "예보 타입은 필수입니다.");
        this.weather = Objects.requireNonNull(weather, "날씨 정보는 필수입니다.");
    }

    /**
     * 같은 격자, 예보 타입 및 발표 기준 시각의 데이터가 다시 수집되면 날씨 정보를 최신 값으로 갱신합니다.
     */
    public void updateWeather(WeatherDTO weather) {
        this.weather = Objects.requireNonNull(
                weather,
                "날씨 정보는 필수입니다."
        );
    }
}
