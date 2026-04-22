package org.zerock.puppyrun.diary.controller.response;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.common.s3.support.S3Url;
import org.zerock.puppyrun.diary.entity.Diary;
import org.zerock.puppyrun.tracking.entity.Tracking;
import org.zerock.puppyrun.tracking.entity.RoutePoint;

/**
 * 다이어리 상세 조회 응답 DTO
 */
@Builder
public record DiaryResponse(
        UUID DiaryId,
        UUID TrackingId,
        LocalDateTime writingTime,
        String title,
        String content,
        Weather weather,
        TrackingDetail trackingDetail,
        @S3Url
        List<String> images
) {

    /**
     * Diary 엔티티를 DiaryResponse로 변환하는 정적 팩토리 메서드
     */
    public static DiaryResponse of(Diary diary, List<RoutePoint> path) {
        Tracking tracking = diary.getTracking();
        return DiaryResponse.builder()
                .DiaryId(diary.getId())
                .TrackingId(getTrackingId(tracking))
                .writingTime(diary.getWritingTime())
                .title(diary.getTitle())
                .content(diary.getContent())
                .weather(Weather.from(diary)) // 날씨 변환 위임
                .trackingDetail(TrackingDetail.of(tracking, path)) // 산책 정보 변환 위임
                .images(diary.getImages())
                .build();
    }

    // Null Safe하게 Tracking ID 추출
    private static UUID getTrackingId(Tracking tracking) {
        return (tracking != null) ? tracking.getId() : null;
    }

    /**
     * 날씨 상세 정보
     */
    @Builder
    public record Weather(
            String temp,
            String sky,
            String pty
    ) {
        public static Weather from(Diary diary) {
            return Weather.builder()
                    .temp(diary.getTemp())
                    .sky(diary.getSky().getCode())
                    .pty(diary.getPty().getCode())
                    .build();
        }
    }

    /**
     * 산책 상세 정보
     */
    @Builder
    public record TrackingDetail(
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            Integer duration,
            String visibility, Integer distance,
            List<TrackingPoint> path
    ) {
        public static TrackingDetail of(Tracking tracking, List<RoutePoint> path) {
            if (tracking == null) {
                return null;
            }
            return TrackingDetail.builder()
                    .startedAt(tracking.getStartedAt())
                    .endedAt(tracking.getEndedAt())
                    .duration(tracking.getDuration())
                    .visibility(tracking.getVisibility().name())
                    .distance(tracking.getDistance())
                    .path(TrackingPoint.listOf(path)) // 경로 변환 위임
                    .build();
        }
    }

    /**
     * 산책 경로 포인트
     */
    @Builder
    public record TrackingPoint(
            Double lat,
            Double lng,
            Integer time
    ) {
        // 리스트 변환 헬퍼 메서드
        public static List<TrackingPoint> listOf(List<RoutePoint> paths) {
            return Optional.ofNullable(paths)
                    .orElseGet(Collections::emptyList)
                    .stream()
                    .map(TrackingPoint::from)
                    .toList();
        }

        private static TrackingPoint from(RoutePoint path) {
            return TrackingPoint.builder()
                    .lat(path.lat())
                    .lng(path.lng())
                    .time(path.time())
                    .build();
        }
    }
}
