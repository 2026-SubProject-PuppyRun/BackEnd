package org.zerock.puppyrun.tracking.controller.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.zerock.puppyrun.tracking.entity.Tracking;
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
            String averagePace          // 평균 속도
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
                    .build();
        }
    }
}
