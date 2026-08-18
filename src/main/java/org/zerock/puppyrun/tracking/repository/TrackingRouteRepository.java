package org.zerock.puppyrun.tracking.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.puppyrun.tracking.entity.TrackingRoute;

/**
 * 산책 경로의 기본 CRUD를 제공하는 Repository입니다.
 */
public interface TrackingRouteRepository extends JpaRepository<TrackingRoute, UUID> {

    /**
     * 산책 기록 ID에 대응하는 경로 데이터를 조회합니다.
     *
     * @param trackingId 산책 기록 ID
     * @return 경로 데이터. 존재하지 않으면 빈 Optional
     */
    Optional<TrackingRoute> findByTrackingId(UUID trackingId);

    List<TrackingRoute> findAllByTrackingIdIn(Collection<UUID> trackingIds);

}
