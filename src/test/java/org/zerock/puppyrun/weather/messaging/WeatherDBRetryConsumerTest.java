package org.zerock.puppyrun.weather.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.zerock.puppyrun.weather.DTO.GridPoint;
import org.zerock.puppyrun.weather.DTO.WeatherDTO;
import org.zerock.puppyrun.weather.DTO.WeatherSaveCommand;
import org.zerock.puppyrun.weather.messaging.DBRetry.WeatherDBRetryConsumer;
import org.zerock.puppyrun.weather.messaging.DBRetry.WeatherDBRetryMessage;
import org.zerock.puppyrun.weather.service.WeatherCommandService;
import org.zerock.puppyrun.weather.utils.WeatherForecast.ForecastType;

class WeatherDBRetryConsumerTest {

    private final WeatherCommandService weatherCommandService = mock(WeatherCommandService.class);
    private final WeatherDBRetryConsumer consumer = new WeatherDBRetryConsumer(weatherCommandService);

    @Test
    @DisplayName("DB 재시도 메시지를 수신하면 포함된 저장 명령을 한 번 실행한다")
    void processRetriesDbSaveOnce() {
        // given
        List<WeatherSaveCommand> commands = List.of(new WeatherSaveCommand(
                ForecastType.SHORT_TERM,
                new GridPoint(60, 127),
                LocalDateTime.of(2026, 8, 5, 8, 0),
                new WeatherDTO(List.of())
        ));
        WeatherDBRetryMessage message = new WeatherDBRetryMessage(commands);

        // when
        consumer.process(message);

        // then
        verify(weatherCommandService).save(commands);
    }
}
