package org.zerock.puppyrun.weather.scheduler;

import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zerock.puppyrun.common.config.CacheType;
import org.zerock.puppyrun.weather.service.WeatherForecastUpdater;
import org.zerock.puppyrun.weather.utils.WeatherForecast.ShortTerm;
import org.zerock.puppyrun.weather.utils.WeatherForecast.UltraShort;

/**
 * 예보별 실행 시점과 전략·캐시 관계만 선언합니다.
 */
@Component
@Slf4j
public class WeatherScheduler {

    private static final ZoneId WEATHER_ZONE = ZoneId.of("Asia/Seoul");

    private final WeatherForecastUpdater weatherForecastUpdater;
    private final boolean initializeOnStartup;

    public WeatherScheduler(
            WeatherForecastUpdater weatherForecastUpdater,
            @Value("${weather.initialize-on-startup:true}") boolean initializeOnStartup
    ) {
        this.weatherForecastUpdater = weatherForecastUpdater;
        this.initializeOnStartup = initializeOnStartup;
    }

    /**
     * 애플리케이션 준비가 완료되면 단기예보 캐시를 한 번 초기화합니다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeWeatherForecasts() {
        if (!initializeOnStartup) {
            log.info("서버 시작 시 날씨 캐시 초기화가 비활성화되어 있습니다.");
            return;
        }

        LocalDateTime requestTime = LocalDateTime.now(WEATHER_ZONE);
        log.info("서버 시작 시 날씨 캐시 초기화를 시작합니다: time={}", requestTime);

        weatherForecastUpdater.update(
                new ShortTerm(),
                CacheType.SHORT_TERM_WEATHER,
                requestTime
        );
    }

    @Scheduled(
            cron = "0 0 * * * *",
            zone = "Asia/Seoul"
    )
    public void scheduledUltraShortForecastUpdate() {
        weatherForecastUpdater.update(
                new UltraShort(),
                CacheType.ULTRA_SHORT_WEATHER,
                LocalDateTime.now(WEATHER_ZONE)
        );
    }

    @Scheduled(
            cron = "0 30 2,5,8,11,14,17,20,23 * * *",
            zone = "Asia/Seoul"
    )
    public void scheduledShortTermForecastUpdate() {
        weatherForecastUpdater.update(
                new ShortTerm(),
                CacheType.SHORT_TERM_WEATHER,
                LocalDateTime.now(WEATHER_ZONE)
        );
    }

}
