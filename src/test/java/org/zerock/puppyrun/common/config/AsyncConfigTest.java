package org.zerock.puppyrun.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncConfigTest {

    @Test
    @DisplayName("비동기 작업은 요청 traceId를 전달하고 종료 후 MDC를 정리한다")
    void propagateAndClearMdc() throws Exception {
        // given
        AsyncConfig asyncConfig = new AsyncConfig();
        TaskDecorator taskDecorator = asyncConfig.mdcTaskDecorator();
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.applicationTaskExecutor(taskDecorator);
        AtomicReference<String> propagatedTraceId = new AtomicReference<>();
        AtomicReference<String> nextTaskTraceId = new AtomicReference<>();
        CountDownLatch firstTaskCompleted = new CountDownLatch(1);
        CountDownLatch secondTaskCompleted = new CountDownLatch(1);

        try {
            MDC.put("traceId", "request-trace-id");

            // when
            executor.execute(() -> {
                propagatedTraceId.set(MDC.get("traceId"));
                firstTaskCompleted.countDown();
            });

            assertThat(firstTaskCompleted.await(3, TimeUnit.SECONDS)).isTrue();
            MDC.clear();

            executor.execute(() -> {
                nextTaskTraceId.set(MDC.get("traceId"));
                secondTaskCompleted.countDown();
            });

            assertThat(secondTaskCompleted.await(3, TimeUnit.SECONDS)).isTrue();

            // then
            assertThat(propagatedTraceId).hasValue("request-trace-id");
            assertThat(nextTaskTraceId).hasValue(null);
        } finally {
            MDC.clear();
            executor.shutdown();
        }
    }
}
