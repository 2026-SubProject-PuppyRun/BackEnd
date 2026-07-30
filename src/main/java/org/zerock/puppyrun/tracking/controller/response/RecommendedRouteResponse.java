package org.zerock.puppyrun.tracking.controller.response;

import java.util.List;
import java.util.UUID;
import org.zerock.puppyrun.tracking.entity.RoutePoint;
import org.zerock.puppyrun.tracking.entity.Tracking;
import org.zerock.puppyrun.tracking.entity.TrackingRoute;
import org.zerock.puppyrun.tracking.util.PaceConverter;

/**
 * 사용자 현재 위치를 기준으로 추천된 산책 경로 목록입니다.
 *
 * @param routes 사용자 위치와 가까운 순서로 정렬된 추천 경로
 */
public record RecommendedRouteResponse(
        List<RecommendedRoute> routes
) {
    private static final double PATH_SIMPLIFICATION_TOLERANCE = 0.00005;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    /**
     * 조회된 경로 Entity를 지도 미리보기용 추천 응답으로 변환합니다.
     *
     * @param trackingRoutes 거리순으로 조회된 공개 경로
     * @param userLatitude 사용자 현재 위도
     * @param userLongitude 사용자 현재 경도
     * @return 추천 경로 응답
     */
    public static RecommendedRouteResponse from(
            List<TrackingRoute> trackingRoutes,
            double userLatitude,
            double userLongitude
    ) {
        List<RecommendedRoute> routes = trackingRoutes.stream()
                .map(route -> RecommendedRoute.from(route, userLatitude, userLongitude))
                .toList();
        return new RecommendedRouteResponse(routes);
    }

    /**
     * 추천 경로 한 건의 요약 정보입니다.
     *
     * @param trackingId 산책 기록 ID
     * @param distanceFromUserMeters 사용자 위치에서 경로 시작점까지의 거리(m)
     * @param routeDistanceMeters 기록된 전체 산책 거리(m)
     * @param durationSeconds 기록된 산책 시간(초)
     * @param averagePace 평균 페이스 문자열
     * @param path 지도 미리보기용 단순화 경로
     */
    public record RecommendedRoute(
            UUID trackingId,
            long distanceFromUserMeters,
            Integer routeDistanceMeters,
            Integer durationSeconds,
            String averagePace,
            List<TrackingPoint> path
    ) {
        private static RecommendedRoute from(
                TrackingRoute trackingRoute,
                double userLatitude,
                double userLongitude
        ) {
            Tracking tracking = trackingRoute.getTracking();
            double startLongitude = trackingRoute.getStartPoint().getX();
            double startLatitude = trackingRoute.getStartPoint().getY();
            List<TrackingPoint> path = trackingRoute
                    .getOptimizedPath(PATH_SIMPLIFICATION_TOLERANCE)
                    .stream()
                    .map(TrackingPoint::from)
                    .toList();

            return new RecommendedRoute(
                    tracking.getId(),
                    Math.round(distanceMeters(userLatitude, userLongitude, startLatitude, startLongitude)),
                    tracking.getDistance(),
                    tracking.getDuration(),
                    PaceConverter.toString(tracking.getAveragePace()),
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

    private static double distanceMeters(
            double firstLatitude,
            double firstLongitude,
            double secondLatitude,
            double secondLongitude
    ) {
        double latitudeDistance = Math.toRadians(secondLatitude - firstLatitude);
        double longitudeDistance = Math.toRadians(secondLongitude - firstLongitude);
        double firstLatitudeRadians = Math.toRadians(firstLatitude);
        double secondLatitudeRadians = Math.toRadians(secondLatitude);

        double haversine = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2)
                + Math.cos(firstLatitudeRadians) * Math.cos(secondLatitudeRadians)
                * Math.sin(longitudeDistance / 2) * Math.sin(longitudeDistance / 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(haversine));
    }
}
