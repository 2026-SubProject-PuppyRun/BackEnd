package org.zerock.puppyrun.weather.messaging.DBRetry;

import java.util.List;
import org.zerock.puppyrun.weather.DTO.WeatherSaveCommand;

/**
 * DB 백업 일괄 저장 10초 지연 보상 재시도 전용 RabbitMQ 메시지 DTO 레코드입니다.
 *
 * @param commands DB 백업 저장을 재시도할 명령 리스트
 */
public record WeatherDBRetryMessage(
        List<WeatherSaveCommand> commands
) {

    public WeatherDBRetryMessage {
        commands = commands == null ? List.of() : List.copyOf(commands);
    }
}
