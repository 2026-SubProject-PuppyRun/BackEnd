package org.zerock.puppyrun.weather.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zerock.puppyrun.common.exception.InvalidValueException;
import org.zerock.puppyrun.weather.DTO.GridPoint;
import org.zerock.puppyrun.weather.DTO.WeatherApiPara;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.exception.WeatherNotFoundException;
import org.zerock.puppyrun.weather.utils.WeatherForecast;
import org.zerock.puppyrun.weather.utils.WeatherForecast.ShortTerm;
import org.zerock.puppyrun.weather.utils.WeatherForecast.UltraShort;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private static final DateTimeFormatter FORECAST_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final WeatherApiClient weatherApiClient;
    private final WeatherMapper weatherMapper;

    /**
     * 조회된 예보 리스트 중 현재 시간(30분 단위 반올림)에 가장 적합한 데이터를 필터링
     */
    public WeatherDTO getNearestTimeWeather(WeatherDTO weather, LocalDateTime now) {
        LocalDateTime nearestHour = now.plusMinutes(30).truncatedTo(ChronoUnit.HOURS);
        String targetDate = nearestHour.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String targetTime = nearestHour.format(DateTimeFormatter.ofPattern("HHmm"));

        if (!weather.date().equals(targetDate)) {
            throw new WeatherNotFoundException("해당 시간대의 날씨 정보를 찾을 수 없습니다.");
        }

        WeatherDTO.Detail detail = weather.detail().stream()
                .filter(weatherDetail -> weatherDetail.time().equals(targetTime))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("타겟 시간 : {}, 날씨 정보를 찾을 수 없습니다.", targetTime);
                    return new WeatherNotFoundException("해당 시간대의 날씨 정보를 찾을 수 없습니다.");
                });

        return new WeatherDTO(targetDate, List.of(detail));
    }

    /**
     * 초단기예보 캐시에서 현재 정시 이후의 날씨를 조회합니다.
     */
    public WeatherDTO getUltraShortWeather(
            GridPoint gridPoint,
            LocalDateTime now,
            int limit
    ) {
        return getRegionalWeather(new UltraShort(), gridPoint, now, limit);
    }

    /**
     * 단기예보 캐시에서 현재 정시 이후의 날씨를 조회합니다.
     */
    public WeatherDTO getShortTermWeather(
            GridPoint gridPoint,
            LocalDateTime now,
            int limit
    ) {
        return getRegionalWeather(new ShortTerm(), gridPoint, now, limit);
    }

    private WeatherDTO getRegionalWeather(
            WeatherForecast forecast,
            GridPoint gridPoint,
            LocalDateTime now,
            int limit
    ) {
        log.info(
                "지역 날씨 예보 API 호출 진행: strategy={}, nx={}, ny={}, time={}",
                forecast.getClass().getSimpleName(),
                gridPoint.nx(),
                gridPoint.ny(),
                now
        );

        if (limit < 1) {
            throw new InvalidValueException("날씨 조회 개수는 1 이상이어야 합니다.");
        }

        WeatherApiPara para = forecast.getPara(now, gridPoint);
        WeatherDTO weather = weatherApiClient.fetchWeather(para)
                .map(response -> weatherMapper.toWeatherDTO(
                        response,
                        forecast.getFilterCategory()
                ))
                .blockOptional()
                .orElseThrow(() -> new WeatherNotFoundException("해당 지역의 날씨 정보가 존재하지 않습니다."));

        LocalDateTime startTime = now.truncatedTo(ChronoUnit.HOURS);

        List<WeatherDTO.Detail> weatherForecasts = weather.detail().stream()
                .filter(detail -> !toDateTime(weather.date(), detail).isBefore(startTime))
                .sorted(Comparator.comparing(detail -> toDateTime(weather.date(), detail)))
                .limit(limit)
                .toList();

        if (weatherForecasts.isEmpty()) {
            throw new WeatherNotFoundException("현재 시간 이후의 날씨 정보가 존재하지 않습니다.");
        }

        return new WeatherDTO(weather.date(), weatherForecasts);
    }

    private LocalDateTime toDateTime(String date, WeatherDTO.Detail detail) {
        return LocalDateTime.parse(
                date + detail.time(),
                FORECAST_DATE_TIME_FORMATTER
        );
    }
}
