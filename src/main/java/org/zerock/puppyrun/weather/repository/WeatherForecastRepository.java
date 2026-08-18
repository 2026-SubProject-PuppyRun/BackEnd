package org.zerock.puppyrun.weather.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.weather.enity.WeatherForecastEntity;
import org.zerock.puppyrun.weather.utils.WeatherForecast.ForecastType;

/**
 * {@link WeatherForecastEntity} 엔티티의 영속성 조작을 위한 Spring Data JPA 레포지토리 인터페이스입니다.
 */
@Repository
public interface WeatherForecastRepository extends JpaRepository<WeatherForecastEntity, UUID>, WeatherForecastRepoCustom {

    /**
     * 특정 예보 발표 시각과 예보 타입에 해당하는 모든 백업 날씨 예보 엔티티를 조회합니다.
     *
     * @param baseDateTime 예보 발표 시각
     * @param type         예보 타입
     * @return 해당 조건의 날씨 예보 엔티티 리스트
     */
    List<WeatherForecastEntity> findAllByBaseDateTimeAndType(
            LocalDateTime baseDateTime,
            ForecastType type
    );

}
