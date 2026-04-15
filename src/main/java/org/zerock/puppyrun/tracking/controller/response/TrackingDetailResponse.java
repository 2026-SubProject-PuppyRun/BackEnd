package org.zerock.puppyrun.tracking.controller.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.tracking.util.PaceConverter;
import org.zerock.puppyrun.tracking.entity.Tracking;

@Builder
public record TrackingDetailResponse(
        UUID id,                     // 산책 고유 아이디
        UUID diaryId,                // 일기 고유 아이디
        LocalDateTime startedAt,     // 산책 시작 시간
        LocalDateTime endedAt,       // 산책 종료 시간
        Integer duration,            // 산책 진행 시간
        String visibility,           // 공개 여부
        Integer distance,            // 이동 거리
        List<String> images,          // 이미지 리스트
        String averagePace,              // 평균 속도
        List<TrackingPoint> path     // 이동 경로 리스트
) {

    @Builder
    public record TrackingPoint(
            Double lat,   // 위도
            Double lng,   // 경도
            Integer time  // 경과 시간
    ) {
    }

    public static TrackingDetailResponse of(Tracking tracking, UUID diaryId) {
        List<TrackingPoint> pathPoints = tracking.getPath().stream()
                .map(p -> new TrackingPoint(p.getLat(), p.getLng(), p.getTime()))
                .toList();

        return TrackingDetailResponse.builder()
                .id(tracking.getId())
                .diaryId(diaryId)
                .startedAt(tracking.getStartedAt())
                .endedAt(tracking.getEndedAt())
                .duration(tracking.getDuration())
                .visibility(tracking.getVisibility().name())
                .distance(tracking.getDistance())
                .averagePace(PaceConverter.toString(tracking.getAveragePace()))
                .path(pathPoints)
                .build();
    }
}
