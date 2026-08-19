package org.zerock.puppyrun.tracking.recommendation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
 * 20만 건 위치 데이터 적재 시 공간 인덱스 유지 비용을 확인하는 수동 실행용 성능 테스트입니다.
 *
 * <p>실행 명령:
 * {@code TRACKING_SPATIAL_INDEX_WRITE_BENCHMARK_ENABLED=true ./gradlew test --tests '*TrackingRouteSpatialIndexWritePerformanceTest'}
 */
@EnabledIfEnvironmentVariable(named = "TRACKING_SPATIAL_INDEX_WRITE_BENCHMARK_ENABLED", matches = "true")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TrackingRouteSpatialIndexWritePerformanceTest extends TestContainerConfig {

    private static final String WITHOUT_INDEX_TABLE = "tracking_route_write_without_spatial_index";
    private static final String WITH_INDEX_TABLE = "tracking_route_write_with_spatial_index";
    private static final String SPATIAL_INDEX = "idx_write_start_point_spatial";
    private static final int TOTAL_ROUTE_COUNT = 200_000;
    private static final int BATCH_SIZE = 2_000;
    private static final int WARM_UP_ROUTE_COUNT = 4_000;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + WITHOUT_INDEX_TABLE);
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + WITH_INDEX_TABLE);
    }

    @Test
    @DisplayName("20만 건 위치 데이터 적재 시 공간 인덱스 유무에 따른 쓰기 처리량을 비교한다")
    void compareInsertThroughputWithAndWithoutSpatialIndex() {
        // given
        createBenchmarkTables();
        assertBenchmarkIndexIsSpatial();

        List<RouteRow> routes = createRoutes(TOTAL_ROUTE_COUNT);
        warmUpAndTruncate(routes.subList(0, WARM_UP_ROUTE_COUNT));

        long[] withoutIndexBatchNanos = new long[TOTAL_ROUTE_COUNT / BATCH_SIZE];
        long[] withIndexBatchNanos = new long[TOTAL_ROUTE_COUNT / BATCH_SIZE];

        // when
        for (int batchIndex = 0; batchIndex < withoutIndexBatchNanos.length; batchIndex++) {
            int fromIndex = batchIndex * BATCH_SIZE;
            int toIndex = fromIndex + BATCH_SIZE;
            List<RouteRow> batch = routes.subList(fromIndex, toIndex);

            // 두 테이블을 같은 크기로 유지하고 매 배치의 선후 순서를 바꿔 실행 순서 편향을 줄입니다.
            if (batchIndex % 2 == 0) {
                withoutIndexBatchNanos[batchIndex] = insertBatch(WITHOUT_INDEX_TABLE, batch);
                withIndexBatchNanos[batchIndex] = insertBatch(WITH_INDEX_TABLE, batch);
            } else {
                withIndexBatchNanos[batchIndex] = insertBatch(WITH_INDEX_TABLE, batch);
                withoutIndexBatchNanos[batchIndex] = insertBatch(WITHOUT_INDEX_TABLE, batch);
            }
        }

        // then
        assertThat(countRows(WITHOUT_INDEX_TABLE)).isEqualTo(TOTAL_ROUTE_COUNT);
        assertThat(countRows(WITH_INDEX_TABLE)).isEqualTo(TOTAL_ROUTE_COUNT);

        WriteStats withoutIndex = calculateStats(withoutIndexBatchNanos);
        WriteStats withIndex = calculateStats(withIndexBatchNanos);
        double elapsedIncreasePercent = (withIndex.totalMillis() - withoutIndex.totalMillis())
                / withoutIndex.totalMillis() * 100.0;
        double throughputDecreasePercent = (withoutIndex.rowsPerSecond() - withIndex.rowsPerSecond())
                / withoutIndex.rowsPerSecond() * 100.0;

        System.out.println("\ntracking route spatial index write benchmark (MySQL 8.4, 2,000-row batches)");
        System.out.println(
                "rows,batch_size,without_index_total_ms,with_index_total_ms,"
                        + "elapsed_increase_percent,without_index_rows_per_sec,with_index_rows_per_sec,"
                        + "throughput_decrease_percent,without_index_batch_mean_ms,with_index_batch_mean_ms,"
                        + "without_index_batch_median_ms,with_index_batch_median_ms,"
                        + "without_index_batch_p95_ms,with_index_batch_p95_ms"
        );
        System.out.printf(
                "%d,%d,%.3f,%.3f,%.2f,%.2f,%.2f,%.2f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f%n",
                TOTAL_ROUTE_COUNT,
                BATCH_SIZE,
                withoutIndex.totalMillis(),
                withIndex.totalMillis(),
                elapsedIncreasePercent,
                withoutIndex.rowsPerSecond(),
                withIndex.rowsPerSecond(),
                throughputDecreasePercent,
                withoutIndex.batchMeanMillis(),
                withIndex.batchMeanMillis(),
                withoutIndex.batchMedianMillis(),
                withIndex.batchMedianMillis(),
                withoutIndex.batchP95Millis(),
                withIndex.batchP95Millis()
        );
    }

    private void createBenchmarkTables() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + WITHOUT_INDEX_TABLE);
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + WITH_INDEX_TABLE);
        jdbcTemplate.execute("""
                CREATE TABLE tracking_route_write_without_spatial_index (
                    tracking_id BIGINT NOT NULL PRIMARY KEY,
                    start_point POINT SRID 4326 NOT NULL
                ) ENGINE=InnoDB
                """);
        jdbcTemplate.execute("""
                CREATE TABLE tracking_route_write_with_spatial_index (
                    tracking_id BIGINT NOT NULL PRIMARY KEY,
                    start_point POINT SRID 4326 NOT NULL,
                    SPATIAL INDEX idx_write_start_point_spatial (start_point)
                ) ENGINE=InnoDB
                """);
    }

    private void assertBenchmarkIndexIsSpatial() {
        String indexType = jdbcTemplate.queryForObject("""
                        SELECT INDEX_TYPE
                        FROM information_schema.STATISTICS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = ?
                          AND INDEX_NAME = ?
                        """,
                String.class,
                WITH_INDEX_TABLE,
                SPATIAL_INDEX
        );

        assertThat(indexType).isEqualTo("SPATIAL");
    }

    private void warmUpAndTruncate(List<RouteRow> warmUpRoutes) {
        insertBatch(WITHOUT_INDEX_TABLE, warmUpRoutes);
        insertBatch(WITH_INDEX_TABLE, warmUpRoutes);
        jdbcTemplate.execute("TRUNCATE TABLE " + WITHOUT_INDEX_TABLE);
        jdbcTemplate.execute("TRUNCATE TABLE " + WITH_INDEX_TABLE);
    }

    private long insertBatch(String tableName, List<RouteRow> routes) {
        long startedAt = System.nanoTime();
        jdbcTemplate.batchUpdate(
                "INSERT INTO " + tableName
                        + " (tracking_id, start_point) "
                        + "VALUES (?, ST_GeomFromText(?, 4326, 'axis-order=long-lat'))",
                routes,
                routes.size(),
                (PreparedStatement statement, RouteRow route) -> {
                    statement.setLong(1, route.trackingId());
                    statement.setString(2, route.pointWkt());
                }
        );
        return System.nanoTime() - startedAt;
    }

    private long countRows(String tableName) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName,
                Long.class
        );
        return count == null ? 0L : count;
    }

    private List<RouteRow> createRoutes(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> {
                    double longitude = 126.5 + unitInterval(mix64(index + 11L));
                    double latitude = 37.0 + unitInterval(mix64(index + 97L));
                    return new RouteRow(
                            index + 1L,
                            "POINT(" + longitude + " " + latitude + ")"
                    );
                })
                .toList();
    }

    private WriteStats calculateStats(long[] batchNanos) {
        long totalNanos = Arrays.stream(batchNanos).sum();
        long[] sortedBatchNanos = batchNanos.clone();
        Arrays.sort(sortedBatchNanos);

        double batchMeanNanos = Arrays.stream(sortedBatchNanos)
                .average()
                .orElseThrow();
        double batchMedianNanos = sortedBatchNanos.length % 2 == 0
                ? (sortedBatchNanos[sortedBatchNanos.length / 2 - 1]
                + sortedBatchNanos[sortedBatchNanos.length / 2]) / 2.0
                : sortedBatchNanos[sortedBatchNanos.length / 2];
        int p95Index = (int) Math.ceil(sortedBatchNanos.length * 0.95) - 1;

        double totalMillis = toMillis(totalNanos);
        return new WriteStats(
                totalMillis,
                TOTAL_ROUTE_COUNT / (totalMillis / 1_000.0),
                toMillis(batchMeanNanos),
                toMillis(batchMedianNanos),
                toMillis(sortedBatchNanos[p95Index])
        );
    }

    private double toMillis(double nanos) {
        return nanos / TimeUnit.MILLISECONDS.toNanos(1L);
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

    private record RouteRow(long trackingId, String pointWkt) {
    }

    private record WriteStats(
            double totalMillis,
            double rowsPerSecond,
            double batchMeanMillis,
            double batchMedianMillis,
            double batchP95Millis
    ) {
    }
}
