package org.zerock.puppyrun.tracking.controller.response;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.tracking.entity.Tracking;
import org.zerock.puppyrun.tracking.entity.TrackingPath;
import org.zerock.puppyrun.tracking.util.PaceConverter;

@Builder
public record MainTrackingResponse(
        List<TrackingDetail> trackingList
) {

    /**
     * Tracking 리스트를 MainTrackingResponse로 변환
     */
    public static MainTrackingResponse from(List<Tracking> trackingList) {
        List<TrackingDetail> details = trackingList.stream()
                .map(TrackingDetail::from)
                .toList();

        return new MainTrackingResponse(details);
    }

    @Builder
    public record TrackingDetail(
            UUID id,                     // 산책 고유 아이디
            LocalDateTime startedAt,     // 산책 시작 시간
            LocalDateTime endedAt,       // 산책 종료 시간
            Integer duration,            // 산책 진행 시간
            String visibility,           // 공개 여부
            Integer distance,            // 이동 거리
            String averagePace,          // 평균 속도
            List<TrackingPoint> path     // 이동 경로 리스트
    ) {
        /**
         * Tracking 엔티티를 TrackingDetail DTO로 변환
         */
        public static TrackingDetail from(Tracking tracking) {
            return TrackingDetail.builder()
                    .id(tracking.getId())
                    .startedAt(tracking.getStartedAt())
                    .endedAt(tracking.getEndedAt())
                    .duration(tracking.getDuration())
                    .visibility(tracking.getVisibility().name())
                    .distance(tracking.getDistance())
                    .averagePace(PaceConverter.toString(tracking.getAveragePace()))
                    .path(TrackingPoint.listOf(tracking.getPath()))
                    .build();
        }
    }

    @Builder
    public record TrackingPoint(
            Double lat,   // 위도
            Double lng,   // 경도
            Integer time  // 경과 시간
    ) {
        /**
         * TrackingPath 엔티티 리스트를 TrackingPoint DTO 리스트로 변환
         */
        public static List<TrackingPoint> listOf(List<TrackingPath> paths) {
            return Optional.ofNullable(paths)
                    .orElseGet(Collections::emptyList)
                    .stream()
                    .map(TrackingPoint::from)
                    .toList();
        }

        /**
         * 단일 TrackingPath 변환
         */
        private static TrackingPoint from(TrackingPath path) {
            return TrackingPoint.builder()
                    .lat(path.getLat())
                    .lng(path.getLng())
                    .time(path.getTime())
                    .build();
        }
    }
}
