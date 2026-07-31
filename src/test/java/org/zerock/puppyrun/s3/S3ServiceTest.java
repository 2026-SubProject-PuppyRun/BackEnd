package org.zerock.puppyrun.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.awspring.cloud.s3.S3Template;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.puppyrun.common.s3.PathContext;
import org.zerock.puppyrun.common.s3.S3Service;
import org.zerock.puppyrun.common.s3.rollback.S3RollbackEvent;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3 파일 서비스")
class S3ServiceTest {

    @Mock
    private S3Template s3Template;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private S3Service s3Service;

    private final PathContext path = new PathContext.UserProfileContext(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3Service, "bucket", "test-bucket");
        ReflectionTestUtils.setField(s3Service, "activeProfile", "local");
    }

    @Test
    @DisplayName("단일 파일을 업로드하고 트랜잭션 보상 대상으로 등록한다")
    void uploadFileAndRegisterRollback() {
        // given
        MockMultipartFile file = file("profile.png", "profile");

        // when
        String uploadedKey = s3Service.upload(file, path);

        // then
        assertThat(uploadedKey)
                .startsWith("local/" + path.getPath())
                .endsWith("_profile.png");
        verify(s3Template).upload(eq("test-bucket"), eq(uploadedKey), any(), any());

        ArgumentCaptor<S3RollbackEvent> eventCaptor = ArgumentCaptor.forClass(S3RollbackEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().filePaths()).containsExactly(uploadedKey);
    }

    @Test
    @DisplayName("여러 파일 중 첨부된 파일만 업로드하고 하나의 보상 이벤트로 묶는다")
    void uploadAttachedFilesTogether() {
        // given
        MockMultipartFile first = file("first.png", "first");
        MockMultipartFile second = file("second.png", "second");
        MockMultipartFile empty = file("empty.png", "");
        List<MultipartFile> files = Arrays.asList(first, null, empty, second);

        // when
        List<String> uploadedKeys = s3Service.uploadAll(files, path);

        // then
        assertThat(uploadedKeys)
                .hasSize(2)
                .allSatisfy(key -> assertThat(key).startsWith("local/" + path.getPath()));
        verify(s3Template, times(2)).upload(eq("test-bucket"), any(), any(), any());

        ArgumentCaptor<S3RollbackEvent> eventCaptor = ArgumentCaptor.forClass(S3RollbackEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().filePaths()).containsExactlyElementsOf(uploadedKeys);
    }

    @Test
    @DisplayName("다중 업로드가 일부 실패하면 성공한 파일을 보상 대상으로 남긴다")
    void registerSuccessfulFilesForRollbackOnPartialFailure() {
        // given
        MockMultipartFile success = file("success.png", "success");
        MockMultipartFile failure = file("failure.png", "failure");
        doAnswer(invocation -> {
            String key = invocation.getArgument(1, String.class);
            if (key.endsWith("_failure.png")) {
                throw new RuntimeException("S3 upload failed");
            }
            return null;
        }).when(s3Template).upload(any(), any(), any(), any());

        // when
        Throwable thrown = catchThrowable(
                () -> s3Service.uploadAll(List.of(success, failure), path)
        );

        // then
        assertThat(thrown)
                .isInstanceOf(CompletionException.class)
                .hasRootCauseMessage("S3 upload failed");

        ArgumentCaptor<S3RollbackEvent> eventCaptor = ArgumentCaptor.forClass(S3RollbackEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().filePaths())
                .singleElement()
                .asString()
                .endsWith("_success.png");
    }

    @Test
    @DisplayName("키와 URL이 섞인 삭제 요청을 S3 객체 키로 정규화해 처리한다")
    void deleteFilesByKeyOrUrl() {
        // given
        List<String> files = Arrays.asList(
                "local/profile/first.png",
                null,
                "https://s3.ap-northeast-2.amazonaws.com/test-bucket/local/profile/second%20file.png"
        );

        // when
        s3Service.deleteAll(files);

        // then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(s3Template, times(2)).deleteObject(eq("test-bucket"), keyCaptor.capture());
        assertThat(keyCaptor.getAllValues())
                .containsExactly(
                        "local/profile/first.png",
                        "local/profile/second file.png"
                );
    }

    private MockMultipartFile file(String name, String content) {
        return new MockMultipartFile("file", name, "image/png", content.getBytes());
    }
}
