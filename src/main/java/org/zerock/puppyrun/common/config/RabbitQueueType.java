package org.zerock.puppyrun.common.config;

import lombok.Getter;

/**
 * RabbitMQ 지연 보상 큐 및 메시징 설정을 통합 관리하는 Enum입니다.
 */
@Getter
public enum RabbitQueueType {

    /**
     * 1차 기상청 API 호출 실패 시 10분 지연 후 재시도하는 큐 사양
     */
    WEATHER_API_RETRY(
            "weather.retry.api.queue",
            "weather.retry.api.delay.queue",
            "weather.retry.api.dlx",
            "weather.retry.api.key",
            "weather.retry.api.delay.key",
            600000L // 10분 TTL (600,000ms)
    ),

    /**
     * 1차 DB 백업 저장 실패 시 10초 지연 후 재시도하는 큐 사양
     */
    WEATHER_DB_RETRY(
            "weather.retry.db.queue",
            "weather.retry.db.delay.queue",
            "weather.retry.db.dlx",
            "weather.retry.db.key",
            "weather.retry.db.delay.key",
            10000L // 10초 TTL (10,000ms)
    );

    private final String targetQueueName;
    private final String delayQueueName;
    private final String dlxExchangeName;
    private final String targetRoutingKey;
    private final String delayRoutingKey;
    private final long ttl;

    RabbitQueueType(
            String targetQueueName,
            String delayQueueName,
            String dlxExchangeName,
            String targetRoutingKey,
            String delayRoutingKey,
            long ttl
    ) {
        this.targetQueueName = targetQueueName;
        this.delayQueueName = delayQueueName;
        this.dlxExchangeName = dlxExchangeName;
        this.targetRoutingKey = targetRoutingKey;
        this.delayRoutingKey = delayRoutingKey;
        this.ttl = ttl;
    }

}
