package org.zerock.puppyrun.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 큐, Exchange, DLX 바인딩 및 메시지 컨버터를 자동 구성하는 빈 설정 클래스입니다.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public MessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages(
                "org.zerock.puppyrun.weather.messaging.APIRetry",
                "org.zerock.puppyrun.weather.messaging.DBRetry"
        );
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @Bean
    public Declarables dynamicRabbitDeclarables() {
        List<Declarable> declarables = new ArrayList<>();

        for (RabbitQueueType queueType : RabbitQueueType.values()) {
            DirectExchange dlxExchange = new DirectExchange(queueType.getDlxExchangeName());
            declarables.add(dlxExchange);

            Queue targetQueue = QueueBuilder.durable(queueType.getTargetQueueName()).build();
            declarables.add(targetQueue);

            Binding targetBinding = BindingBuilder.bind(targetQueue)
                    .to(dlxExchange)
                    .with(queueType.getTargetRoutingKey());
            declarables.add(targetBinding);

            Queue delayQueue = QueueBuilder.durable(queueType.getDelayQueueName())
                    .deadLetterExchange(queueType.getDlxExchangeName())
                    .deadLetterRoutingKey(queueType.getTargetRoutingKey())
                    .ttl((int) queueType.getTtl())
                    .build();
            declarables.add(delayQueue);

            Binding delayBinding = BindingBuilder.bind(delayQueue)
                    .to(dlxExchange)
                    .with(queueType.getDelayRoutingKey());
            declarables.add(delayBinding);
        }

        return new Declarables(declarables);
    }
}
