package org.zerock.puppyrun.s3;

import io.awspring.cloud.s3.S3Template;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.puppyrun.common.exception.DataIntegrityException;
import org.zerock.puppyrun.common.s3.PathContext;
import org.zerock.puppyrun.common.s3.S3Service;
import org.zerock.puppyrun.common.s3.rollback.S3RollbackEvent;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Template s3Template;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private S3Service s3Service;

    private final PathContext dummyPath = new PathContext.UserProfileContext(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        // @Value 필드 수동 주입
        ReflectionTestUtils.setField(s3Service, "bucket", "test-bucket");
        ReflectionTestUtils.setField(s3Service, "activeProfile", "local");
    }

    @Nested
    @DisplayName("파일 업로드 테스트")
    class UploadTest {

        @Test
        @DisplayName("파일이 null이면 에러 없이 null을 반환한다 (Skip)")
        void upload_null_skip() {
            String result = s3Service.upload(null, dummyPath);
            assertThat(result).isNull();
            verifyNoInteractions(s3Template, eventPublisher);
        }

        @Test
        @DisplayName("파일 객체는 존재하지만 내용이 비어있으면 null을 반환한다 (Skip)")
        void upload_empty_file_skip() {
            MockMultipartFile emptyFile = new MockMultipartFile("file", "test.txt", "text/plain", new byte[0]);

            String result = s3Service.upload(emptyFile, dummyPath);

            assertThat(result).isNull();
            verifyNoInteractions(s3Template, eventPublisher);
        }

        @Test
        @DisplayName("정상적인 파일 업로드 시 S3에 업로드하고 롤백 이벤트를 발행한다")
        void upload_success() {
            MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "test-content".getBytes());

            String result = s3Service.upload(file, dummyPath);

            assertThat(result).isNotNull();
            verify(s3Template, times(1)).upload(any(), any(), any(), any());
            verify(eventPublisher, times(1)).publishEvent(any(S3RollbackEvent.class));
        }
    }

    @Nested
    @DisplayName("다중 파일 업로드 테스트")
    class UploadAllTest {

        @Test
        @DisplayName("리스트가 null이거나 비어있으면 빈 리스트를 반환한다")
        void uploadAll_null_or_empty_returns_empty_list() {
            assertThat(s3Service.uploadAll(null, dummyPath)).isEmpty();
            assertThat(s3Service.uploadAll(List.of(), dummyPath)).isEmpty();
            verifyNoInteractions(s3Template, eventPublisher);
        }

        @Test
        @DisplayName("리스트 내 null 요소는 무시하고 정상 파일만 처리한다")
        void uploadAll_filters_null_elements() {
            MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "test".getBytes());
            List<MultipartFile> input = Arrays.asList(file, null);

            List<String> results = s3Service.uploadAll(input, dummyPath);

            assertThat(results).hasSize(1);
            verify(s3Template, times(1)).upload(any(), any(), any(), any());
            verify(eventPublisher, times(1)).publishEvent(any(S3RollbackEvent.class));
        }

        @Test
        @DisplayName("리스트 내에 비어있는 파일이 섞여있으면 비어있는 파일만 제외하고 업로드한다")
        void uploadAll_with_empty_file_skips_empty_only() {
            MockMultipartFile validFile = new MockMultipartFile("file", "test.png", "image/png", "test".getBytes());
            MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);
            List<MultipartFile> input = List.of(validFile, emptyFile);

            List<String> results = s3Service.uploadAll(input, dummyPath);

            assertThat(results).hasSize(1); // 정상 파일 1개만 성공
            verify(s3Template, times(1)).upload(any(), any(), any(), any());
            verify(eventPublisher, times(1)).publishEvent(any(S3RollbackEvent.class));
        }
    }

    @Nested
    @DisplayName("파일 삭제 테스트")
    class DeleteTest {

        @Test
        @DisplayName("삭제 경로가 null이면 아무 작업도 하지 않는다 (Skip)")
        void delete_null_skip() {
            s3Service.delete(null);
            verifyNoInteractions(s3Template);
        }

        @Test
        @DisplayName("삭제 경로가 빈 문자열(blank)이면 DataIntegrityException이 발생한다")
        void delete_blank_throws_exception() {
            assertThatThrownBy(() -> s3Service.delete(""))
                    .isExactlyInstanceOf(DataIntegrityException.class);
        }

        @Test
        @DisplayName("여러 파일 삭제 시 null 요소는 안전하게 무시한다")
        void deleteAll_filters_null_elements() {
            List<String> input = Arrays.asList("path1", null, "path2");

            s3Service.deleteAll(input);

            verify(s3Template, times(2)).deleteObject(any(), any());
        }
    }
}
