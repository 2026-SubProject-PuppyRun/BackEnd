package org.zerock.puppyrun.weather.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zerock.puppyrun.weather.DTO.WeatherSaveCommand;
import org.zerock.puppyrun.weather.DTO.WeatherUpdateResult;
import org.zerock.puppyrun.weather.messaging.APIRetry.WeatherAPIRetryPublisher;
import org.zerock.puppyrun.weather.messaging.DBRetry.WeatherDBRetryPublisher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 날씨 수집 결과의 캐시 저장, DB 저장 및 보상 재시도 분기를 제어합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherUpdateResultHandler {

    private final WeatherCommandService weatherCommandService;
    private final WeatherCacheProcess weatherCacheProcess;
    private final WeatherAPIRetryPublisher apiRetryPublisher;
    private final WeatherDBRetryPublisher dbRetryPublisher;

    /**
     * 최초 수집 결과를 처리합니다. API 실패만 10분 재시도 큐에 발행합니다.
     */
    public Mono<Void> processInitial(List<WeatherUpdateResult> results) {
        if (results == null || results.isEmpty()) {
            return Mono.empty();
        }

        return Mono.fromRunnable(() -> processInitialSynchronously(results))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /**
     * API 재시도 결과를 처리합니다. 다시 실패한 결과는 재발행하지 않습니다.
     */
    public void processRetry(WeatherUpdateResult result) {
        if (result == null || !result.success()) {
            if (result == null) {
                log.error("[weather API 재시도 큐] 재시도 결과가 없습니다.");
            } else {
                log.error(
                        "[weather API 재시도 큐] 최종 실패 type={}, nx={}, ny={} | 재발행하지 않고 실패 캐시 기록을 유지합니다.",
                        result.forecast().getType(),
                        result.gridPoint().nx(),
                        result.gridPoint().ny()
                );
            }
            return;
        }

        cacheSuccess(result);

        if (result.forecast().isDbBackupRequired()) {
            databaseSuccess(List.of(result));
        }

        log.info(
                "[weather API 재시도 큐] 재시도 성공 type={}, nx={}, ny={} | 후속 저장 완료",
                result.forecast().getType(),
                result.gridPoint().nx(),
                result.gridPoint().ny()
        );
    }

    private void processInitialSynchronously(List<WeatherUpdateResult> results) {
        List<WeatherUpdateResult> successes = results.stream()
                .filter(WeatherUpdateResult::success)
                .toList();
        List<WeatherUpdateResult> failures = results.stream()
                .filter(result -> !result.success())
                .toList();

        log.info(
                "[weather 1차 수집 분류 완료] type={}, 총 {}건 (성공 {}건, API 실패 {}건)",
                results.getFirst().forecast().getType(),
                results.size(),
                successes.size(),
                failures.size()
        );

        successes.forEach(this::cacheSuccess);

        List<WeatherUpdateResult> dbTargets = successes.stream()
                .filter(result -> result.forecast().isDbBackupRequired())
                .toList();

        databaseSuccess(dbTargets);

        failures.forEach(weatherCacheProcess::putFailed);

        publishApiRetries(failures);
    }

    private void cacheSuccess(WeatherUpdateResult result) {
        weatherCacheProcess.putWeather(
                result.forecast().getCacheType(),
                result.gridPoint(),
                result.weather()
        );
        weatherCacheProcess.removeFailed(result);
    }

    private void databaseSuccess(List<WeatherUpdateResult> results) {
        if (results.isEmpty()) {
            return;
        }

        List<WeatherSaveCommand> commands = results.stream()
                .map(WeatherSaveCommand::from)
                .toList();

        try {
            weatherCommandService.save(commands);
        } catch (Exception saveException) {
            log.warn(
                    "[weather DB 재시도 큐] 저장 실패 총 {}건 저장 실패: {} | 10초 지연 큐 발송",
                    commands.size(),
                    saveException.getMessage(),
                    saveException
            );

            try {
                dbRetryPublisher.publish(commands);
            } catch (Exception publishException) {
                log.error(
                        "[weather DB 재시도 큐] 재시도 발행 실패 총 {}건을 지연 큐에 발행하지 못했습니다: {}",
                        commands.size(),
                        publishException.getMessage(),
                        publishException
                );
            }
        }
    }

    private void publishApiRetries(List<WeatherUpdateResult> failures) {
        if (failures.isEmpty()) {
            return;
        }

        try {
            apiRetryPublisher.publish(failures);
        } catch (Exception exception) {
            log.error(
                    "[weather API 재시도 큐] 재시도 발행 실패 총 {}건을 지연 큐에 발행하지 못했습니다: {}",
                    failures.size(),
                    exception.getMessage(),
                    exception
            );
        }
    }
}
