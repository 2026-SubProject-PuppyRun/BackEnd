package org.zerock.puppyrun.weather.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.codec.DecodingException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.zerock.puppyrun.common.config.CacheType;
import org.zerock.puppyrun.common.exception.CacheNotFoundException;
import org.zerock.puppyrun.common.exception.ExternalApiParsingException;
import org.zerock.puppyrun.weather.DTO.GridPoint;
import org.zerock.puppyrun.weather.DTO.WeatherApiPara;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.exception.WeatherApiResponseException;
import org.zerock.puppyrun.weather.utils.WeatherForecast;
import org.zerock.puppyrun.weather.utils.WeatherRegionCatalog;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 예보 전략을 API 요청, 공통 DTO 변환, 캐시 저장 순서로 실행합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherForecastUpdater {

    private static final int API_CALL_INTERVAL_MILLIS = 1000;
    private static final int MAX_CONCURRENT_API_CALLS = 5;
    private static final Duration API_RESPONSE_TIMEOUT = Duration.ofSeconds(10);

    private final WeatherApiClient weatherApiClient;
    private final WeatherMapper weatherMapper;
    private final CacheManager cacheManager;
    private final WeatherRegionCatalog regionCatalog;

    public void update(WeatherForecast forecast, CacheType cacheType, LocalDateTime requestTime) {
        log.info(
                "날씨 데이터 업데이트 시작: strategy={}, cache={}",
                forecast.getClass().getSimpleName(),
                cacheType.getCacheName()
        );

        Flux.fromIterable(getGridPoints())
                .delayElements(Duration.ofMillis(API_CALL_INTERVAL_MILLIS))
                .flatMap(
                        gridPoint -> update(forecast, cacheType, requestTime, gridPoint),
                        MAX_CONCURRENT_API_CALLS
                )
                .doOnComplete(() -> log.info(
                        "날씨 데이터 업데이트 완료: strategy={}, cache={}",
                        forecast.getClass().getSimpleName(),
                        cacheType.getCacheName()
                ))
                .subscribe();
    }

    Mono<WeatherDTO> update(
            WeatherForecast forecast,
            CacheType cacheType,
            LocalDateTime requestTime,
            GridPoint gridPoint
    ) {
        WeatherApiPara para = forecast.getPara(requestTime, gridPoint);

        return weatherApiClient.fetchWeather(para)
                .timeout(API_RESPONSE_TIMEOUT)
                .map(response -> weatherMapper.toWeatherDTO(
                        response,
                        forecast.getFilterCategory()
                ))
                .doOnNext(weather -> putWeatherToCache(cacheType, gridPoint, weather))
                .doOnNext(weather -> log.info(
                        "캐시 갱신 완료: cache={}, nx={}, ny={}",
                        cacheType.getCacheName(),
                        gridPoint.nx(),
                        gridPoint.ny()
                ))
                .onErrorResume(exception -> {
                    logUpdateFailure(forecast, para, exception);
                    return Mono.empty();
                });
    }

    private void logUpdateFailure(
            WeatherForecast forecast,
            WeatherApiPara para,
            Throwable exception
    ) {
        WeatherFailure failure = classifyFailure(exception);

        log.error(
                "날씨 갱신 실패: strategy={}, path={}, baseDate={}, baseTime={}, nx={}, ny={}, "
                        + "errorType={}, responseCode={}, detail={}",
                forecast.getClass().getSimpleName(),
                para.path(),
                para.baseDate(),
                para.baseTime(),
                para.nx(),
                para.ny(),
                failure.errorType(),
                failure.responseCode(),
                failure.detail()
        );
    }

    WeatherFailure classifyFailure(Throwable exception) {
        if (exception instanceof TimeoutException) {
            return new WeatherFailure(
                    "TIMEOUT",
                    "NO_RESPONSE",
                    "%d초 안에 HTTP 응답 본문 완료 신호를 받지 못했습니다."
                            .formatted(API_RESPONSE_TIMEOUT.toSeconds())
            );
        }

        if (exception instanceof WebClientResponseException responseException) {
            return new WeatherFailure(
                    "HTTP_ERROR",
                    String.valueOf(responseException.getStatusCode().value()),
                    abbreviate(responseException.getResponseBodyAsString())
            );
        }

        if (exception instanceof WebClientRequestException requestException) {
            Throwable cause = getRootCause(requestException);
            return new WeatherFailure(
                    "CONNECTION_ERROR",
                    "NO_RESPONSE",
                    cause.getClass().getSimpleName() + ": " + safeMessage(cause)
            );
        }

        if (exception instanceof DecodingException) {
            return new WeatherFailure(
                    "DECODING_ERROR",
                    "RESPONSE_RECEIVED",
                    safeMessage(exception)
            );
        }

        if (exception instanceof WeatherApiResponseException responseException) {
            return new WeatherFailure(
                    "API_RESPONSE_ERROR",
                    responseException.getResponseCode(),
                    safeMessage(responseException)
            );
        }

        if (exception instanceof ExternalApiParsingException apiException) {
            return new WeatherFailure(
                    "API_RESPONSE_ERROR",
                    apiException.getErrorCode().getCode(),
                    safeMessage(apiException)
            );
        }

        return new WeatherFailure(
                "UNKNOWN_ERROR",
                exception.getClass().getSimpleName(),
                safeMessage(exception)
        );
    }

    private Throwable getRootCause(Throwable exception) {
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    private String safeMessage(Throwable exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? "메시지 없음"
                : message.replaceAll("[\\r\\n]+", " ");
    }

    private String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return "응답 본문 없음";
        }

        String normalized = value.replaceAll("[\\r\\n]+", " ");
        int maxLength = 500;
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength) + "...";
    }

    record WeatherFailure(
            String errorType,
            String responseCode,
            String detail
    ) {
    }

    private void putWeatherToCache(
            CacheType cacheType,
            GridPoint gridPoint,
            WeatherDTO weather
    ) {
        Cache cache = cacheManager.getCache(cacheType.getCacheName());
        if (cache == null) {
            throw new CacheNotFoundException("날씨 캐시를 찾을 수 없습니다.");
        }
        cache.put(gridPoint, weather);
    }

    private List<GridPoint> getGridPoints() {
        return regionCatalog.getRegions().stream()
                .map(region -> new GridPoint(region.nx(), region.ny()))
                .distinct()
                .toList();
    }
}
