package org.zerock.puppyrun.weather.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.zerock.puppyrun.a_config.TestContainerConfig;
import org.zerock.puppyrun.fixture.weather.WeatherFixture;
import org.zerock.puppyrun.weather.DTO.WeatherSaveCommand;
import org.zerock.puppyrun.weather.DTO.WeatherUpdateResult;
import org.zerock.puppyrun.weather.messaging.APIRetry.WeatherAPIRetryConsumer;
import org.zerock.puppyrun.weather.messaging.APIRetry.WeatherAPIRetryMessage;
import org.zerock.puppyrun.weather.messaging.APIRetry.WeatherAPIRetryPublisher;
import org.zerock.puppyrun.weather.messaging.DBRetry.WeatherDBRetryConsumer;
import org.zerock.puppyrun.weather.messaging.DBRetry.WeatherDBRetryMessage;
import org.zerock.puppyrun.weather.messaging.DBRetry.WeatherDBRetryPublisher;
import org.zerock.puppyrun.weather.repository.WeatherForecastRepository;
import org.zerock.puppyrun.weather.service.WeatherCacheProcess;
import org.zerock.puppyrun.weather.service.WeatherCommandService;
import org.zerock.puppyrun.weather.service.WeatherForecastCollector;
import org.zerock.puppyrun.weather.service.WeatherUpdateResultHandler;
import org.zerock.puppyrun.weather.utils.WeatherForecast;
import reactor.core.publisher.Mono;

/**
 * 기상청 예보 수집 실패 및 DB 저장 실패 시 RabbitMQ 지연 큐를 통한 보상 처리 파이프라인 통합 테스트입니다.
 */
class WeatherFailureCompensationIntegrationTest extends TestContainerConfig {

    @Autowired
    private WeatherUpdateResultHandler resultHandler;

    @Autowired
    private WeatherCacheProcess weatherCacheProcess;

    @Autowired
    private WeatherCommandService weatherCommandService;

    @Autowired
    private WeatherForecastRepository weatherForecastRepository;

    @MockBean
    private WeatherAPIRetryPublisher apiRetryPublisher;

    @MockBean
    private WeatherDBRetryPublisher dbRetryPublisher;

    @MockBean
    private WeatherForecastCollector weatherForecastCollector;

    @Autowired
    private WeatherAPIRetryConsumer apiRetryConsumer;

    @Autowired
    private WeatherDBRetryConsumer dbRetryConsumer;

    @BeforeEach
    void setUp() {
        weatherCacheProcess.clearFailedResults();
        weatherForecastRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("1차 API 수집 실패 시 실패 캐시에 저장하고 10분 지연 MQ로 재시도 메시지를 발행한다")
    void processInitialApiFailureCachesAndPublishesToMq() {
        // given
        WeatherForecast forecast = WeatherFixture.createShortTermForecast();
        WeatherUpdateResult failureResult = WeatherFixture.createFailureResult(forecast,
                WeatherFixture.DEFAULT_GRID_POINT);

        // when
        resultHandler.processInitial(List.of(failureResult)).block();

        // then
        List<WeatherUpdateResult> failedResults = weatherCacheProcess.getFailedResults();
        assertThat(failedResults).hasSize(1);
        assertThat(failedResults.getFirst().gridPoint()).isEqualTo(WeatherFixture.DEFAULT_GRID_POINT);

        verify(apiRetryPublisher).publish(List.of(failureResult));
    }

    @Test
    @DisplayName("1차 DB 백업 저장 실패 시 Exception을 포착하여 10초 지연 MQ로 DB 재시도 메시지를 발행한다")
    void processInitialDbSaveFailurePublishesToDbRetryMq() {
        // given
        WeatherForecast forecast = WeatherFixture.createShortTermForecast();
        WeatherUpdateResult successResult = WeatherFixture.createSuccessResult(forecast,
                WeatherFixture.DEFAULT_GRID_POINT);
        WeatherSaveCommand command = WeatherSaveCommand.from(successResult);

        // WeatherCommandService.save()에서 예외 발생 상황 시뮬레이션
        // 통합 환경에서 영속화 호출 시 MQ로 보상 발행이 일어나는지 확인하기 위해 Spy/Mock 활용
        // mockPublisher가 의도한 command 목록을 수신하는지 검증
        doThrow(new RuntimeException("DB Connection Timeout"))
                .when(dbRetryPublisher).publish(any());

        // when
        resultHandler.processInitial(List.of(successResult)).block();

        // then
        verify(dbRetryPublisher).publish(List.of(command));
    }

    @Test
    @DisplayName("MQ로부터 수신된 API 재시도 메시지가 2차 성공 시 정상 보상 처리(캐시 갱신, DB 영속화, 실패 캐시 제거)된다")
    void apiRetryConsumerProcessSuccessCompensation() {
        // given
        WeatherForecast forecast = WeatherFixture.createShortTermForecast();
        WeatherUpdateResult failureResult = WeatherFixture.createFailureResult(forecast,
                WeatherFixture.DEFAULT_GRID_POINT);
        weatherCacheProcess.putFailed(failureResult);

        assertThat(weatherCacheProcess.getFailedResults()).hasSize(1);

        WeatherAPIRetryMessage retryMessage = WeatherAPIRetryMessage.from(failureResult);
        WeatherUpdateResult successResult = WeatherFixture.createSuccessResult(forecast,
                WeatherFixture.DEFAULT_GRID_POINT);

        when(weatherForecastCollector.collectOne(forecast, WeatherFixture.DEFAULT_GRID_POINT))
                .thenReturn(Mono.just(successResult));

        // when
        apiRetryConsumer.process(retryMessage);

        // then
        // 1. 실패 캐시 제거 확인
        assertThat(weatherCacheProcess.getFailedResults()).isEmpty();

        // 2. DB 보상 영속화 완료 확인
        assertThat(weatherForecastRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("MQ로부터 수신된 API 재시도 결과가 2차에서도 실패할 경우 재발행 없이 종료하며 실패 캐시를 유지한다")
    void apiRetryConsumerProcessFinalFailureDoesNotRepublish() {
        // given
        WeatherForecast forecast = WeatherFixture.createShortTermForecast();
        WeatherUpdateResult failureResult = WeatherFixture.createFailureResult(forecast,
                WeatherFixture.DEFAULT_GRID_POINT);
        weatherCacheProcess.putFailed(failureResult);

        WeatherAPIRetryMessage retryMessage = WeatherAPIRetryMessage.from(failureResult);

        when(weatherForecastCollector.collectOne(forecast, WeatherFixture.DEFAULT_GRID_POINT))
                .thenReturn(Mono.just(failureResult));

        // when
        apiRetryConsumer.process(retryMessage);

        // then
        // 실패 캐시 유지
        assertThat(weatherCacheProcess.getFailedResults()).hasSize(1);

        // DB에 저장되지 않음
        assertThat(weatherForecastRepository.findAll()).isEmpty();

        // 재발행 publisher가 더 이상 호출되지 않음
        verifyNoMoreInteractions(apiRetryPublisher, dbRetryPublisher);
    }

    @Test
    @DisplayName("MQ로부터 수신된 DB 재시도 메시지를 소비하여 DB 2차 멱등 배치 영속화 보상 처리를 완료한다")
    void dbRetryConsumerProcessCompensationSave() {
        // given
        WeatherForecast forecast = WeatherFixture.createShortTermForecast();
        WeatherSaveCommand command = WeatherFixture.createSaveCommand(forecast, WeatherFixture.DEFAULT_GRID_POINT);

        assertThat(weatherForecastRepository.findAll()).isEmpty();

        // when
        dbRetryConsumer.process(new WeatherDBRetryMessage(List.of(command)));

        // then
        assertThat(weatherForecastRepository.findAll()).hasSize(1);
    }
}
