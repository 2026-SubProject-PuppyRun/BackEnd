package org.zerock.puppyrun.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 시작 시 RabbitMQ 브로커와의 커넥션을 확인하고 큐 리소스를 동기화하는 컴포넌트입니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitInitializer implements ApplicationRunner {

    private final RabbitAdmin rabbitAdmin;

    @Override
    public void run(ApplicationArguments args) {
        try {
            rabbitAdmin.initialize();
            log.info("RabbitMQ 큐 및 Exchange 리소스 초기화 완료");
        } catch (Exception e) {
            log.warn("RabbitMQ 브로커에 연결할 수 없어 동기화를 건너끕니다: {}", e.getMessage());
        }
    }
}
