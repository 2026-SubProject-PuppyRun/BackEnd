package org.zerock.puppyrun.tracking.repository;

import java.util.List;
import org.zerock.puppyrun.tracking.entity.TrackingRoute;

/**
 * QueryDSL을 사용하는 산책 경로 확장 조회 계약입니다.
 *
 * <p>추천 조건이 늘어날 때 기본 JpaRepository를 변경하지 않고 구현체에서 동적 조건을 조합할 수 있습니다.</p>
 */
public interface TrackingRouteRepositoryCustom {

    /**
     * 사용자 위치 주변에서 시작하는 공개 산책 경로를 가까운 순서로 조회합니다.
     *
     * @param pointWkt 사용자 위치를 표현한 WKT POINT. 좌표 순서는 경도, 위도
     * @param boundsWkt 공간 인덱스 후보 검색에 사용할 WKT POLYGON
     * @param radiusMeters 사용자 위치로부터 허용할 최대 거리(m)
     * @param limit 반환할 최대 공개 경로 수
     * @return 사용자 위치와 가까운 순서로 정렬된 공개 경로 목록
     */
    List<TrackingRoute> findRecommendedRoutes(
            String pointWkt,
            String boundsWkt,
            int radiusMeters,
            int limit
    );
}
