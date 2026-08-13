package org.zerock.puppyrun.weather.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.weather.DTO.WeatherSaveCommand;
import org.zerock.puppyrun.weather.enity.WeatherForecastEntity;
import org.zerock.puppyrun.weather.repository.WeatherForecastRepository;
import org.zerock.puppyrun.weather.utils.WeatherForecast.ForecastType;

/**
 * 수집된 기상청 날씨 예보 성공 데이터를 데이터베이스에 영속화(Save/Flush)하는 전담 커맨드 서비스입니다.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WeatherCommandService {

    private final WeatherForecastRepository weatherForecastRepository;

    /**
     * 날씨 예보 저장 명령을 새로운 데이터로 일괄 저장합니다.
     *
     * @param commands 날씨 예보 저장 명령 목록
     */
    public void save(List<WeatherSaveCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }

        List<WeatherForecastEntity> entities = commands.stream()
                .map(this::toEntity)
                .toList();

        weatherForecastRepository.saveAllAndFlush(entities);

        log.info(
                "전체 날씨 DB 저장 완료: type={} 요청={}, 저장={}",
                commands.getFirst().forecastType(),
                commands.size(),
                entities.size()
        );
    }

    private WeatherForecastEntity toEntity(WeatherSaveCommand command) {
        return WeatherForecastEntity.builder()
                .type(command.forecastType())
                .nx(command.gridPoint().nx())
                .ny(command.gridPoint().ny())
                .baseDateTime(command.forecastStartTime())
                .weather(command.weather())
                .build();
    }
}
