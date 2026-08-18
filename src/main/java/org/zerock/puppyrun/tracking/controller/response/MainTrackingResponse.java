package org.zerock.puppyrun.tracking.controller.response;

import java.util.List;
import java.util.UUID;
import org.zerock.puppyrun.common.pagination.SliceResult;
import org.zerock.puppyrun.common.s3.support.S3Url;
import org.zerock.puppyrun.tracking.DTO.MainTrackingSummary;
import org.zerock.puppyrun.tracking.entity.RoutePoint;

public record MainTrackingResponse(
        List<TrackingDetail> trackingList,
        boolean hasNext,
        int pageSize
) {

    /**
     * 산책 목록 조회 결과를 API 응답으로 변환합니다.
     */
    public static MainTrackingResponse from(SliceResult<MainTrackingSummary> summaries) {
        List<TrackingDetail> details = summaries.content().stream()
                .map(TrackingDetail::from)
                .toList();

        return new MainTrackingResponse(details, summaries.hasNext(), details.size());
    }

    public record TrackingDetail(
            UUID id,                     // 산책 고유 아이디
            @S3Url String featuredImage, // 대표 이미지
            List<TrackingPoint> path     // 산책 경로
    ) {

        private static TrackingDetail from(MainTrackingSummary summary) {
            return new TrackingDetail(
                    summary.trackingId(),
                    summary.featuredImage(),
                    summary.path().stream()
                            .map(TrackingPoint::from)
                            .toList()
            );
        }
    }

    public record TrackingPoint(
            Double lat,
            Double lng,
            Integer time
    ) {

        private static TrackingPoint from(RoutePoint point) {
            return new TrackingPoint(point.lat(), point.lng(), point.time());
        }
    }
}
