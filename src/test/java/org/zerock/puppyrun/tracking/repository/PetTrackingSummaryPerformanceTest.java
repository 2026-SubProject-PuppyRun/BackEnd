package org.zerock.puppyrun.tracking.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.zerock.puppyrun.a_config.TestContainerConfig;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.pet.entity.Breed;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.tracking.DTO.TotalPetTracking;
import org.zerock.puppyrun.tracking.entity.PetTracking;
import org.zerock.puppyrun.tracking.entity.Tracking;

/**
 * 수동 실행용 성능 테스트.
 *
 * <p>실행 명령:
 * {@code PET_TRACKING_BENCHMARK_ENABLED=true ./gradlew test --tests '*PetTrackingSummaryPerformanceTest'}
 */
@EnabledIfEnvironmentVariable(named = "PET_TRACKING_BENCHMARK_ENABLED", matches = "true")
class PetTrackingSummaryPerformanceTest extends TestContainerConfig {

    private static final int PET_COUNT = 5;
    private static final int ROWS_PER_DAY = 10_000;
    private static final int TOTAL_TRACKING_COUNT = 100_000;
    private static final int WARM_UP_COUNT = 2;
    private static final int MEASUREMENT_COUNT = 5;
    private static final LocalDate BASE_DATE = LocalDate.of(2026, 1, 1);

    @Autowired
    private PetTrackingRepository petTrackingRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("QueryDSL 집계와 JPA-only 방식의 대용량 성능을 비교한다")
    void compareQueryDslAndJpaOnly() {
        // given
        List<UUID> petIds = createOwnerAndPets();
        insertTrackingRows(petIds);

        System.out.println("\npet tracking summary benchmark (MySQL 8.4, warm median)");
        System.out.println("matched_rows,querydsl_ms,jpa_criteria_ms,jpa_entity_ms");

        // when
        for (int dayCount : List.of(1, 5, 10)) {
            LocalDate endDate = BASE_DATE.plusDays(dayCount - 1L);

            List<TotalPetTracking> expected = queryDslSummary(petIds, BASE_DATE, endDate);
            List<TotalPetTracking> criteriaResult = jpaCriteriaSummary(petIds, BASE_DATE, endDate);
            List<TotalPetTracking> entityResult = jpaEntitySummary(petIds, BASE_DATE, endDate);

            // then
            assertSameSummary(expected, criteriaResult);
            assertSameSummary(expected, entityResult);

            double queryDslMillis = medianMillis(
                    () -> queryDslSummary(petIds, BASE_DATE, endDate));
            double criteriaMillis = medianMillis(
                    () -> jpaCriteriaSummary(petIds, BASE_DATE, endDate));
            double entityMillis = medianMillis(
                    () -> jpaEntitySummary(petIds, BASE_DATE, endDate));

            System.out.printf("%d,%.3f,%.3f,%.3f%n",
                    dayCount * ROWS_PER_DAY,
                    queryDslMillis,
                    criteriaMillis,
                    entityMillis);
        }
    }

    private List<UUID> createOwnerAndPets() {
        Member member = Member.builder()
                .nickName("summary-performance-user")
                .email("summary-performance@example.com")
                .password("password")
                .build();
        entityManager.persist(member);

        List<UUID> petIds = new ArrayList<>();
        for (int i = 0; i < PET_COUNT; i++) {
            Pet pet = Pet.builder()
                    .member(member)
                    .name("pet-" + i)
                    .birthYear(LocalDate.of(2020, 1, 1))
                    .breed(Breed.MALTESE)
                    .color("#FFFFFF")
                    .weight(3.0)
                    .isNeutered(false)
                    .gender("M")
                    .build();
            entityManager.persist(pet);
            petIds.add(pet.getId());
        }
        entityManager.flush();
        entityManager.clear();
        return petIds;
    }

    private void insertTrackingRows(List<UUID> petIds) {
        UUID memberId = jdbcTemplate.queryForObject(
                "SELECT id FROM member WHERE email = ?",
                (resultSet, rowNumber) -> bytesToUuid(resultSet.getBytes(1)),
                "summary-performance@example.com");

        List<Integer> indexes = java.util.stream.IntStream.range(0, TOTAL_TRACKING_COUNT)
                .boxed()
                .toList();

        jdbcTemplate.batchUpdate("""
                        INSERT INTO tracking (
                            id, member_id, started_at, ended_at, duration, distance,
                            average_pace, rest_duration, visibility, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PUBLIC', NOW(), NOW())
                        """,
                indexes,
                2_000,
                (statement, index) -> {
                    LocalDateTime startedAt = BASE_DATE
                            .plusDays(index / ROWS_PER_DAY)
                            .atStartOfDay()
                            .plusSeconds(index % ROWS_PER_DAY);
                    statement.setBytes(1, uuidToBytes(trackingId(index)));
                    statement.setBytes(2, uuidToBytes(memberId));
                    statement.setTimestamp(3, Timestamp.valueOf(startedAt));
                    statement.setTimestamp(4, Timestamp.valueOf(startedAt.plusMinutes(30)));
                    statement.setInt(5, 1_800);
                    statement.setInt(6, 900 + index % 201);
                    statement.setDouble(7, 4.0 + index % 7);
                    statement.setInt(8, index % 121);
                });

        jdbcTemplate.batchUpdate("""
                        INSERT INTO pet_tracking (id, pet_id, tracking_id, created_at, updated_at)
                        VALUES (?, ?, ?, NOW(), NOW())
                        """,
                indexes,
                2_000,
                (statement, index) -> {
                    statement.setBytes(1, uuidToBytes(petTrackingId(index)));
                    statement.setBytes(2, uuidToBytes(petIds.get(index % petIds.size())));
                    statement.setBytes(3, uuidToBytes(trackingId(index)));
                });
        entityManager.clear();
    }

    private List<TotalPetTracking> queryDslSummary(
            List<UUID> petIds,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return petTrackingRepository.getTrackingSummaryByPetId(petIds, startDate, endDate);
    }

    /**
     * QueryDSL 없이 JPA Criteria API만 사용하고 합계/평균 계산은 DB에 맡기는 방식.
     * Pet에 PetTracking 역방향 컬렉션이 없기 때문에 펫 정보와 집계를 각각 조회한다.
     */
    private List<TotalPetTracking> jpaCriteriaSummary(
            List<UUID> petIds,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<Pet> pets = entityManager.createQuery("""
                        SELECT p
                        FROM Pet p
                        WHERE p.id IN :petIds
                        """, Pet.class)
                .setParameter("petIds", petIds)
                .getResultList();

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = criteriaBuilder.createTupleQuery();
        Root<PetTracking> petTracking = query.from(PetTracking.class);
        Join<PetTracking, Tracking> tracking = petTracking.join("tracking");

        query.multiselect(
                        petTracking.get("pet").get("id").alias("petId"),
                        criteriaBuilder.sumAsLong(tracking.get("distance")).alias("distance"),
                        criteriaBuilder.sumAsLong(tracking.get("duration")).alias("duration"),
                        criteriaBuilder.count(tracking).alias("count"),
                        criteriaBuilder.avg(tracking.get("averagePace")).alias("averagePace"),
                        criteriaBuilder.sumAsLong(tracking.get("restDuration")).alias("restDuration"))
                .where(criteriaBuilder.and(
                        petTracking.get("pet").get("id").in(petIds),
                        criteriaBuilder.greaterThanOrEqualTo(
                                tracking.get("startedAt"), startDate.atStartOfDay()),
                        criteriaBuilder.lessThan(
                                tracking.get("startedAt"), endDate.plusDays(1).atStartOfDay())))
                .groupBy(petTracking.get("pet").get("id"));

        Map<UUID, Tuple> totalsByPetId = new LinkedHashMap<>();
        for (Tuple tuple : entityManager.createQuery(query).getResultList()) {
            totalsByPetId.put(tuple.get("petId", UUID.class), tuple);
        }

        return pets.stream()
                .map(pet -> toSummary(pet, totalsByPetId.get(pet.getId()), startDate, endDate))
                .toList();
    }

    /**
     * Spring Data JPA에서 흔히 사용하는 최적화된 fetch join + Java 집계 방식.
     * N+1은 피하지만 조건에 맞는 PetTracking/Tracking 엔티티를 전부 메모리에 올린다.
     */
    private List<TotalPetTracking> jpaEntitySummary(
            List<UUID> petIds,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<Pet> pets = entityManager.createQuery("""
                        SELECT p
                        FROM Pet p
                        WHERE p.id IN :petIds
                        """, Pet.class)
                .setParameter("petIds", petIds)
                .getResultList();

        List<PetTracking> rows = entityManager.createQuery("""
                        SELECT pt
                        FROM PetTracking pt
                        JOIN FETCH pt.pet p
                        JOIN FETCH pt.tracking t
                        WHERE p.id IN :petIds
                          AND t.startedAt >= :startedAt
                          AND t.startedAt < :endedAt
                        """, PetTracking.class)
                .setParameter("petIds", petIds)
                .setParameter("startedAt", startDate.atStartOfDay())
                .setParameter("endedAt", endDate.plusDays(1).atStartOfDay())
                .getResultList();

        Map<UUID, MutableTotals> totalsByPetId = new LinkedHashMap<>();
        for (PetTracking row : rows) {
            Tracking tracking = row.getTracking();
            totalsByPetId.computeIfAbsent(row.getPet().getId(), ignored -> new MutableTotals())
                    .add(tracking);
        }

        return pets.stream()
                .map(pet -> toSummary(pet, totalsByPetId.get(pet.getId()), startDate, endDate))
                .toList();
    }

    private TotalPetTracking toSummary(
            Pet pet,
            Tuple totals,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (totals == null) {
            return emptySummary(pet, startDate, endDate);
        }
        return new TotalPetTracking(
                pet.getId(), startDate, endDate, pet.getName(), pet.getProfileImageUrl(),
                pet.getColor(), pet.getWalkedDistance(),
                Math.toIntExact(totals.get("distance", Long.class)),
                Math.toIntExact(totals.get("duration", Long.class)),
                totals.get("count", Long.class),
                totals.get("averagePace", Double.class),
                Math.toIntExact(totals.get("restDuration", Long.class)));
    }

    private TotalPetTracking toSummary(
            Pet pet,
            MutableTotals totals,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (totals == null) {
            return emptySummary(pet, startDate, endDate);
        }
        return new TotalPetTracking(
                pet.getId(), startDate, endDate, pet.getName(), pet.getProfileImageUrl(),
                pet.getColor(), pet.getWalkedDistance(),
                Math.toIntExact(totals.distance),
                Math.toIntExact(totals.duration),
                totals.count,
                totals.averagePace(),
                Math.toIntExact(totals.restDuration));
    }

    private TotalPetTracking emptySummary(Pet pet, LocalDate startDate, LocalDate endDate) {
        return new TotalPetTracking(
                pet.getId(), startDate, endDate, pet.getName(), pet.getProfileImageUrl(),
                pet.getColor(), pet.getWalkedDistance(), 0, 0, 0L, 0.0, 0);
    }

    private double medianMillis(Supplier<List<TotalPetTracking>> invocation) {
        for (int i = 0; i < WARM_UP_COUNT; i++) {
            entityManager.clear();
            assertThat(invocation.get()).hasSize(PET_COUNT);
        }

        long[] measurements = new long[MEASUREMENT_COUNT];
        for (int i = 0; i < MEASUREMENT_COUNT; i++) {
            entityManager.clear();
            long startedAt = System.nanoTime();
            List<TotalPetTracking> result = invocation.get();
            measurements[i] = System.nanoTime() - startedAt;
            assertThat(result).hasSize(PET_COUNT);
        }
        Arrays.sort(measurements);
        return measurements[measurements.length / 2] / 1_000_000.0;
    }

    private void assertSameSummary(
            List<TotalPetTracking> expected,
            List<TotalPetTracking> actual
    ) {
        Map<UUID, TotalPetTracking> actualByPetId = new LinkedHashMap<>();
        actual.forEach(summary -> actualByPetId.put(summary.petId(), summary));

        assertThat(actualByPetId).hasSize(expected.size());
        for (TotalPetTracking expectedSummary : expected) {
            TotalPetTracking actualSummary = actualByPetId.get(expectedSummary.petId());
            assertThat(actualSummary).isNotNull();
            assertThat(actualSummary.totalDistance()).isEqualTo(expectedSummary.totalDistance());
            assertThat(actualSummary.totalDuration()).isEqualTo(expectedSummary.totalDuration());
            assertThat(actualSummary.totalCount()).isEqualTo(expectedSummary.totalCount());
            assertThat(actualSummary.averageSpeed()).isCloseTo(
                    expectedSummary.averageSpeed(),
                    org.assertj.core.data.Offset.offset(1.0e-9));
            assertThat(actualSummary.restDuration()).isEqualTo(expectedSummary.restDuration());
        }
    }

    private static UUID trackingId(int index) {
        return new UUID(0x1000L, index + 1L);
    }

    private static UUID petTrackingId(int index) {
        return new UUID(0x2000L, index + 1L);
    }

    private static byte[] uuidToBytes(UUID uuid) {
        return ByteBuffer.allocate(16)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits())
                .array();
    }

    private static UUID bytesToUuid(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    private static final class MutableTotals {
        private long distance;
        private long duration;
        private long count;
        private double paceSum;
        private long paceCount;
        private long restDuration;

        private MutableTotals add(Tracking tracking) {
            distance += tracking.getDistance();
            duration += tracking.getDuration();
            count++;
            if (tracking.getAveragePace() != null) {
                paceSum += tracking.getAveragePace();
                paceCount++;
            }
            restDuration += tracking.getRestDuration();
            return this;
        }

        private double averagePace() {
            return paceCount == 0 ? 0.0 : paceSum / paceCount;
        }
    }
}
