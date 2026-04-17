package org.zerock.puppyrun.common.s3;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.zerock.puppyrun.common.exception.DataIntegrityException;
import org.zerock.puppyrun.common.s3.rollback.S3UploadRollback;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    /**
     * 파일 업로드
     */
    @S3UploadRollback
    public String upload(MultipartFile file, PathContext path) {
        validateFile(file);

        // Key만 추출
        String key = generateKey(file, path);

        // 업로드 실행
        return uploadToS3(file, key);
    }

    /**
     * 파일 삭제
     *
     * @param fileData DB에 저장된 Key 또는 만약의 경우를 대비한 Full URL
     */
    @Async
    public void delete(String fileData) {
        if (fileData == null || fileData.isBlank()) {
            log.warn("삭제하려는 S3 파일 경로가 비어있습니다.");
            return;
        }

        // URL이 들어와도, Key가 들어와도 안전하게 Key만 추출
        String key = extractKey(fileData);

        try {
            if (!s3Template.objectExists(bucket, key)) {
                log.warn("삭제 시도 실패: S3에 파일이 존재하지 않습니다. Key: {}", key);
                return;
            }

            s3Template.deleteObject(bucket, key);
            log.info("S3 파일 삭제 성공: {}", key);

        } catch (Exception e) {
            log.error("S3 파일 삭제 중 오류 발생 - Key: {}, 메시지: {}", key, e.getMessage());
        }
    }

    private String uploadToS3(MultipartFile file, String key) {
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = ObjectMetadata.builder()
                    .contentType(file.getContentType())
                    .build();

            // S3Template으로 업로드
            s3Template.upload(bucket, key, inputStream, metadata);

            log.info("S3 파일 업로드 성공 (Key): {}", key);
            return key;

        } catch (IOException e) {
            log.error("S3 업로드 중 실패 - Key: {}, 에러: {}", key, e.getMessage());
            throw new RuntimeException("S3 업로드 중 오류 발생", e);
        }
    }

    private String generateKey(MultipartFile file, PathContext path) {
        String folderPath = path.getPath();
        String originalName = file.getOriginalFilename();

        String savedName = UUID.randomUUID() + "_" +
                (originalName != null ? originalName.replaceAll("\\s", "_") : "unnamed");

        return String.format("%s/%s/%s", activeProfile, folderPath, savedName);
    }

    private String extractKey(String fileData) {
        try {
            // URL 형태라면 Key 부분만 잘라냄
            if (fileData.contains(bucket)) {
                String key = fileData.substring(fileData.lastIndexOf(bucket) + bucket.length() + 1);
                return URLDecoder.decode(key, StandardCharsets.UTF_8);
            }
            // 이미 Key 형태라면 디코딩만 수행
            return URLDecoder.decode(fileData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return fileData;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DataIntegrityException("업로드할 파일이 비어있습니다.");
        }
    }
}
