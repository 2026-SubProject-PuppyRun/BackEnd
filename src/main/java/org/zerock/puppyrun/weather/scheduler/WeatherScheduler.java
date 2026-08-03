package org.zerock.puppyrun.weather.scheduler;

import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class WeatherScheduler {

    private static final ZoneId WEATHER_ZONE = ZoneId.of("Asia/Seoul");

    private final WeatherForecastUpdater weatherForecastUpdater;

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void scheduledUltraShortForecastUpdate() {
        weatherForecastUpdater.update(
                new UltraShort(),
                CacheType.ULTRA_SHORT_WEATHER,
                LocalDateTime.now(WEATHER_ZONE)
        );
    }

    @Scheduled(cron = "0 30 2,5,8,11,14,17,20,23 * * *", zone = "Asia/Seoul")
    public void scheduledShortTermForecastUpdate() {
        weatherForecastUpdater.update(
                new ShortTerm(),
                CacheType.SHORT_TERM_WEATHER,
                LocalDateTime.now(WEATHER_ZONE)
        );
    }
}
