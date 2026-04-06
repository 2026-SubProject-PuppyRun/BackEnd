package org.zerock.puppyrun.common.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@Configuration
public class AsyncConfig {
    /**
     * 알림 설정을 위한 스레드 풀 생성
     */
    @Bean(name = "notificationTaskExecutor")
    public Executor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 기본 스레드 수: 코어 수와 동일하게 설정
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());

        // 최대 스레드 수: CorePool이 꽉 차고 Queue도 꽉 찼을 때, 임시로 스레드를 더 생성
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);

        // 대기열(Queue) 크기: CorePool의 스레드가 모두 바쁠 때, 요청을 이만큼 쌓아둘 수 있음
        executor.setQueueCapacity(50);

        // 스레드가 다 찰 경우 메인 스레드에서 직접 처리
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 로그에 스레드 이름이 찍혀 디버깅에 용이
        executor.setThreadNamePrefix("FCM-Noti-");

        executor.initialize();
        return executor;
    }
}
