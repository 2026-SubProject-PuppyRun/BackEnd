package org.zerock.puppyrun.weather.scheduler;

import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zerock.puppyrun.weather.service.WeatherForecastCollector;
import org.zerock.puppyrun.weather.service.WeatherUpdateResultHandler;
import org.zerock.puppyrun.weather.utils.WeatherForecast.ShortTerm;
import org.zerock.puppyrun.weather.utils.WeatherForecast.UltraShort;

/**
 * 정기적인 기상청 예보 수집(초단기/단기) 및 시작 시 캐시 초기화 스케줄러입니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WeatherScheduler {

    private static final ZoneId WEATHER_ZONE = ZoneId.of("Asia/Seoul");

    private final WeatherForecastCollector weatherForecastCollector;
    private final WeatherUpdateResultHandler resultHandler;


    /**
     * 매시간 정각마다 초단기예보 수집 및 캐시 저장을 실행합니다.
     */
    @Scheduled(
            cron = "0 0 * * * *",
            zone = "Asia/Seoul"
    )
    public void scheduledUltraShortForecastUpdate() {
        LocalDateTime requestTime = LocalDateTime.now(WEATHER_ZONE);
        log.info("정기 초단기예보 수집 실행 (시각={})", requestTime);

        weatherForecastCollector.collectAll(new UltraShort(requestTime))
                .collectList()
                .flatMap(resultHandler::processInitial)
                .subscribe();
    }

    /**
     * 매일 02:30부터 3시간마다 단기예보 수집 및 캐시/DB 백업 저장을 실행합니다.
     *
     * <p>실행 시각: 02:30, 05:30, 08:30, 11:30, 14:30, 17:30, 20:30, 23:30</p>
     */
    @Scheduled(
            cron = "0 30 2,5,8,11,14,17,20,23 * * *",
            zone = "Asia/Seoul"
    )
    public void scheduledShortTermForecastUpdate() {
        LocalDateTime requestTime = LocalDateTime.now(WEATHER_ZONE);
        log.info("정기 단기예보 수집 실행 (시각={})", requestTime);

        weatherForecastCollector.collectAll(new ShortTerm(requestTime))
                .collectList()
                .flatMap(resultHandler::processInitial)
                .subscribe();
    }
}
