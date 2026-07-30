package org.zerock.puppyrun.tracking.controller.response;

import java.util.List;
import java.util.UUID;
import org.zerock.puppyrun.tracking.entity.RoutePoint;
import org.zerock.puppyrun.tracking.entity.TrackingRoute;

/**
 * 사용자 현재 위치를 기준으로 추천된 산책 경로 목록입니다.
 *
 * @param routes 사용자 위치와 가까운 순서로 정렬된 추천 경로
 */
public record RecommendedRouteResponse(
        List<RecommendedRoute> routes
) {
    private static final double PATH_SIMPLIFICATION_TOLERANCE = 0.00005;

    /**
     * 조회된 경로 Entity를 지도 미리보기용 추천 응답으로 변환합니다.
     *
     * @param trackingRoutes 거리순으로 조회된 공개 경로
     * @return 추천 경로 응답
     */
    public static RecommendedRouteResponse from(List<TrackingRoute> trackingRoutes) {
        List<RecommendedRoute> routes = trackingRoutes.stream()
                .map(RecommendedRoute::from)
                .toList();
        return new RecommendedRouteResponse(routes);
    }

    /**
     * 추천 경로 한 건의 요약 정보입니다.
     *
     * @param trackingId 산책 기록 ID
     * @param path 지도 미리보기용 단순화 경로
     */
    public record RecommendedRoute(
            UUID trackingId,
            List<TrackingPoint> path
    ) {
        private static RecommendedRoute from(TrackingRoute trackingRoute) {
            List<TrackingPoint> path = trackingRoute
                    .getOptimizedPath(PATH_SIMPLIFICATION_TOLERANCE)
                    .stream()
                    .map(TrackingPoint::from)
                    .toList();

            return new RecommendedRoute(
                    trackingRoute.getTrackingId(),
                    path
            );
        }
    }

    /**
     * 지도 미리보기에 사용하는 경량 좌표입니다.
     *
     * @param lat 위도
     * @param lng 경도
     */
    public record TrackingPoint(
            Double lat,
            Double lng
    ) {
        private static TrackingPoint from(RoutePoint point) {
            return new TrackingPoint(point.lat(), point.lng());
        }
    }
}
