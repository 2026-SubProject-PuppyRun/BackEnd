package org.zerock.puppyrun.tracking.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zerock.puppyrun.tracking.entity.TrackingRoute;

public interface TrackingRouteRepository extends JpaRepository<TrackingRoute, UUID> {

    // 특정 산책 기록의 경로 데이터 조회
    Optional<TrackingRoute> findByTrackingId(UUID trackingId);

    /**
     * 특정 지점에서 특정 반경(distance) 내에 포함되는 산책 경로들이 있는지 확인하는 공간 쿼리
     */
    @Query(value = "SELECT * FROM tracking_route t " +
            "WHERE ST_Distance_Sphere(t.route, ST_GeomFromText(:point, 4326)) <= :distance",
            nativeQuery = true)
    List<TrackingRoute> findRoutesWithinRadius(@Param("point") String pointWkt, @Param("distance") double distance);
}
