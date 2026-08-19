package org.zerock.puppyrun.tracking.recommendation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.a_config.TestContainerConfig;

/**
 * MySQL 공간 인덱스의 후보 축소 효과를 확인하는 수동 실행용 성능 테스트입니다.
 *
 * <p>실행 명령:
 * {@code TRACKING_SPATIAL_INDEX_BENCHMARK_ENABLED=true ./gradlew test --tests '*TrackingRouteSpatialIndexPerformanceTest'}
 */
@EnabledIfEnvironmentVariable(named = "TRACKING_SPATIAL_INDEX_BENCHMARK_ENABLED", matches = "true")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TrackingRouteSpatialIndexPerformanceTest extends TestContainerConfig {

    private static final String BENCHMARK_TABLE = "tracking_route_spatial_benchmark";
    private static final String BENCHMARK_INDEX = "idx_benchmark_start_point_spatial";
    private static final int TOTAL_ROUTE_COUNT = 100_000;
    private static final int WARM_UP_COUNT = 2;
    private static final int MEASUREMENT_COUNT = 20;
    private static final double USER_LATITUDE = 37.5665;
    private static final double USER_LONGITUDE = 126.9780;
    private static final int RADIUS_METERS = 3_000;
    private static final int RESULT_LIMIT = 50;
    private static final double METERS_PER_LATITUDE_DEGREE = 111_320.0;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + BENCHMARK_TABLE);
    }

    @Test
    @DisplayName("10만 건 위치 데이터에서 공간 인덱스 생성 전후의 반경 조회 성능과 실행 계획을 비교한다")
    void compareRadiusSearchBeforeAndAfterSpatialIndex() {
        // given
        assertProductionIndexIsSpatial();
        createBenchmarkTable();
        insertRoutes();
        jdbcTemplate.execute("ANALYZE TABLE " + BENCHMARK_TABLE);

        String pointWkt = "POINT(" + USER_LONGITUDE + " " + USER_LATITUDE + ")";
        String boundsWkt = createBoundsWkt();
        Supplier<List<Long>> radiusSearch = () -> findRoutes(pointWkt, boundsWkt);

        List<Long> withoutIndexResult = radiusSearch.get();
        String withoutIndexPlan = explainRadiusSearch(pointWkt, boundsWkt);
        TimingStats withoutIndex = measureMillis(radiusSearch);

        jdbcTemplate.execute("CREATE SPATIAL INDEX " + BENCHMARK_INDEX
                + " ON " + BENCHMARK_TABLE + " (start_point)");
        jdbcTemplate.execute("ANALYZE TABLE " + BENCHMARK_TABLE);

        // when
        List<Long> withIndexResult = radiusSearch.get();
        String withIndexPlan = explainRadiusSearch(pointWkt, boundsWkt);
        TimingStats withIndex = measureMillis(radiusSearch);

        // then
        assertThat(withIndexResult).containsExactlyElementsOf(withoutIndexResult);
        assertThat(withoutIndexPlan).doesNotContain(BENCHMARK_INDEX);
        assertThat(withIndexPlan).contains(BENCHMARK_INDEX);

        double improvementPercent = (withoutIndex.medianMillis() - withIndex.medianMillis())
                / withoutIndex.medianMillis() * 100.0;
        double speedup = withoutIndex.medianMillis() / withIndex.medianMillis();

        System.out.println("\ntracking route spatial index benchmark (MySQL 8.4, warm measurements)");
        System.out.printf(
                "rows,radius_m,limit,"
                        + "without_index_mean_ms,without_index_median_ms,without_index_p95_ms,"
                        + "with_index_mean_ms,with_index_median_ms,with_index_p95_ms,"
                        + "median_improvement_percent,median_speedup%n"
        );
        System.out.printf(
                "%d,%d,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.2f,%.2f%n",
                TOTAL_ROUTE_COUNT,
                RADIUS_METERS,
                RESULT_LIMIT,
                withoutIndex.meanMillis(),
                withoutIndex.medianMillis(),
                withoutIndex.p95Millis(),
                withIndex.meanMillis(),
                withIndex.medianMillis(),
                withIndex.p95Millis(),
                improvementPercent,
                speedup
        );
        System.out.println("without_index_plan=" + withoutIndexPlan);
        System.out.println("with_spatial_index_plan=" + withIndexPlan);
    }

    private void assertProductionIndexIsSpatial() {
        String indexType = jdbcTemplate.queryForObject("""
                        SELECT INDEX_TYPE
                        FROM information_schema.STATISTICS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'tracking_route'
                          AND INDEX_NAME = 'idx_start_point_spatial'
                        """,
                String.class
        );

        assertThat(indexType).isEqualTo("SPATIAL");
    }

    private void createBenchmarkTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + BENCHMARK_TABLE);
        jdbcTemplate.execute("""
                CREATE TABLE tracking_route_spatial_benchmark (
                    tracking_id BIGINT NOT NULL PRIMARY KEY,
                    start_point POINT SRID 4326 NOT NULL
                ) ENGINE=InnoDB
                """);
    }

    private void insertRoutes() {
        List<Integer> indexes = IntStream.range(0, TOTAL_ROUTE_COUNT)
                .boxed()
                .toList();

        jdbcTemplate.batchUpdate("""
                        INSERT INTO tracking_route_spatial_benchmark (tracking_id, start_point)
                        VALUES (?, ST_GeomFromText(?, 4326, 'axis-order=long-lat'))
                        """,
                indexes,
                2_000,
                (PreparedStatement statement, Integer index) -> {
                    double longitude = 126.5 + unitInterval(mix64(index + 11L));
                    double latitude = 37.0 + unitInterval(mix64(index + 97L));
                    statement.setLong(1, index + 1L);
                    statement.setString(2, "POINT(" + longitude + " " + latitude + ")");
                }
        );
    }

    private List<Long> findRoutes(String pointWkt, String boundsWkt) {
        return jdbcTemplate.queryForList("""
                        SELECT tracking_id
                        FROM tracking_route_spatial_benchmark
                        WHERE ST_Contains(
                                  ST_GeomFromText(?, 4326, 'axis-order=long-lat'),
                                  start_point
                              )
                          AND ST_Distance_Sphere(
                                  start_point,
                                  ST_GeomFromText(?, 4326, 'axis-order=long-lat')
                              ) <= ?
                        ORDER BY ST_Distance_Sphere(
                                     start_point,
                                     ST_GeomFromText(?, 4326, 'axis-order=long-lat')
                                 ),
                                 tracking_id
                        LIMIT ?
                        """,
                Long.class,
                boundsWkt,
                pointWkt,
                RADIUS_METERS,
                pointWkt,
                RESULT_LIMIT
        );
    }

    private String explainRadiusSearch(String pointWkt, String boundsWkt) {
        return jdbcTemplate.queryForObject("""
                        EXPLAIN FORMAT=JSON
                        SELECT tracking_id
                        FROM tracking_route_spatial_benchmark
                        WHERE ST_Contains(
                                  ST_GeomFromText(?, 4326, 'axis-order=long-lat'),
                                  start_point
                              )
                          AND ST_Distance_Sphere(
                                  start_point,
                                  ST_GeomFromText(?, 4326, 'axis-order=long-lat')
                              ) <= ?
                        ORDER BY ST_Distance_Sphere(
                                     start_point,
                                     ST_GeomFromText(?, 4326, 'axis-order=long-lat')
                                 ),
                                 tracking_id
                        LIMIT ?
                        """,
                String.class,
                boundsWkt,
                pointWkt,
                RADIUS_METERS,
                pointWkt,
                RESULT_LIMIT
        );
    }

    private TimingStats measureMillis(Supplier<List<Long>> invocation) {
        for (int i = 0; i < WARM_UP_COUNT; i++) {
            assertThat(invocation.get()).isNotEmpty();
        }

        long[] measurements = new long[MEASUREMENT_COUNT];
        for (int i = 0; i < MEASUREMENT_COUNT; i++) {
            long startedAt = System.nanoTime();
            List<Long> result = invocation.get();
            measurements[i] = System.nanoTime() - startedAt;
            assertThat(result).isNotEmpty();
        }

        Arrays.sort(measurements);
        double meanNanos = Arrays.stream(measurements)
                .average()
                .orElseThrow();
        double medianNanos = measurements.length % 2 == 0
                ? (measurements[measurements.length / 2 - 1] + measurements[measurements.length / 2]) / 2.0
                : measurements[measurements.length / 2];
        int p95Index = (int) Math.ceil(measurements.length * 0.95) - 1;

        return new TimingStats(
                toMillis(meanNanos),
                toMillis(medianNanos),
                toMillis(measurements[p95Index])
        );
    }

    private double toMillis(double nanos) {
        return nanos / TimeUnit.MILLISECONDS.toNanos(1L);
    }

    private String createBoundsWkt() {
        double latitudeDelta = RADIUS_METERS / METERS_PER_LATITUDE_DEGREE;
        double longitudeDelta = RADIUS_METERS
                / (METERS_PER_LATITUDE_DEGREE * Math.cos(Math.toRadians(USER_LATITUDE)));

        double minLatitude = USER_LATITUDE - latitudeDelta;
        double maxLatitude = USER_LATITUDE + latitudeDelta;
        double minLongitude = USER_LONGITUDE - longitudeDelta;
        double maxLongitude = USER_LONGITUDE + longitudeDelta;

        return "POLYGON(("
                + minLongitude + " " + minLatitude + ", "
                + maxLongitude + " " + minLatitude + ", "
                + maxLongitude + " " + maxLatitude + ", "
                + minLongitude + " " + maxLatitude + ", "
                + minLongitude + " " + minLatitude
                + "))";
    }

    private static long mix64(long value) {
        long mixed = value;
        mixed = (mixed ^ (mixed >>> 30)) * 0xbf58476d1ce4e5b9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94d049bb133111ebL;
        return mixed ^ (mixed >>> 31);
    }

    private static double unitInterval(long value) {
        return (value >>> 11) * 0x1.0p-53;
    }

    private record TimingStats(double meanMillis, double medianMillis, double p95Millis) {
    }
}
