package org.zerock.puppyrun.weather.messaging.APIRetry;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.zerock.puppyrun.common.config.RabbitQueueType;
import org.zerock.puppyrun.weather.DTO.WeatherUpdateResult;

/**
 * 1차 수집 실패 건에 대해 기상청 API 10분 지연 보상 재시도 메시지를 RabbitMQ에 발송하는 전담 Publisher 클래스입니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WeatherAPIRetryPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 수집 실패한 결과 리스트를 개별 격자 단위 메시지로 변환하여 10분 지연 큐로 발송합니다.
     *
     * @param fails 1차 API 수집 실패 결과 리스트
     */
    public void publish(List<WeatherUpdateResult> fails) {
        if (fails == null || fails.isEmpty()) {
            return;
        }

        RabbitQueueType queueType = RabbitQueueType.WEATHER_API_RETRY;
        String exchange = queueType.getDlxExchangeName();
        String routingKey = queueType.getDelayRoutingKey();

        for (WeatherUpdateResult failedResult : fails) {
            WeatherAPIRetryMessage message = WeatherAPIRetryMessage.from(failedResult);

            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            log.info("API 실패 응답 RabbitMQ 10분 지연 큐 발송 완료: nx={}, ny={}",
                    failedResult.gridPoint().nx(), failedResult.gridPoint().ny());
        }
    }
}
