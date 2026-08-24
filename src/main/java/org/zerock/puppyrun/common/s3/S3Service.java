package org.zerock.puppyrun.common.s3;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.zerock.puppyrun.common.exception.DataIntegrityException;
import org.zerock.puppyrun.common.s3.rollback.S3RollbackEvent;

/**
 * AWS S3 파일 업로드 및 삭제를 담당하는 인프라 서비스입니다. 다중 업로드는 성공한 객체를 롤백 보상 이벤트에 등록해 현재 DB 트랜잭션이 실패할 때 정리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Template s3Template;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * 단일 파일을 S3에 업로드합니다.
     *
     * @param file 업로드할 멀티파트 파일
     * @param path 저장될 도메인 경로(PathContext)
     * @return S3에 저장된 최종 Key (경로 포함)
     */
    public String upload(MultipartFile file, PathContext path) {
        if (file == null || file.isEmpty()) {
            return null; // 이미지가 null이거나 비어있으면 넘어감 (skip)
        }

        String key = uploadOne(file, path);
        publishRollbackEvent(List.of(key));

        return key;
    }

    /**
     * 여러 파일을 순차적으로 S3에 업로드합니다.
     *
     * @param files 업로드할 멀티파트 파일 리스트
     * @param path  저장될 도메인 경로(PathContext)
     * @return 업로드 완료된 S3 Key 리스트
     */
    public List<String> uploadAll(List<MultipartFile> files, PathContext path) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        // null이거나 비어있는 파일은 "첨부하지 않은 것"으로 간주하여 필터링 (skip)
        List<MultipartFile> validFiles = files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();

        if (validFiles.isEmpty()) {
            return List.of();
        }

        List<String> uploadedKeys = new ArrayList<>(validFiles.size());
        try {
            for (MultipartFile file : validFiles) {
                uploadedKeys.add(uploadOne(file, path));
            }
        } catch (RuntimeException e) {
            // 첫 실패 이전까지 성공한 객체를 현재 트랜잭션의 롤백 대상으로 등록
            publishRollbackEvent(uploadedKeys);
            log.warn("순차 S3 업로드가 중단되었습니다. 성공: {}건, 롤백 대상: {}건",
                    uploadedKeys.size(), uploadedKeys.size(), e);
            throw e;
        }

        // 모두 성공한 경우에도 이후 DB 트랜잭션 실패에 대비해 통합 롤백 이벤트 발행
        publishRollbackEvent(uploadedKeys);

        return List.copyOf(uploadedKeys);
    }

    private String uploadOne(MultipartFile file, PathContext path) {
        String key = generateKey(file, path);
        return uploadToS3(file, key);
    }

    private void publishRollbackEvent(List<String> uploadedKeys) {
        if (!uploadedKeys.isEmpty()) {
            eventPublisher.publishEvent(new S3RollbackEvent(uploadedKeys));
        }
    }

    /**
     * 여러 파일을 S3에서 비동기로 삭제합니다.
     *
     * @param files 삭제할 파일들의 Key 또는 Full URL 리스트
     */
    @Async("applicationTaskExecutor")
    public void deleteAll(List<String> files) {
        if (files == null || files.isEmpty()) {
            return;
        }

        deleteFiles(files);
    }

    /**
     * 단일 파일을 S3에서 비동기로 삭제합니다.
     *
     * @param file 삭제할 파일의 Key 또는 Full URL
     */
    @Async("applicationTaskExecutor")
    public void delete(String file) {
        if (file == null) {
            return; // null이면 넘어감
        }
        deleteFiles(List.of(file));
    }

    private void deleteFiles(List<String> files) {
        files.stream()
                .filter(Objects::nonNull)
                .forEach(this::deleteToS3);
    }

    /**
     * 실제 S3 삭제 로직을 수행합니다.
     */
    private void deleteToS3(String file) {
        if (file == null || file.isBlank()) {
            throw new DataIntegrityException("삭제할 파일 경로가 비어있습니다.");
        }

        // URL이 들어와도, Key가 들어와도 안전하게 Key만 추출
        String key = extractKey(file);

        try {
            s3Template.deleteObject(bucket, key);
            log.info("S3 파일 삭제 성공: {}", key);

        } catch (Exception e) {
            log.error("S3 파일 삭제 중 오류 발생 - Key: {}, 메시지: {}", key, e.getMessage());
        }
    }

    /**
     * S3Template을 사용하여 물리적인 파일 전송을 수행합니다.
     */
    private String uploadToS3(MultipartFile file, String key) {
        // 이 시점에는 이미 위에서 검증되었으나, 마지막 방어선으로 유지
        if (file == null || file.isEmpty()) {
            throw new DataIntegrityException("업로드할 파일이 존재하지 않거나 비어있습니다.");
        }

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

    /**
     * 프로필, 도메인 경로, UUID를 조합하여 S3 내의 유니크한 저장 경로를 생성합니다.
     */
    private String generateKey(MultipartFile file, PathContext path) {
        String folderPath = path.getPath();
        String originalName = file.getOriginalFilename();

        String savedName = UUID.randomUUID() + "_" +
                (originalName != null ? originalName.replaceAll("\\s", "_") : "unnamed");

        // 예: local/profile/uuid_name.png
        return String.format("%s/%s/%s", activeProfile, folderPath, savedName);
    }

    /**
     * Full URL이 들어오는 경우를 대비하여 순수 S3 Object Key만 추출합니다.
     */
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
}
