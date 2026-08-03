package org.zerock.puppyrun.weather.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.zerock.puppyrun.common.config.CacheType;
import org.zerock.puppyrun.weather.DTO.GridPoint;
import org.zerock.puppyrun.weather.DTO.PrecipitationType;
import org.zerock.puppyrun.weather.DTO.SkyType;
import org.zerock.puppyrun.weather.DTO.WeatherApiPara;
import org.zerock.puppyrun.weather.DTO.WeatherApiResponse;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.DTO.WeatherFilterCategory;
import org.zerock.puppyrun.weather.DTO.WeatherRegion;
import org.zerock.puppyrun.weather.exception.WeatherApiResponseException;
import org.zerock.puppyrun.weather.utils.WeatherForecast;
import org.zerock.puppyrun.weather.utils.WeatherForecast.ShortTerm;
import org.zerock.puppyrun.weather.utils.WeatherRegionCatalog;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

class WeatherForecastUpdaterTest {

    private final WeatherApiClient weatherApiClient = mock(WeatherApiClient.class);
    private final WeatherMapper weatherMapper = mock(WeatherMapper.class);
    private final CacheManager cacheManager = new ConcurrentMapCacheManager(
            CacheType.SHORT_TERM_WEATHER.getCacheName()
    );
    private final WeatherRegionCatalog regionCatalog = mock(WeatherRegionCatalog.class);
    private final WeatherForecastUpdater updater = new WeatherForecastUpdater(
            weatherApiClient,
            weatherMapper,
            cacheManager,
            regionCatalog
    );

    @Test
    @DisplayName("단기예보 전략으로 조회한 응답을 공통 DTO로 변환해 단기예보 캐시에 저장한다")
    void updateShortTermForecast() {
        // given
        GridPoint gridPoint = new GridPoint(60, 127);
        LocalDateTime requestTime = LocalDateTime.of(2026, 8, 3, 10, 30);
        WeatherForecast forecast = new ShortTerm();
        WeatherApiResponse apiResponse = mock(WeatherApiResponse.class);
        WeatherDTO weather = new WeatherDTO(
                List.of(new WeatherDTO.WeatherList(
                        "20260803",
                        "1100",
                        "25.0",
                        SkyType.CLOUDY,
                        PrecipitationType.RAIN,
                        "1mm 미만"
                ))
        );
        WeatherFilterCategory category = new WeatherFilterCategory(
                "TMP",
                "SKY",
                "PTY",
                "PCP"
        );
        when(weatherApiClient.fetchWeather(any(WeatherApiPara.class)))
                .thenReturn(Mono.just(apiResponse));
        when(weatherMapper.toWeatherDTO(apiResponse, category)).thenReturn(weather);

        // when
        WeatherDTO result = updater.update(
                forecast,
                CacheType.SHORT_TERM_WEATHER,
                requestTime,
                gridPoint
        ).block();

        // then
        ArgumentCaptor<WeatherApiPara> paraCaptor = ArgumentCaptor.forClass(WeatherApiPara.class);
        verify(weatherApiClient).fetchWeather(paraCaptor.capture());
        WeatherApiPara para = paraCaptor.getValue();
        assertThat(para.path()).isEqualTo("/getVilageFcst");
        assertThat(para.baseDate()).isEqualTo("20260803");
        assertThat(para.baseTime()).isEqualTo("0800");
        verify(weatherMapper).toWeatherDTO(apiResponse, category);
        Cache cache = cacheManager.getCache(CacheType.SHORT_TERM_WEATHER.getCacheName());
        assertThat(cache).isNotNull();
        assertThat(cache.get(gridPoint, WeatherDTO.class)).isEqualTo(weather);
        assertThat(result).isEqualTo(weather);
    }

    @Test
    @DisplayName("이전 지역의 응답을 기다리는 동안 다음 지역의 예보 요청을 시작한다")
    void updateNextRegionWithoutWaitingForPreviousResponse() throws InterruptedException {
        // given
        LocalDateTime requestTime = LocalDateTime.of(2026, 8, 3, 10, 30);
        WeatherForecast forecast = new ShortTerm();
        Sinks.One<WeatherApiResponse> pendingFirstResponse = Sinks.one();
        CountDownLatch secondRequestStarted = new CountDownLatch(1);
        when(regionCatalog.getRegions()).thenReturn(List.of(
                new WeatherRegion(List.of("서울특별시", "용산구"), 60, 126, 37.53, 126.97),
                new WeatherRegion(List.of("서울특별시", "종로구"), 61, 127, 37.58, 126.99)
        ));
        when(weatherApiClient.fetchWeather(any(WeatherApiPara.class)))
                .thenAnswer(invocation -> {
                    WeatherApiPara para = invocation.getArgument(0);
                    if (para.nx() == 60) {
                        return pendingFirstResponse.asMono();
                    }

                    secondRequestStarted.countDown();
                    return Mono.empty();
                });

        // when
        updater.update(forecast, CacheType.SHORT_TERM_WEATHER, requestTime);

        // then
        assertThat(secondRequestStarted.await(4, TimeUnit.SECONDS)).isTrue();
        pendingFirstResponse.tryEmitEmpty();
    }

    @Test
    @DisplayName("응답을 받지 못한 타임아웃은 응답 코드가 없는 오류로 분류한다")
    void classifyTimeoutWithoutResponseCode() {
        // given
        TimeoutException exception = new TimeoutException("reactor timeout");

        // when
        WeatherForecastUpdater.WeatherFailure failure = updater.classifyFailure(exception);

        // then
        assertThat(failure.errorType()).isEqualTo("TIMEOUT");
        assertThat(failure.responseCode()).isEqualTo("NO_RESPONSE");
        assertThat(failure.detail()).contains("HTTP 응답 본문 완료 신호");
    }

    @Test
    @DisplayName("HTTP 오류 응답은 실제 상태 코드를 포함해 분류한다")
    void classifyHttpStatusCode() {
        // given
        WebClientResponseException exception = WebClientResponseException.create(
                429,
                "Too Many Requests",
                HttpHeaders.EMPTY,
                "요청 한도를 초과했습니다.".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        // when
        WeatherForecastUpdater.WeatherFailure failure = updater.classifyFailure(exception);

        // then
        assertThat(failure.errorType()).isEqualTo("HTTP_ERROR");
        assertThat(failure.responseCode()).isEqualTo("429");
        assertThat(failure.detail()).contains("요청 한도를 초과했습니다.");
    }

    @Test
    @DisplayName("기상청 응답 오류는 기상청 resultCode를 포함해 분류한다")
    void classifyWeatherApiResponseCode() {
        // given
        WeatherApiResponseException exception = new WeatherApiResponseException(
                "22",
                "LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR"
        );

        // when
        WeatherForecastUpdater.WeatherFailure failure = updater.classifyFailure(exception);

        // then
        assertThat(failure.errorType()).isEqualTo("API_RESPONSE_ERROR");
        assertThat(failure.responseCode()).isEqualTo("22");
        assertThat(failure.detail()).contains("LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR");
    }
}
