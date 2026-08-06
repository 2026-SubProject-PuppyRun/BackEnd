package org.zerock.puppyrun.weather.service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zerock.puppyrun.weather.DTO.GridPoint;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.DTO.WeatherUpdateResult;
import org.zerock.puppyrun.weather.exception.WeatherApiResponseException;
import org.zerock.puppyrun.weather.utils.WeatherForecast;
import org.zerock.puppyrun.weather.utils.WeatherRegionCatalog;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 기상청 Open API를 비동기로 호출하고 응답을 날씨 수집 결과로 변환하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherForecastCollector {

    private static final int MAX_CONCURRENT_REQUESTS = 5;
    private static final Duration API_REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final WeatherApiClient weatherApiClient;
    private final WeatherRegionCatalog weatherRegionCatalog;
    private final WeatherMapper weatherMapper;

    /**
     * 등록된 모든 지역의 격자 좌표에 대해 지정된 예보를 수집합니다.
     *
     * @param forecast 수집할 예보
     * @return 지역별 날씨 수집 결과
     */
    public Flux<WeatherUpdateResult> collectAll(WeatherForecast forecast) {
        if (forecast == null) {
            return Flux.error(new IllegalArgumentException("예보 정보는 필수입니다."));
        }

        List<GridPoint> gridPoints = weatherRegionCatalog.getRegions().stream()
                .map(region -> new GridPoint(region.nx(), region.ny()))
                .distinct()
                .toList();

        return collect(forecast, gridPoints);
    }

    /**
     * DB를 조회하여 기존 날씨 정보를 캐시하고, DB에 없는 격자 정보만 기상청 API로 요청하여 가져옵니다.
     *
     * @param forecast 수집 대상 예보
     * @return DB에 없어 API로 새로 수집된 결과 Flux
     */
    public Flux<WeatherUpdateResult> collect(WeatherForecast forecast, List<GridPoint> gridPoints) {
        if (forecast == null) {
            return Flux.error(new IllegalArgumentException("예보 정보는 필수입니다."));
        }

        log.info(
                "기상청 날씨 예보 수집 시작: 예보종류={}, 총 {}개 격자 좌표",
                forecast.getClass().getSimpleName(),
                gridPoints.size()
        );

        return Flux.fromIterable(gridPoints)
                .flatMap(
                        gridPoint -> fetchForecast(forecast, gridPoint),
                        MAX_CONCURRENT_REQUESTS
                )
                .doOnComplete(() ->
                        log.info(
                                "기상청 날씨 예보 수집 완료: 예보종류={}",
                                forecast.getClass().getSimpleName()
                        )
                );
    }

    /**
     * 지정된 단일 격자 좌표의 날씨를 수집합니다.
     *
     * @param forecast  수집할 예보
     * @param gridPoint 수집 대상 격자 좌표
     * @return 날씨 수집 결과
     */
    public Mono<WeatherUpdateResult> collectOne(
            WeatherForecast forecast,
            GridPoint gridPoint
    ) {
        if (forecast == null) {
            return Mono.error(new IllegalArgumentException("예보 정보는 필수입니다."));
        }

        if (gridPoint == null) {
            return Mono.error(new IllegalArgumentException("격자 좌표는 필수입니다."));
        }

        return fetchForecast(forecast, gridPoint);
    }

    /**
     * 기상청 API 응답을 받은 뒤 Mapper로 WeatherDTO를 생성하고 성공 결과인 WeatherUpdateResult로 변환합니다.
     */
    private Mono<WeatherUpdateResult> fetchForecast(
            WeatherForecast forecast,
            GridPoint gridPoint
    ) {
        return weatherApiClient.fetchWeather(forecast.getPara(gridPoint))
                .timeout(API_REQUEST_TIMEOUT)
                .map(response -> {
                    WeatherDTO weather = weatherMapper.toWeatherDTO(
                            response,
                            forecast.getFilterCategory()
                    );

                    return WeatherUpdateResult.success(
                            forecast,
                            gridPoint,
                            weather
                    );
                })
                .onErrorResume(error ->
                        handleFailureResponse(forecast, gridPoint, error)
                );
    }

    /**
     * API 호출 또는 응답 매핑 실패를 실패 결과로 변환합니다.
     */
    private Mono<WeatherUpdateResult> handleFailureResponse(
            WeatherForecast forecast,
            GridPoint gridPoint,
            Throwable error
    ) {
        if (error instanceof WeatherApiResponseException apiEx) {
            log.warn(
                    "기상청 API 응답 오류: nx={}, ny={}, code={}, message={}",
                    gridPoint.nx(),
                    gridPoint.ny(),
                    apiEx.getResponseCode(),
                    apiEx.getMessage()
            );
        } else {
            log.error(
                    "기상청 API 호출 또는 응답 변환 실패: nx={}, ny={}, 예보종류={}, error={}",
                    gridPoint.nx(),
                    gridPoint.ny(),
                    forecast.getClass().getSimpleName(),
                    Objects.toString(
                            error.getMessage(),
                            error.getClass().getSimpleName()
                    ),
                    error
            );
        }

        return Mono.just(
                WeatherUpdateResult.failure(
                        forecast,
                        gridPoint
                )
        );
    }
}
