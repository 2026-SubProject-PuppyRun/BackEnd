package org.zerock.puppyrun.tracking.repository;

import static org.zerock.puppyrun.tracking.entity.QTracking.tracking;
import static org.zerock.puppyrun.tracking.entity.QTrackingRoute.trackingRoute;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.SimpleExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Repository;
import org.zerock.puppyrun.tracking.entity.TrackingRoute;
import org.zerock.puppyrun.tracking.entity.Visibility;

/**
 * 위치 기반 추천 경로 조회를 QueryDSL로 구현합니다.
 *
 * <p>MySQL이 공개 여부 조인을 먼저 수행하면 {@code start_point} 공간 인덱스를 사용하지 않을 수 있습니다.
 * 이를 방지하기 위해 공간 인덱스로 거리순 후보 ID를 먼저 조회하고, 후보의 공개 여부와 연관 {@link org.zerock.puppyrun.tracking.entity.Tracking} 정보를 두 번째
 * 쿼리에서 확인합니다.</p>
 */
@Repository
@RequiredArgsConstructor
public class TrackingRouteRepositoryCustomImpl implements TrackingRouteRepositoryCustom {
    private static final int SRID = 4326;
    private static final String AXIS_ORDER = "axis-order=long-lat";
    private static final int MINIMUM_CANDIDATE_BATCH_SIZE = 50;
    private static final int CANDIDATE_BATCH_MULTIPLIER = 4;

    private final JPAQueryFactory queryFactory;

    /**
     * 공간 인덱스 후보 조회와 공개 경로 조회를 반복하여 요청한 추천 개수를 채웁니다.
     *
     * <p>후보 배치에 비공개 경로가 많아 결과가 부족하면 다음 거리순 배치를 이어서 조회합니다.</p>
     *
     * @param pointWkt     사용자 위치 WKT POINT
     * @param boundsWkt    후보 검색 영역 WKT POLYGON
     * @param radiusMeters 최대 검색 반경(m)
     * @param limit        최대 추천 개수
     * @return 거리순 공개 경로 목록
     */
    @Override
    public List<TrackingRoute> findRecommendedRoutes(
            String pointWkt,
            String boundsWkt,
            int radiusMeters,
            int limit
    ) {
        if (limit <= 0) {
            return List.of();
        }

        SimpleExpression<Point> userPoint = geometryFromText(Point.class, pointWkt);
        SimpleExpression<Geometry> searchBounds = geometryFromText(Geometry.class, boundsWkt);
        BooleanExpression startsWithinBounds = Expressions.booleanTemplate(
                "function('MBRContains', {0}, {1}) = 1",
                searchBounds,
                trackingRoute.startPoint
        );
        NumberExpression<Double> distanceFromUser = Expressions.numberTemplate(
                Double.class,
                "function('ST_Distance_Sphere', {0}, {1})",
                trackingRoute.startPoint,
                userPoint
        );

        int candidateBatchSize = Math.max(
                MINIMUM_CANDIDATE_BATCH_SIZE,
                limit * CANDIDATE_BATCH_MULTIPLIER
        );
        List<TrackingRoute> recommendations = new ArrayList<>(limit);
        long offset = 0;

        while (recommendations.size() < limit) {
            // Tracking 조인 없이 경로 테이블만 조회해야 MySQL이 start_point 공간 인덱스를 우선 적용할 수 있습니다.
            List<UUID> candidateIds = findCandidateIds(
                    startsWithinBounds,
                    distanceFromUser,
                    radiusMeters,
                    candidateBatchSize,
                    offset
            );
            if (candidateIds.isEmpty()) {
                break;
            }

            // 후보의 공개 여부를 확인하면서 응답에 필요한 Tracking까지 한 번에 로딩합니다.
            Map<UUID, TrackingRoute> publicRoutesById = findPublicRoutesById(candidateIds);
            for (UUID candidateId : candidateIds) {
                TrackingRoute route = publicRoutesById.get(candidateId);
                if (route == null) {
                    continue;
                }

                recommendations.add(route);
                if (recommendations.size() == limit) {
                    break;
                }

            }

            offset += candidateIds.size();
            if (candidateIds.size() < candidateBatchSize) {
                break;
            }
        }

        return List.copyOf(recommendations);
    }

    /**
     * 검색 사각형으로 후보를 축소한 후 정확한 구면 거리를 적용해 후보 ID를 조회합니다.
     *
     * @param startsWithinBounds 공간 인덱스 후보 조건
     * @param distanceFromUser   사용자 위치와 경로 시작점의 구면 거리 표현식
     * @param radiusMeters       최대 검색 반경(m)
     * @param batchSize          한 번에 조회할 후보 개수
     * @param offset             거리순 후보 조회 시작 위치
     * @return 거리순 후보 경로 ID
     */
    private List<UUID> findCandidateIds(
            BooleanExpression startsWithinBounds,
            NumberExpression<Double> distanceFromUser,
            int radiusMeters,
            int batchSize,
            long offset
    ) {
        return queryFactory
                .select(trackingRoute.trackingId)
                .from(trackingRoute)
                .where(
                        startsWithinBounds,
                        distanceFromUser.loe((double) radiusMeters)
                )
                .orderBy(distanceFromUser.asc(), trackingRoute.trackingId.asc())
                .offset(offset)
                .limit(batchSize)
                .fetch();
    }

    /**
     * 후보 중 공개 경로만 조회하고 응답 생성에 필요한 Tracking 연관 객체를 fetch join합니다.
     *
     * @param candidateIds 공간 검색으로 선별한 경로 ID
     * @return 경로 ID를 키로 갖는 공개 경로 Map
     */
    private Map<UUID, TrackingRoute> findPublicRoutesById(List<UUID> candidateIds) {
        List<TrackingRoute> publicRoutes = queryFactory
                .selectFrom(trackingRoute)
                .join(trackingRoute.tracking, tracking).fetchJoin()
                .where(
                        trackingRoute.trackingId.in(candidateIds),
                        tracking.visibility.eq(Visibility.PUBLIC)
                )
                .fetch();

        Map<UUID, TrackingRoute> routesById = new LinkedHashMap<>();
        for (TrackingRoute route : publicRoutes) {
            routesById.put(route.getTrackingId(), route);
        }
        return routesById;
    }

    /**
     * WKT 문자열을 SRID 4326 공간 객체로 변환하는 MySQL 함수를 QueryDSL 표현식으로 생성합니다.
     *
     * @param geometryType 반환할 Geometry 타입
     * @param wkt          변환할 WKT 문자열
     * @param <T>          Point, Polygon 등 Geometry 하위 타입
     * @return {@code ST_GeomFromText} QueryDSL 표현식
     */
    private <T extends Geometry> SimpleExpression<T> geometryFromText(Class<T> geometryType, String wkt) {
        return Expressions.template(
                geometryType,
                "function('ST_GeomFromText', {0}, {1}, {2})",
                Expressions.constant(wkt),
                Expressions.constant(SRID),
                Expressions.constant(AXIS_ORDER)
        );
    }
}
