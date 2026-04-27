package org.zerock.puppyrun.tracking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.zerock.puppyrun.common.exception.InvalidValueException;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tracking_route",
        indexes = {
                @Index(name = "idx_start_point_spatial", columnList = "start_point")
        })
public class TrackingRoute {
    public static final int SRID_NUM = 4326;
    private static final String POINT = "POINT SRID " + SRID_NUM;
    private static final String LINESTRING = "LINESTRING SRID " + SRID_NUM;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), SRID_NUM);

    @Id
    private UUID trackingId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracking_id")
    private Tracking tracking;

    // 원본 데이터 보관
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private List<RoutePoint> rawPath;

    // 추천 및 공간 분석용 데이터
    @Column(columnDefinition = POINT, nullable = false)
    private Point startPoint;

    @Column(columnDefinition = POINT, nullable = false)
    private Point endPoint;

    @Column(name = "route", columnDefinition = LINESTRING, nullable = false)
    private LineString route;

    public TrackingRoute(Tracking tracking, List<RoutePoint> routePoints) {
        if (routePoints == null || routePoints.isEmpty()) {
            throw new InvalidValueException("산책 경로는 최소 1개 이상의 좌표가 필요합니다.");
        }
        this.tracking = tracking;
        this.route = createLineString(routePoints);
        this.rawPath = routePoints;
        this.startPoint = this.route.getStartPoint();
        this.endPoint = this.route.getEndPoint();
    }


    private LineString createLineString(List<RoutePoint> points) {
        Coordinate[] coords = points.stream()
                .map(p -> new Coordinate(p.lng(), p.lat()))
                .toArray(Coordinate[]::new);

        if (coords.length == 1) {
            coords = new Coordinate[]{coords[0], coords[0]};
        }
        return GEOMETRY_FACTORY.createLineString(coords);
    }

    /**
     * 단순화된 좌표 리스트를 반환합니다.
     */
    public List<RoutePoint> getOptimizedPath(double tolerance) {
        if (this.route == null) {
            return List.of();
        }
        LineString simplified = (LineString) DouglasPeuckerSimplifier.simplify(this.route, tolerance);
        return toRoutePoints(simplified);
    }

    /**
     * 원본 좌표 리스트(JSON 필드)를 반환합니다.
     */
    public List<RoutePoint> getOriginalPath() {
        return this.rawPath != null ? List.copyOf(this.rawPath) : List.of();
    }

    /**
     * LineString을 RoutePoint 리스트로 변환하는 내부 메서드
     */
    private List<RoutePoint> toRoutePoints(LineString line) {
        if (line == null) {
            return List.of();
        }

        return Arrays.stream(line.getCoordinates())
                .map(c -> new RoutePoint(c.y, c.x, 0))
                .toList();
    }

    public int getPointCount() {
        return route != null ? route.getNumPoints() : 0;
    }

    public boolean isWithin(Geometry area) {
        return route != null && route.within(area);
    }

    public boolean intersects(Geometry other) {
        return route != null && route.intersects(other);
    }
}
