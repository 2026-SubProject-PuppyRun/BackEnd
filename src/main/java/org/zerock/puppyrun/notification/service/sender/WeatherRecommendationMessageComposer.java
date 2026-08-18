package org.zerock.puppyrun.notification.service.sender;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.notification.client.DTO.PushTask;
import org.zerock.puppyrun.notification.client.DTO.TokenPushTask;
import org.zerock.puppyrun.notification.repository.DTO.EnabledNotifications;
import org.zerock.puppyrun.weather.DTO.GridPoint;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.DTO.WeatherRegion;
import org.zerock.puppyrun.weather.service.WeatherQueryService;
import org.zerock.puppyrun.weather.utils.WeatherRegionCatalog;

/**
 * 시간 출처와 무관하게 날씨 추천 TokenPushTask를 생성하는 Bean입니다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class WeatherRecommendationMessageComposer {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final int FORECAST_HOURS = 3;

    private final WeatherRegionCatalog weatherRegionCatalog;
    private final WeatherQueryService weatherQueryService;

    public List<PushTask> createPushTasks(
            List<EnabledNotifications> memberSettings,
            List<WeatherRecommendationTarget> targets,
            LocalDateTime referenceTime
    ) {
        if (memberSettings.isEmpty() || targets.isEmpty()) {
            return List.of();
        }
        Map<UUID, WeatherRecommendationTarget> targetByMemberId = targets.stream()
                .collect(Collectors.toMap(WeatherRecommendationTarget::memberId, Function.identity(),
                        (first, ignored) -> first));
        Map<WeatherRequest, Optional<WeatherDTO>> weatherCache = new HashMap<>();
        return memberSettings.stream()
                .map(member -> createTask(member, targetByMemberId.get(member.memberId()), referenceTime, weatherCache))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<PushTask> createTask(
            EnabledNotifications member, WeatherRecommendationTarget target,
            LocalDateTime referenceTime, Map<WeatherRequest, Optional<WeatherDTO>> weatherCache
    ) {
        if (target == null || target.latitude() == null || target.longitude() == null) {
            return Optional.empty();
        }
        WeatherRegion region = weatherRegionCatalog.findNearestRegion(target.latitude(), target.longitude());

        WeatherRequest request = new WeatherRequest(new GridPoint(region.nx(), region.ny()),
                forecastStartTime(target, referenceTime));

        Optional<WeatherDTO> weather = weatherCache.computeIfAbsent(request, this::findForecast);

        return weather.map(value -> new TokenPushTask(
                        member.fcmToken(),
                        member.type(),
                        "산책하기 좋은 시간이에요!",
                        target.time().format(TIME_FORMATTER) + " 전후 날씨예요.\n" + formatForecasts(value)
                )
        );
    }

    private LocalDateTime forecastStartTime(WeatherRecommendationTarget target, LocalDateTime referenceTime) {
        LocalDateTime preferredAt = referenceTime.withHour(target.time().getHour())
                .withMinute(target.time().getMinute())
                .withSecond(0).withNano(0);
        return (preferredAt.isBefore(referenceTime) ?
                preferredAt.plusDays(1) :
                preferredAt
        ).minusHours(1);
    }

    private Optional<WeatherDTO> findForecast(WeatherRequest request) {
        try {
            return Optional.of(
                    weatherQueryService.getFcstWeather(request.gridPoint(), request.startTime(), FORECAST_HOURS));
        } catch (RuntimeException exception) {
            log.warn("event=weather_recommendation_forecast_not_found, nx={}, ny={}, startTime={}",
                    request.gridPoint().nx(), request.gridPoint().ny(), request.startTime(), exception);
            return Optional.empty();
        }
    }

    private String formatForecasts(WeatherDTO weather) {
        return weather.weatherList().stream().map(this::formatForecast).collect(Collectors.joining("\n"));
    }

    private String formatForecast(WeatherDTO.WeatherList forecast) {
        String precipitation = forecast.pty().getDescription();
        if (forecast.pcp() != null && forecast.pcp() > 0) {
            precipitation += " " + forecast.pcp() + "mm";
        }
        String time = forecast.time() != null && forecast.time().matches("\\d{4}")
                ? forecast.time().substring(0, 2) + ":" + forecast.time().substring(2) : forecast.time();
        return time + " " + forecast.temp() + "℃ · " + forecast.sky().getDescription() + " · " + precipitation;
    }

    private record WeatherRequest(GridPoint gridPoint, LocalDateTime startTime) {
    }
}
