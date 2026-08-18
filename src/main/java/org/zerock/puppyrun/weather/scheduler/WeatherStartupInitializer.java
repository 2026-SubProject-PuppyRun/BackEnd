package org.zerock.puppyrun.weather.scheduler;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.zerock.puppyrun.weather.DTO.GridPoint;
import org.zerock.puppyrun.weather.service.WeatherForecastCollector;
import org.zerock.puppyrun.weather.service.WeatherQueryService;
import org.zerock.puppyrun.weather.service.WeatherUpdateResultHandler;
import org.zerock.puppyrun.weather.utils.WeatherForecast;
import org.zerock.puppyrun.weather.utils.WeatherForecast.ShortTerm;
import org.zerock.puppyrun.weather.utils.WeatherRegionCatalog;

@Component
@RequiredArgsConstructor
@Slf4j
public class WeatherStartupInitializer {
    private static final ZoneId WEATHER_ZONE = ZoneId.of("Asia/Seoul");

    private final WeatherForecastCollector weatherForecastCollector;
    private final WeatherUpdateResultHandler resultHandler;

    private final WeatherQueryService weatherQueryService;
    private final WeatherRegionCatalog weatherRegionCatalog;

    @Value("${weather.initialize-on-startup:true}")
    private boolean initializeOnStartup;

    /**
     * 애플리케이션 준비 완료 이벤트 시 1회 초기 수집 및 저장 파이프라인을 비동기로 실행합니다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeShortTermWeatherOnStartup() {
        if (!initializeOnStartup) {
            log.info("[weather 초기화] 서버 시작 시 날씨 초기 수집이 비활성화되어 있습니다.");
            return;
        }

        LocalDateTime requestTime = LocalDateTime.now(WEATHER_ZONE);
        WeatherForecast forecast = new ShortTerm(requestTime);

        // DB 조회 후 존재 격자 목록 반환
        Set<GridPoint> existGridPoints = weatherQueryService
                .findAllByBaseDateTimeAndType(
                        forecast.getBaseDateTime(),
                        forecast.getType()
                )
                .stream()
                .map(r -> new GridPoint(r.getNx(), r.getNy()))
                .collect(Collectors.toSet());

        // 전체 지역 중 DB에 없는 격자만 추출
        List<GridPoint> missingRegions = weatherRegionCatalog.getRegions().stream()
                .map(v -> new GridPoint(v.nx(), v.ny()))
                .distinct()
                .filter(gridPoint -> !existGridPoints.contains(gridPoint))
                .toList();

        log.info("[weather 초기화] 서버 가동 시작 1회 초기 수집 실행 (시각={}, 미확인 지역={})", requestTime, missingRegions.size());
        weatherForecastCollector.collect(forecast, missingRegions)
                .collectList()
                .flatMap(resultHandler::processInitial)
                .subscribe();
    }
}
