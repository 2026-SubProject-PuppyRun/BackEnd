package org.zerock.puppyrun.common.scheduler;

import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zerock.puppyrun.common.config.CacheType;
import org.zerock.puppyrun.common.exception.CacheNotFoundException;
import org.zerock.puppyrun.weather.DTO.DateTimeDTO;
import org.zerock.puppyrun.weather.DTO.RegionType;
import org.zerock.puppyrun.weather.DTO.WeatherApiPara;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.service.WeatherApiClient;
import org.zerock.puppyrun.weather.service.WeatherMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class WeatherScheduler {

    private final WeatherApiClient weatherApiClient;
    private final WeatherMapper weatherMapper;
    private final CacheManager cacheManager;

    @Scheduled(cron = "0 0 * * * *")
    public void scheduledWeatherUpdate() {
        log.info("날씨 데이터 정기 업데이트 시작");
        DateTimeDTO dateTimeDTO = weatherApiClient.createCurrentDateTimeDto();

        Arrays.stream(RegionType.values()).forEach(region -> {
            WeatherApiPara para = new WeatherApiPara(dateTimeDTO.baseDate(), dateTimeDTO.baseTime(), region.getNx(),
                    region.getNy());

            weatherApiClient.fetchWeather(para)
                    .subscribe(
                            response -> {
                                try {
                                    List<WeatherDTO> dtoList = weatherMapper.toWeatherDTOList(response);
                                    putWeatherToCache(region.name(), dtoList);
                                } catch (Exception e) {
                                    log.error("스케줄러 갱신 중 오류 발생 (Region: {}): {}", region.name(), e.getMessage());
                                }
                            },
                            error -> log.error("API 호출 실패 (Region: {}): {}", region.name(), error.getMessage())
                    );
        });
    }

    private void putWeatherToCache(String key, List<WeatherDTO> weatherDTOList) {
        Cache cache = cacheManager.getCache(CacheType.REGIONAL_WEATHER.getCacheName());
        if (cache == null) {
            log.error("캐시를 찾을 수 없습니다. : {} ", CacheType.REGIONAL_WEATHER.getCacheName());
            throw new CacheNotFoundException("캐시를 찾을 수 없습니다.");
        }
        cache.put(key, weatherDTOList);
        log.info("캐시 갱신 완료: Key={}", key);
    }
}
