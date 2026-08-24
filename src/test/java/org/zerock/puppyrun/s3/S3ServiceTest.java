package org.zerock.puppyrun.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.awspring.cloud.s3.S3Template;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
    @DisplayName("순차 업로드가 일부 실패하면 이전에 성공한 파일을 롤백 이벤트에 등록한다")
    void registerPreviousSuccessfulFilesOnUploadFailure() {
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
                .isInstanceOf(RuntimeException.class)
                .hasMessage("S3 upload failed");
        verify(s3Template, times(2)).upload(eq("test-bucket"), any(), any(), any());

        verify(s3Template, never()).deleteObject(eq("test-bucket"), any());
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

    @Test
    @DisplayName("파일명이 같은 사진 100장을 모두 업로드하고 중복 없는 Key 100개를 보상 대상으로 등록한다")
    void uploadOneHundredFilesTogether() {
        // given
        List<MultipartFile> files = IntStream.range(0, 100)
                .mapToObj(index -> (MultipartFile) file("same-name.png", "image-" + index))
                .toList();

        // when
        List<String> uploadedKeys = s3Service.uploadAll(files, path);

        // then
        assertThat(uploadedKeys)
                .hasSize(100)
                .doesNotHaveDuplicates()
                .allSatisfy(key -> assertThat(key)
                        .startsWith("local/" + path.getPath())
                        .endsWith("_same-name.png"));
        verify(s3Template, times(100)).upload(eq("test-bucket"), any(), any(), any());

        ArgumentCaptor<S3RollbackEvent> eventCaptor = ArgumentCaptor.forClass(S3RollbackEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().filePaths())
                .hasSize(100)
                .containsExactlyElementsOf(uploadedKeys);
    }

    @Test
    @DisplayName("사진 입력 100개에 정상·빈 파일·null이 섞이면 정상 파일 60개만 업로드한다")
    void uploadOnlyValidFilesFromOneHundredMixedInputs() {
        // given
        List<MultipartFile> files = new ArrayList<>();
        for (int index = 0; index < 100; index++) {
            if (index % 5 == 0) {
                files.add(null);
            } else if (index % 5 == 1) {
                files.add(file("empty-" + index + ".png", ""));
            } else {
                files.add(file("photo-" + index + ".png", "image-" + index));
            }
        }

        // when
        List<String> uploadedKeys = s3Service.uploadAll(files, path);

        // then
        assertThat(uploadedKeys).hasSize(60).doesNotHaveDuplicates();
        verify(s3Template, times(60)).upload(eq("test-bucket"), any(), any(), any());

        ArgumentCaptor<S3RollbackEvent> eventCaptor = ArgumentCaptor.forClass(S3RollbackEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().filePaths()).hasSize(60);
    }

    @ParameterizedTest(name = "100장 중 {0}번 인덱스에서 첫 실패")
    @ValueSource(ints = {1, 10, 50, 99})
    @DisplayName("사진 100장의 실패 위치가 달라도 실패 전 성공한 Key만 롤백 이벤트에 등록한다")
    void stopAtFirstFailureAndRegisterPreviousSuccessfulKeys(int failureIndex) {
        // given
        List<MultipartFile> files = IntStream.range(0, 100)
                .mapToObj(index -> index == failureIndex
                        ? (MultipartFile) file("fail-" + index + ".png", "failure")
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
        Throwable thrown = catchThrowable(() -> s3Service.uploadAll(files, path));

        // then
        assertThat(thrown)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("S3 upload failed");
        verify(s3Template, times(failureIndex + 1))
                .upload(eq("test-bucket"), any(), any(), any());

        verify(s3Template, never()).deleteObject(eq("test-bucket"), any());
        ArgumentCaptor<S3RollbackEvent> eventCaptor = ArgumentCaptor.forClass(S3RollbackEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().filePaths())
                .hasSize(failureIndex)
                .allMatch(key -> key.contains("_success-"));
    }

    @Test
    @DisplayName("사진 100장의 첫 번째 업로드가 실패하면 나머지 99장을 시도하지 않고 이벤트도 발행하지 않는다")
    void stopWithoutEventWhenFirstOfOneHundredFilesFails() {
        // given
        List<MultipartFile> files = IntStream.range(0, 100)
                .mapToObj(index -> (MultipartFile) file("fail-" + index + ".png", "failure"))
                .toList();
        doAnswer(invocation -> {
            throw new RuntimeException("S3 upload failed");
        }).when(s3Template).upload(any(), any(), any(), any());

        // when
        Throwable thrown = catchThrowable(() -> s3Service.uploadAll(files, path));

        // then
        assertThat(thrown)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("S3 upload failed");
        verify(s3Template).upload(eq("test-bucket"), any(), any(), any());
        verify(s3Template, never()).deleteObject(eq("test-bucket"), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    private MockMultipartFile file(String name, String content) {
        return new MockMultipartFile("file", name, "image/png", content.getBytes());
    }
}
