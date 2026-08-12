package org.zerock.puppyrun.statistics.controller.Response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.zerock.puppyrun.common.s3.support.S3Url;
import org.zerock.puppyrun.statistics.DTO.TodayPetActivityTracking;
import org.zerock.puppyrun.tracking.util.PaceConverter;

public record PetActivityResponse(
        List<PetActivity> activities
) {
    record PetActivity(
            UUID petId,                 // 강아지 고유 ID
            String petName,             // 강아지 이름
            @S3Url String PetProfileUrl, // 강아지 프로파일 이미지

            UUID trackingId,            // 산책 고유 ID (상세 페이지 이동용)
            LocalDateTime startedAt,    // 산책 시작 시간
            LocalDateTime endedAt,      // 산책 종료 시간
            Integer distanceM,          // 산책 거리 (m)
            Integer durationSec,        // 산책 시간 (초)
            String averagePace          // 산책 페이스

    ) {
        public static PetActivity from(TodayPetActivityTracking activityTrackings) {
            return new PetActivity(
                    activityTrackings.petId(),
                    activityTrackings.petName(),
                    activityTrackings.PetProfileUrl(),
                    activityTrackings.trackingId(),
                    activityTrackings.startedAt(),
                    activityTrackings.endedAt(),
                    activityTrackings.distance(),
                    activityTrackings.duration(),
                    PaceConverter.toString(activityTrackings.averagePace())
            );
        }
    }

    public static PetActivityResponse of(List<TodayPetActivityTracking> activityTrackings) {
        // 리스트가 비었거나 null 일경우
        if (Objects.isNull(activityTrackings) || activityTrackings.isEmpty()) {
            return new PetActivityResponse(List.of());
        }

        List<PetActivity> activityList = activityTrackings.stream()
                .map(PetActivity::from)
                .toList();

        return new PetActivityResponse(activityList);
    }

}
