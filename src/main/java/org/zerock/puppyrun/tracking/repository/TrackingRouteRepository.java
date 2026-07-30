package org.zerock.puppyrun.tracking.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.puppyrun.tracking.entity.TrackingRoute;

/**
 * 산책 경로의 기본 CRUD와 위치 기반 추천 조회를 제공하는 Repository입니다.
 *
 * <p>단순 영속성 작업은 {@link JpaRepository}가 담당하고, 공간 함수를 사용하는 추천 조회는
 * {@link TrackingRouteRepositoryCustom} 구현에 위임합니다.</p>
 */
public interface TrackingRouteRepository
        extends JpaRepository<TrackingRoute, UUID>, TrackingRouteRepositoryCustom {

    /**
     * 산책 기록 ID에 대응하는 경로 데이터를 조회합니다.
     *
     * @param trackingId 산책 기록 ID
     * @return 경로 데이터. 존재하지 않으면 빈 Optional
     */
    Optional<TrackingRoute> findByTrackingId(UUID trackingId);

}
