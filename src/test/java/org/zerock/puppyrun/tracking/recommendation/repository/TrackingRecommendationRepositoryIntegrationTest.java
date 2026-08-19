package org.zerock.puppyrun.tracking.recommendation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zerock.puppyrun.a_config.TestContainerConfig;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.tracking.entity.RoutePoint;
import org.zerock.puppyrun.tracking.entity.Tracking;
import org.zerock.puppyrun.tracking.entity.TrackingRoute;
import org.zerock.puppyrun.tracking.entity.Visibility;

class TrackingRecommendationRepositoryIntegrationTest extends TestContainerConfig {

    private static final double USER_LATITUDE = 37.5665;
    private static final double USER_LONGITUDE = 126.9780;
    private static final double METERS_PER_LATITUDE_DEGREE = 111_320.0;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TrackingRecommendationRepository trackingRecommendationRepository;

    @Test
    @DisplayName("검색 반경 안의 공개 산책 경로만 사용자 위치와 가까운 순서로 조회한다")
    void findPublicRoutesWithinRadiusOrderedByDistance() {
        // given
        Member member = persistMember("spatial-order");
        TrackingRoute nearest = persistRoute(member, Visibility.PUBLIC, USER_LATITUDE, USER_LONGITUDE);
        persistRoute(member, Visibility.PRIVATE, USER_LATITUDE + 0.001, USER_LONGITUDE);
        TrackingRoute middle = persistRoute(member, Visibility.PUBLIC, USER_LATITUDE + 0.005, USER_LONGITUDE);
        TrackingRoute farthest = persistRoute(member, Visibility.PUBLIC, USER_LATITUDE + 0.015, USER_LONGITUDE);
        persistRoute(member, Visibility.PUBLIC, USER_LATITUDE + 0.1, USER_LONGITUDE);
        entityManager.flush();
        entityManager.clear();

        int radiusMeters = 3_000;

        // when
        List<TrackingRoute> routes = trackingRecommendationRepository.findRecommendedRoutes(
                pointWkt(USER_LATITUDE, USER_LONGITUDE),
                boundsWkt(USER_LATITUDE, USER_LONGITUDE, radiusMeters),
                radiusMeters,
                10
        );

        // then
        assertThat(routes)
                .extracting(TrackingRoute::getTrackingId)
                .containsExactly(
                        nearest.getTrackingId(),
                        middle.getTrackingId(),
                        farthest.getTrackingId()
                );
    }

    @Test
    @DisplayName("가까운 후보가 비공개 경로뿐이면 다음 공간 인덱스 후보 배치에서 공개 경로를 보충한다")
    void continueCandidateSearchWhenNearestBatchContainsPrivateRoutes() {
        // given
        Member member = persistMember("spatial-batch");
        for (int index = 0; index < 55; index++) {
            persistRoute(
                    member,
                    Visibility.PRIVATE,
                    USER_LATITUDE + 0.0001 * index,
                    USER_LONGITUDE
            );
        }

        List<TrackingRoute> expected = new ArrayList<>();
        for (int index = 0; index < 3; index++) {
            expected.add(persistRoute(
                    member,
                    Visibility.PUBLIC,
                    USER_LATITUDE + 0.006 + 0.0001 * index,
                    USER_LONGITUDE
            ));
        }
        entityManager.flush();
        entityManager.clear();

        int radiusMeters = 3_000;

        // when
        List<TrackingRoute> routes = trackingRecommendationRepository.findRecommendedRoutes(
                pointWkt(USER_LATITUDE, USER_LONGITUDE),
                boundsWkt(USER_LATITUDE, USER_LONGITUDE, radiusMeters),
                radiusMeters,
                3
        );

        // then
        assertThat(routes)
                .extracting(TrackingRoute::getTrackingId)
                .containsExactlyElementsOf(expected.stream()
                        .map(TrackingRoute::getTrackingId)
                        .toList());
    }

    private Member persistMember(String suffix) {
        Member member = Member.builder()
                .email("tracking-recommendation-" + suffix + "@test.com")
                .nickName("tracking-recommendation-" + suffix)
                .password("encoded-password")
                .build();
        entityManager.persist(member);
        return member;
    }

    private TrackingRoute persistRoute(
            Member member,
            Visibility visibility,
            double latitude,
            double longitude
    ) {
        Tracking tracking = Tracking.builder()
                .member(member)
                .startedAt(LocalDateTime.of(2026, 8, 19, 9, 0))
                .endedAt(LocalDateTime.of(2026, 8, 19, 9, 30))
                .distance(2_000)
                .averagePace(6.5)
                .restDuration(300)
                .visibility(visibility)
                .images(List.of())
                .build();
        entityManager.persist(tracking);

        TrackingRoute route = new TrackingRoute(
                tracking,
                List.of(new RoutePoint(latitude, longitude, 0))
        );
        entityManager.persist(route);
        return route;
    }

    private String pointWkt(double latitude, double longitude) {
        return "POINT(" + longitude + " " + latitude + ")";
    }

    private String boundsWkt(double latitude, double longitude, int radiusMeters) {
        double latitudeDelta = radiusMeters / METERS_PER_LATITUDE_DEGREE;
        double longitudeDelta = radiusMeters
                / (METERS_PER_LATITUDE_DEGREE * Math.cos(Math.toRadians(latitude)));

        double minLatitude = latitude - latitudeDelta;
        double maxLatitude = latitude + latitudeDelta;
        double minLongitude = longitude - longitudeDelta;
        double maxLongitude = longitude + longitudeDelta;

        return "POLYGON(("
                + minLongitude + " " + minLatitude + ", "
                + maxLongitude + " " + minLatitude + ", "
                + maxLongitude + " " + maxLatitude + ", "
                + minLongitude + " " + maxLatitude + ", "
                + minLongitude + " " + minLatitude
                + "))";
    }
}
