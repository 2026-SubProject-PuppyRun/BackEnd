package org.zerock.puppyrun.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import io.awspring.cloud.s3.S3Template;
import jakarta.persistence.EntityManager;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.puppyrun.a_config.TestContainerConfig;
import org.zerock.puppyrun.common.s3.PathContext;
import org.zerock.puppyrun.common.s3.S3Service;
import org.zerock.puppyrun.member.entity.Member;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("S3와 DB 트랜잭션 보상 처리 통합 테스트")
class S3TransactionRollbackIntegrationTest extends TestContainerConfig {

    private static final String BUCKET = "AWS_S3_BUCKET_NAME";
    private static final PathContext PATH = new PathContext.UserProfileContext(
            UUID.fromString("00000000-0000-0000-0000-000000000001")
    );

    @Autowired
    private S3Service s3Service;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager entityManager;

    @MockBean
    private S3Template s3Template;

    @Test
    @DisplayName("S3 업로드 이후 후속 작업이 실패하면 트랜잭션 롤백 후 업로드 객체를 삭제한다")
    void deleteUploadedObjectAfterTransactionRollback() {
        // given
        MockMultipartFile file = file("profile.png", "profile");
        AtomicReference<String> uploadedKey = new AtomicReference<>();

        // when
        Throwable thrown = catchThrowable(() -> transactionTemplate.executeWithoutResult(status -> {
            uploadedKey.set(s3Service.upload(file, PATH));
            throw new IllegalStateException("downstream database work failed");
        }));

        // then
        assertThat(thrown)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("downstream database work failed");
        assertThat(uploadedKey.get()).endsWith("_profile.png");
        verify(s3Template).upload(eq(BUCKET), eq(uploadedKey.get()), any(), any());
        verify(s3Template, timeout(3_000)).deleteObject(BUCKET, uploadedKey.get());
    }

    @Test
    @DisplayName("메서드 반환 후 JPA flush가 실패해도 트랜잭션 롤백 후 업로드 객체를 삭제한다")
    void deleteUploadedObjectAfterCommitTimeFlushFailure() {
        // given
        MockMultipartFile file = file("flush-failure.png", "profile");
        AtomicReference<String> uploadedKey = new AtomicReference<>();

        // when
        Throwable thrown = catchThrowable(() -> transactionTemplate.executeWithoutResult(status -> {
            uploadedKey.set(s3Service.upload(file, PATH));

            // Service로는 만들 수 없는 중복 이메일 상태를 의도적으로 구성해 커밋 시 flush 실패를 재현한다.
            entityManager.persist(member("first", "duplicate@test.com"));
            entityManager.persist(member("second", "duplicate@test.com"));
        }));

        // then
        assertThat(thrown)
                .isNotNull()
                .hasRootCauseInstanceOf(SQLIntegrityConstraintViolationException.class);
        assertThat(uploadedKey.get()).endsWith("_flush-failure.png");
        verify(s3Template).upload(eq(BUCKET), eq(uploadedKey.get()), any(), any());
        verify(s3Template, timeout(3_000)).deleteObject(BUCKET, uploadedKey.get());
    }

    @Test
    @DisplayName("S3 업로드 이후 DB 트랜잭션이 커밋되면 업로드 객체를 삭제하지 않는다")
    void keepUploadedObjectAfterTransactionCommit() {
        // given
        MockMultipartFile file = file("profile.png", "profile");

        // when
        String uploadedKey = transactionTemplate.execute(status -> s3Service.upload(file, PATH));

        // then
        assertThat(uploadedKey).endsWith("_profile.png");
        verify(s3Template).upload(eq(BUCKET), eq(uploadedKey), any(), any());
        verify(s3Template, after(500).never()).deleteObject(eq(BUCKET), any());
    }

    @Test
    @DisplayName("순차 업로드가 부분 실패하면 트랜잭션 롤백 전에는 삭제하지 않고 롤백 후 이전 성공 객체를 삭제한다")
    void deletePreviousSuccessfulObjectsOnlyAfterTransactionRollback() {
        // given
        MockMultipartFile success = file("success.png", "success");
        MockMultipartFile failure = file("failure.png", "failure");
        AtomicReference<Throwable> uploadFailure = new AtomicReference<>();
        doAnswer(invocation -> {
            String key = invocation.getArgument(1, String.class);
            if (key.endsWith("_failure.png")) {
                throw new RuntimeException("S3 upload failed");
            }
            return null;
        }).when(s3Template).upload(any(), any(), any(), any());

        // when
        transactionTemplate.executeWithoutResult(status -> {
            uploadFailure.set(catchThrowable(
                    () -> s3Service.uploadAll(List.of(success, failure), PATH)
            ));
            verify(s3Template, never()).deleteObject(eq(BUCKET), any());
            status.setRollbackOnly();
        });

        // then
        assertThat(uploadFailure.get())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("S3 upload failed");

        ArgumentCaptor<String> deletedKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(s3Template, timeout(3_000)).deleteObject(eq(BUCKET), deletedKeyCaptor.capture());
        assertThat(deletedKeyCaptor.getValue()).endsWith("_success.png");
        verify(s3Template, after(500).never())
                .deleteObject(eq(BUCKET), org.mockito.ArgumentMatchers.endsWith("_failure.png"));
    }

    @Test
    @DisplayName("사진 100장의 81번째 업로드가 실패하면 이후 요청을 중단하고 롤백 후 이전 성공 80장을 삭제한다")
    void stopAtEightyFirstFailureAndDeletePreviousEightyObjectsAfterRollback() {
        // given
        List<MultipartFile> files = IntStream.range(0, 100)
                .mapToObj(index -> index == 80
                        ? (MultipartFile) file("fail-80.png", "failure")
                        : (MultipartFile) file("success-" + index + ".png", "success"))
                .toList();
        doAnswer(invocation -> {
            String key = invocation.getArgument(1, String.class);
            if (key.contains("_fail-")) {
                throw new RuntimeException("S3 upload failed");
            }
            return null;
        }).when(s3Template).upload(any(), any(), any(), any());

        // when
        Throwable thrown = catchThrowable(() -> transactionTemplate.executeWithoutResult(
                status -> s3Service.uploadAll(files, PATH)
        ));

        // then
        assertThat(thrown)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("S3 upload failed");
        verify(s3Template, times(81)).upload(eq(BUCKET), any(), any(), any());

        ArgumentCaptor<String> deletedKeysCaptor = ArgumentCaptor.forClass(String.class);
        verify(s3Template, timeout(5_000).times(80))
                .deleteObject(eq(BUCKET), deletedKeysCaptor.capture());
        assertThat(deletedKeysCaptor.getAllValues())
                .hasSize(80)
                .doesNotHaveDuplicates()
                .allMatch(key -> key.contains("_success-"));
    }

    private MockMultipartFile file(String name, String content) {
        return new MockMultipartFile("file", name, "image/png", content.getBytes());
    }

    private Member member(String nickname, String email) {
        return Member.builder()
                .id(UUID.randomUUID())
                .nickName(nickname)
                .email(email)
                .password("encoded-password")
                .build();
    }
}
