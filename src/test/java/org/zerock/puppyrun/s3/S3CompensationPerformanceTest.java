package org.zerock.puppyrun.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

import io.awspring.cloud.s3.S3Template;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.puppyrun.a_config.TestContainerConfig;
import org.zerock.puppyrun.common.s3.PathContext;
import org.zerock.puppyrun.common.s3.S3Service;
import org.zerock.puppyrun.support.BenchmarkStatistics;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
@EnabledIfEnvironmentVariable(named = "S3_COMPENSATION_BENCHMARK_ENABLED", matches = "true")
@TestPropertySource(properties = {
        "logging.level.org.zerock.puppyrun.common.s3=OFF"
})
@DisplayName("S3 다중 업로드와 트랜잭션 보상 수동 성능 테스트")
class S3CompensationPerformanceTest extends TestContainerConfig {

    private static final int WARM_UP_COUNT = 5;
    private static final int SAMPLE_COUNT = 30;
    private static final PathContext PATH = new PathContext.UserProfileContext(
            UUID.fromString("00000000-0000-0000-0000-000000000001")
    );

    @Autowired
    private S3Service s3Service;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockBean
    private S3Template s3Template;

    private final AtomicReference<String> failureMarker = new AtomicReference<>();
    private final AtomicInteger uploadAttempts = new AtomicInteger();
    private final AtomicInteger deleteAttempts = new AtomicInteger();

    @BeforeEach
    void setUp() {
        reset(s3Template);

        doAnswer(invocation -> {
            uploadAttempts.incrementAndGet();
            String key = invocation.getArgument(1, String.class);
            String marker = failureMarker.get();
            if (marker != null && key.endsWith(marker)) {
                throw new RuntimeException("controlled S3 upload failure");
            }
            return null;
        }).when(s3Template).upload(any(), any(), any(), any());

        doAnswer(invocation -> {
            deleteAttempts.incrementAndGet();
            return null;
        }).when(s3Template).deleteObject(any(), any());
    }

    @Test
    @DisplayName("Mock S3 경계에서 100장 순차 업로드와 81번째 실패 보상 완료 시간을 반복 측정한다")
    void benchmarkUploadAndRollbackCompensation() {
        // given
        List<MultipartFile> successFiles = files(false);
        List<MultipartFile> failureFiles = files(true);

        IntStream.range(0, WARM_UP_COUNT).forEach(ignored -> measureSuccessfulUpload(successFiles));
        IntStream.range(0, WARM_UP_COUNT).forEach(ignored -> measureRollback(failureFiles));

        // when
        List<Long> uploadSamples = new ArrayList<>(SAMPLE_COUNT);
        List<Long> rollbackSamples = new ArrayList<>(SAMPLE_COUNT);
        for (int sample = 0; sample < SAMPLE_COUNT; sample++) {
            clearInvocations(s3Template);
            uploadSamples.add(measureSuccessfulUpload(successFiles));
            rollbackSamples.add(measureRollback(failureFiles));
        }

        BenchmarkStatistics uploadStats = BenchmarkStatistics.fromNanos(uploadSamples);
        BenchmarkStatistics rollbackStats = BenchmarkStatistics.fromNanos(rollbackSamples);

        // then
        System.out.println(uploadStats.format("s3_upload_all_100", 100, "files"));
        System.out.println(rollbackStats.format("s3_fail_at_81_and_delete_80", 80, "deleted-files"));
    }

    private long measureSuccessfulUpload(List<MultipartFile> files) {
        failureMarker.set(null);
        uploadAttempts.set(0);
        deleteAttempts.set(0);

        long startedAt = System.nanoTime();
        List<String> keys = transactionTemplate.execute(status -> s3Service.uploadAll(files, PATH));
        long elapsed = System.nanoTime() - startedAt;

        assertThat(keys).hasSize(100).doesNotHaveDuplicates();
        assertThat(uploadAttempts).hasValue(100);
        assertThat(deleteAttempts).hasValue(0);
        return elapsed;
    }

    private long measureRollback(List<MultipartFile> files) {
        failureMarker.set("_fail-80.png");
        uploadAttempts.set(0);
        deleteAttempts.set(0);

        long startedAt = System.nanoTime();
        Throwable thrown = catchThrowable(() -> transactionTemplate.executeWithoutResult(
                status -> s3Service.uploadAll(files, PATH)
        ));
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(1))
                .until(() -> deleteAttempts.get() == 80);
        long elapsed = System.nanoTime() - startedAt;

        assertThat(thrown)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("controlled S3 upload failure");
        assertThat(uploadAttempts).hasValue(81);
        assertThat(deleteAttempts).hasValue(80);
        return elapsed;
    }

    private List<MultipartFile> files(boolean includeFailure) {
        return IntStream.range(0, 100)
                .mapToObj(index -> {
                    String name = includeFailure && index == 80
                            ? "fail-80.png"
                            : "success-" + index + ".png";
                    return (MultipartFile) new MockMultipartFile(
                            "file",
                            name,
                            "image/png",
                            new byte[1_024]
                    );
                })
                .toList();
    }
}
