package org.zerock.puppyrun.weather.messaging.DBRetry;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.zerock.puppyrun.common.config.RabbitQueueType;
import org.zerock.puppyrun.weather.DTO.WeatherSaveCommand;

/**
 * 1차 DB 백업 저장 실패 건에 대해 DB 일괄 저장 10초 지연 보상 재시도 메시지를 RabbitMQ에 발송하는 전담 Publisher 클래스입니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WeatherDBRetryPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * DB 저장에 실패한 성공 수집 결과 리스트를 10초 지연 큐로 발송합니다.
     *
     * @param commands DB 백업 저장 실패 명령 리스트
     */
    public void publish(List<WeatherSaveCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }

        RabbitQueueType queueType = RabbitQueueType.WEATHER_DB_RETRY;
        String exchange = queueType.getDlxExchangeName();
        String routingKey = queueType.getDelayRoutingKey();

        WeatherDBRetryMessage message = new WeatherDBRetryMessage(commands);

        rabbitTemplate.convertAndSend(exchange, routingKey, message);
        log.info("[weather DB 재시도 큐] 10초 지연 큐로 메시지 발송 완료 (총 {}건)", commands.size());
    }
}
