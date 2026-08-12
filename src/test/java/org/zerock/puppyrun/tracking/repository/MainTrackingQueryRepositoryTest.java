package org.zerock.puppyrun.tracking.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zerock.puppyrun.a_config.TestContainerConfig;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.tracking.DTO.MainTrackingSummary;
import org.zerock.puppyrun.tracking.entity.RoutePoint;
import org.zerock.puppyrun.tracking.entity.Tracking;
import org.zerock.puppyrun.tracking.entity.TrackingRoute;
import org.zerock.puppyrun.tracking.entity.Visibility;

class MainTrackingQueryRepositoryTest extends TestContainerConfig {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private TrackingRepository trackingRepository;

    @Test
    @DisplayName("산책 목록은 대표 이미지와 경로를 한 번의 쿼리로 조회한다")
    void findMainTrackingSummaries() {
        // given
        Member member = persistMember();

        Tracking olderTracking = persistTracking(
                member,
                LocalDateTime.of(2026, 8, 1, 9, 0),
                List.of()
        );
        persistRoute(olderTracking, List.of(new RoutePoint(37.5600, 126.9700, 0)));

        Tracking latestTracking = persistTracking(
                member,
                LocalDateTime.of(2026, 8, 2, 9, 0),
                List.of("tracking/featured.jpg", "tracking/second.jpg")
        );
        List<RoutePoint> latestPath = List.of(
                new RoutePoint(37.5665, 126.9780, 0),
                new RoutePoint(37.5670, 126.9790, 600)
        );
        persistRoute(latestTracking, latestPath);

        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        // when
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        MainTrackingSummary summaryResult = trackingRepository.findMainTrackingSummaries(member.getId(), pageable);
        List<MainTrackingSummary.TrackingSummary> summaries = summaryResult.trackingSummaries();

        // then
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L);
        assertThat(summaries).hasSize(2);

        MainTrackingSummary.TrackingSummary latestSummary = summaries.getFirst();
        assertThat(latestSummary.trackingId()).isEqualTo(latestTracking.getId());
        assertThat(latestSummary.featuredImage()).isEqualTo("tracking/featured.jpg");
        assertThat(latestSummary.path()).containsExactlyElementsOf(latestPath);

        MainTrackingSummary.TrackingSummary olderSummary = summaries.getLast();
        assertThat(olderSummary.trackingId()).isEqualTo(olderTracking.getId());
        assertThat(olderSummary.featuredImage()).isNull();
        assertThat(olderSummary.path())
                .containsExactly(new RoutePoint(37.5600, 126.9700, 0));
    }

    private Member persistMember() {
        Member member = Member.builder()
                .email("main-tracking-query@test.com")
                .nickName("main-tracking-query")
                .password("encoded-password")
                .build();
        entityManager.persist(member);
        return member;
    }

    private Tracking persistTracking(Member member, LocalDateTime startedAt, List<String> images) {
        Tracking tracking = Tracking.builder()
                .member(member)
                .startedAt(startedAt)
                .endedAt(startedAt.plusMinutes(30))
                .distance(2_000)
                .averagePace(6.5)
                .restDuration(300)
                .visibility(Visibility.PRIVATE)
                .images(images)
                .build();
        entityManager.persist(tracking);
        return tracking;
    }

    private void persistRoute(Tracking tracking, List<RoutePoint> path) {
        entityManager.persist(new TrackingRoute(tracking, path));
    }

}
