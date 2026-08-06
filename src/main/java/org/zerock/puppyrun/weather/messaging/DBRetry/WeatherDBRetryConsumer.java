package org.zerock.puppyrun.weather.messaging.DBRetry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.zerock.puppyrun.weather.service.WeatherCommandService;

/**
 * 10초 지연 큐({@code weather.retry.db.queue})로부터 DB 저장 실패 메시지를 수신하여 DB 일괄 저장(Batch Insert) 재시도를 수행하는 리스너 컨슈머입니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WeatherDBRetryConsumer {

    private final WeatherCommandService weatherCommandService;

    /**
     * RabbitMQ 큐로부터 10초 지연 보상 메시지를 수신하여 DB 일괄 저장을 재시도합니다.
     *
     * @param message DB 재시도 메시지
     */
    @RabbitListener(queues = "weather.retry.db.queue")
    public void process(WeatherDBRetryMessage message) {
        if (message == null || message.commands().isEmpty()) {
            log.info("[DB 재시도 큐 수신] 수신된 메시지에 저장할 데이터가 없습니다.");
            return;
        }

        log.info("[DB 재시도 큐 수신] 10초 지연 후 소비 시작 | 총 {}건", message.commands().size());

        try {
            weatherCommandService.save(message.commands());
            log.info("[DB 재시도 성공] 총 {}건 DB 일괄 저장 완료", message.commands().size());
        } catch (Exception e) {
            log.error("[DB 최종 실패] 총 {}건 DB 저장 최종 실패: 원인={}", message.commands().size(), e.getMessage(), e);
        }
    }
}
