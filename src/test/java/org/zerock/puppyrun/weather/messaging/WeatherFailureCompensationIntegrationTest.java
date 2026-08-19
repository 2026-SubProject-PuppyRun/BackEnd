package org.zerock.puppyrun.weather.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zerock.puppyrun.a_config.TestContainerConfig;
import org.zerock.puppyrun.fixture.weather.WeatherFixture;
import org.zerock.puppyrun.weather.DTO.WeatherSaveCommand;
import org.zerock.puppyrun.weather.DTO.WeatherUpdateResult;
import org.zerock.puppyrun.weather.messaging.APIRetry.WeatherAPIRetryPublisher;
import org.zerock.puppyrun.weather.messaging.DBRetry.WeatherDBRetryPublisher;
import org.zerock.puppyrun.weather.repository.WeatherForecastRepository;
import org.zerock.puppyrun.weather.service.WeatherCacheProcess;
import org.zerock.puppyrun.weather.service.WeatherCommandService;
import org.zerock.puppyrun.weather.service.WeatherUpdateResultHandler;
import org.zerock.puppyrun.weather.utils.WeatherForecast;
import reactor.core.publisher.Mono;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * RabbitMQ 테스트 컨테이너와 1초 지연(1,000ms) TTL 설정을 활용한 실제 메시지 브로커 E2E 보상 처리 통합 테스트입니다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class WeatherFailureCompensationIntegrationTest extends TestContainerConfig {

    @Autowired
    private WeatherUpdateResultHandler resultHandler;

    @Autowired
    private WeatherCacheProcess weatherCacheProcess;

    @Autowired
    private WeatherCommandService weatherCommandService;

    @Autowired
    private WeatherForecastRepository weatherForecastRepository;

    @Autowired
    private WeatherAPIRetryPublisher apiRetryPublisher;

    @Autowired
    private WeatherDBRetryPublisher dbRetryPublisher;

    @BeforeEach
    void setUp() {
        weatherCacheProcess.clearFailedResults();
        weatherForecastRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("1차 API 수집 실패 시 실제 RabbitMQ 1초 지연 큐로 메시지가 발송되고 1초 후 비동기 리스너가 수신하여 2차 보상(캐시/DB)을 완료한다")
    void apiFailureFlowWithRealRabbitMqE2E() {
        // given
        WeatherForecast forecast = WeatherFixture.createShortTermForecast();
        WeatherUpdateResult failureResult = WeatherFixture.createFailureResult(forecast, WeatherFixture.DEFAULT_GRID_POINT);
        WeatherUpdateResult successResult = WeatherFixture.createSuccessResult(forecast, WeatherFixture.DEFAULT_GRID_POINT);

        // 2차 수집 시에는 성공 응답을 유도
        when(weatherForecastCollector.collectOne(any(), any()))
                .thenReturn(Mono.just(successResult));

        // when (1차 실패 처리 -> 1초 지연 RabbitMQ DLX 발행)
        resultHandler.processInitial(List.of(failureResult)).block();

        // 1차 직후에는 실패 캐시에 존재함
        assertThat(weatherCacheProcess.getFailedResults()).hasSize(1);

        // then (RabbitMQ DLX 1초 지연 후 @RabbitListener가 비동기로 메시지 소비 완료 대기)
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    // 1. 2차 수집 성공으로 실패 캐시 제거 확인
                    assertThat(weatherCacheProcess.getFailedResults()).isEmpty();
                    // 2. DB 영속화 보상 처리 완료 확인
                    assertThat(weatherForecastRepository.findAll()).hasSize(1);
                });
    }

    @Test
    @DisplayName("DB 저장 실패 메시지를 실제 RabbitMQ 1초 지연 큐로 전달하고 비동기 리스너가 수신하여 DB에 영속화한다")
    void dbFailureFlowWithRealRabbitMqE2E() {
        // given
        WeatherForecast forecast = WeatherFixture.createShortTermForecast();
        WeatherSaveCommand command = WeatherFixture.createSaveCommand(forecast, WeatherFixture.DEFAULT_GRID_POINT);

        assertThat(weatherForecastRepository.findAll()).isEmpty();

        // when (실제 DB 지연 큐로 발행)
        dbRetryPublisher.publish(List.of(command));

        // then (1초 지연 후 @RabbitListener가 메시지를 수신하여 DB 저장을 완료할 때까지 대기)
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    assertThat(weatherForecastRepository.findAll()).hasSize(1);
                });
    }
}
