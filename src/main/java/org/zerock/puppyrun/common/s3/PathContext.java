package org.zerock.puppyrun.common.s3;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.zerock.puppyrun.common.s3.PathContext.DiaryPhotoContext;
import org.zerock.puppyrun.common.s3.PathContext.PetProfileContext;
import org.zerock.puppyrun.common.s3.PathContext.TrackingPhotoContext;
import org.zerock.puppyrun.common.s3.PathContext.UserProfileContext;

/**
 * S3 이미지 업로드 시, 각 도메인별로 표준화된 저장 경로(Path)를 생성하는 인터페이스입니다.
 */
public sealed interface PathContext
        permits DiaryPhotoContext, TrackingPhotoContext, PetProfileContext, UserProfileContext {

    // yyyy/MM/dd 형식을 공통으로 관리합니다.
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * 도메인별 규칙에 따라 생성된 S3 디렉토리 경로(Prefix)를 반환합니다.
     */
    String getPath();

    /**
     * [사용자 프로필] 저장 경로 컨텍스트 경로 형식: public/users/{userId}/profile
     */
    record UserProfileContext(UUID userId) implements PathContext {
        @Override
        public String getPath() {
            return String.format("public/users/%s/profile", userId);
        }
    }

    /**
     * [반려동물 프로필] 저장 경로 컨텍스트 경로 형식: public/pets/{petId}/profiles
     */
    record PetProfileContext(UUID petId) implements PathContext {
        @Override
        public String getPath() {
            return String.format("public/pets/%s/profiles", petId);
        }
    }

    /**
     * [다이어리 사진] 저장 경로 컨텍스트 경로 형식: diaries/{yyyy/MM/dd}/{diaryId}
     */
    record DiaryPhotoContext(UUID diaryId, LocalDate date) implements PathContext {
        @Override
        public String getPath() {
            String datePath = date.format(DATE_FORMATTER);
            return String.format("diaries/%s/%s", datePath, diaryId);
        }
    }

    /**
     * [산책 트래킹 사진] 저장 경로 컨텍스트 경로 형식: tracking/{yyyy/MM/dd}/{trackingId}
     */
    record TrackingPhotoContext(UUID trackingId, LocalDate date) implements PathContext {
        @Override
        public String getPath() {
            String datePath = date.format(DATE_FORMATTER);
            return String.format("tracking/%s/%s", datePath, trackingId);
        }
    }
}
