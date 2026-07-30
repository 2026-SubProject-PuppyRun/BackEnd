package org.zerock.puppyrun.tracking.recommendation.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.tracking.entity.TrackingRoute;
import org.zerock.puppyrun.tracking.recommendation.controller.response.RecommendedRouteResponse;
import org.zerock.puppyrun.tracking.recommendation.repository.TrackingRecommendationRepository;

/**
 * 사용자 위치를 기준으로 공개 산책 경로를 추천합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackingRecommendationService {
    private static final double METERS_PER_LATITUDE_DEGREE = 111_320.0;
    private static final double MINIMUM_LONGITUDE_SCALE = 0.01;

    private final TrackingRecommendationRepository trackingRecommendationRepository;

    /**
     * 사용자 위치 주변에서 시작하는 공개 산책 경로를 가까운 순서로 추천합니다.
     *
     * <p>검색 반경을 포함하는 사각 영역으로 공간 인덱스 후보를 만들고, Repository에서 정확한
     * 구면 거리와 공개 여부를 적용합니다.</p>
     *
     * @param latitude 사용자 현재 위도
     * @param longitude 사용자 현재 경도
     * @param radiusMeters 추천 경로 검색 반경(m)
     * @param limit 반환할 최대 추천 개수
     * @return 거리순 추천 경로 응답
     */
    public RecommendedRouteResponse getRecommendedRoutes(
            double latitude,
            double longitude,
            int radiusMeters,
            int limit
    ) {
        String pointWkt = "POINT(" + longitude + " " + latitude + ")";
        String boundsWkt = createBoundsWkt(latitude, longitude, radiusMeters);
        List<TrackingRoute> routes = trackingRecommendationRepository.findRecommendedRoutes(
                pointWkt,
                boundsWkt,
                radiusMeters,
                limit
        );

        return RecommendedRouteResponse.from(routes);
    }

    /**
     * 원형 검색 반경을 포함하는 최소 사각 영역을 WKT POLYGON으로 생성합니다.
     *
     * <p>위도에 따라 경도 1도의 실제 거리가 달라지므로 현재 위도의 코사인 값으로 경도 범위를
     * 보정합니다. 극지방에서는 분모가 0에 가까워지는 것을 방지하기 위해 최소 스케일을 적용합니다.</p>
     */
    private String createBoundsWkt(double latitude, double longitude, int radiusMeters) {
        double latitudeDelta = radiusMeters / METERS_PER_LATITUDE_DEGREE;
        double longitudeScale = Math.max(
                Math.abs(Math.cos(Math.toRadians(latitude))),
                MINIMUM_LONGITUDE_SCALE
        );
        double longitudeDelta = radiusMeters / (METERS_PER_LATITUDE_DEGREE * longitudeScale);

        double minLatitude = Math.max(-90.0, latitude - latitudeDelta);
        double maxLatitude = Math.min(90.0, latitude + latitudeDelta);
        double minLongitude = Math.max(-180.0, longitude - longitudeDelta);
        double maxLongitude = Math.min(180.0, longitude + longitudeDelta);

        return "POLYGON(("
                + minLongitude + " " + minLatitude + ", "
                + maxLongitude + " " + minLatitude + ", "
                + maxLongitude + " " + maxLatitude + ", "
                + minLongitude + " " + maxLatitude + ", "
                + minLongitude + " " + minLatitude
                + "))";
    }
}
