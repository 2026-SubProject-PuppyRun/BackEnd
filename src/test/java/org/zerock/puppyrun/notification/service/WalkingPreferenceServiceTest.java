package org.zerock.puppyrun.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.notification.entity.WalkingPreference;
import org.zerock.puppyrun.notification.repository.WalkingPreferenceRepository;
import org.zerock.puppyrun.tracking.entity.Tracking;
import org.zerock.puppyrun.tracking.entity.TrackingRoute;
import org.zerock.puppyrun.tracking.entity.RoutePoint;
import org.zerock.puppyrun.tracking.repository.TrackingRepository;
import org.zerock.puppyrun.tracking.repository.TrackingRouteRepository;

@ExtendWith(MockitoExtension.class)
class WalkingPreferenceServiceTest {

    @Mock
    private TrackingRepository trackingRepository;

    @Mock
    private TrackingRouteRepository trackingRouteRepository;

    @Mock
    private WalkingPreferenceRepository walkingPreferenceRepository;

    @InjectMocks
    private WalkingPreferenceService walkingPreferenceService;

    @Test
    @DisplayName("최근 4주 산책 기록을 주차별 가중치로 합산해 평일 선호 시간 스냅샷을 적재한다")
    void createDailySnapshotsScoresAllWeeklyBoundaries() {
        // given
        LocalDateTime analysisAt = LocalDateTime.of(2026, 8, 14, 3, 0);
        UUID memberId = UUID.randomUUID();
        Member member = member(memberId);
        List<Tracking> trackings = List.of(
                tracking(member, LocalDateTime.of(2026, 8, 13, 18, 0)),
                tracking(member, LocalDateTime.of(2026, 8, 7, 18, 0)),
                tracking(member, LocalDateTime.of(2026, 7, 31, 18, 0)),
                tracking(member, LocalDateTime.of(2026, 7, 24, 18, 0))
        );
        given(trackingRepository.findActiveMemberIds(any(), any(), any(Pageable.class)))
                .willReturn(List.of(memberId));
        given(trackingRepository.findAllByMemberIdsAndDateRange(any(), any(), any()))
                .willReturn(trackings);
        given(walkingPreferenceRepository.findAllByMemberIdInAndAnalysisDate(
                any(),
                any(LocalDate.class)
        )).willReturn(List.of());

        // when
        walkingPreferenceService.createDailySnapshots(analysisAt);

        // then
        WalkingPreference snapshot = savedSnapshot();
        assertThat(snapshot.getAnalysisDate()).isEqualTo(analysisAt.toLocalDate());
        assertThat(snapshot.getWeekdayTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(snapshot.getWeekdayScore()).isEqualTo(10);
        assertThat(snapshot.getWeekendTime()).isNull();
        assertThat(snapshot.getWeekendScore()).isNull();
    }

    @Test
    @DisplayName("동점인 시간대는 가장 최근 산책한 시간대를 선호 시간으로 선정한다")
    void createDailySnapshotsPrefersMostRecentBucketWhenScoresAreTied() {
        // given
        LocalDateTime analysisAt = LocalDateTime.of(2026, 8, 14, 3, 0);
        UUID memberId = UUID.randomUUID();
        Member member = member(memberId);
        List<Tracking> trackings = List.of(
                tracking(member, LocalDateTime.of(2026, 8, 13, 8, 0)),
                tracking(member, LocalDateTime.of(2026, 7, 31, 18, 0)),
                tracking(member, LocalDateTime.of(2026, 7, 24, 18, 0)),
                tracking(member, LocalDateTime.of(2026, 7, 17, 18, 0))
        );
        given(trackingRepository.findActiveMemberIds(any(), any(), any(Pageable.class)))
                .willReturn(List.of(memberId));
        given(trackingRepository.findAllByMemberIdsAndDateRange(any(), any(), any()))
                .willReturn(trackings);
        given(walkingPreferenceRepository.findAllByMemberIdInAndAnalysisDate(
                any(),
                any(LocalDate.class)
        )).willReturn(List.of());

        // when
        walkingPreferenceService.createDailySnapshots(analysisAt);

        // then
        WalkingPreference snapshot = savedSnapshot();
        assertThat(snapshot.getWeekdayTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(snapshot.getWeekdayScore()).isEqualTo(4);
    }

    @Test
    @DisplayName("가장 최근 산책 경로의 마지막 좌표와 산책 날짜를 스냅샷에 저장한다")
    void createDailySnapshotsStoresLastWalkingLocation() {
        // given
        LocalDateTime analysisAt = LocalDateTime.of(2026, 8, 14, 3, 0);
        UUID memberId = UUID.randomUUID();
        Member member = member(memberId);
        Tracking latestTracking = tracking(member, LocalDateTime.of(2026, 8, 13, 18, 0));
        UUID trackingId = latestTracking.getId();
        TrackingRoute route = mock(TrackingRoute.class);
        given(route.getTrackingId()).willReturn(trackingId);
        given(route.getOriginalPath()).willReturn(List.of(
                new RoutePoint(37.5665, 126.9780, 0),
                new RoutePoint(37.5670, 126.9790, 600)
        ));
        given(trackingRepository.findActiveMemberIds(any(), any(), any(Pageable.class)))
                .willReturn(List.of(memberId));
        given(trackingRepository.findAllByMemberIdsAndDateRange(any(), any(), any()))
                .willReturn(List.of(latestTracking));
        given(walkingPreferenceRepository.findAllByMemberIdInAndAnalysisDate(
                any(),
                any(LocalDate.class)
        )).willReturn(List.of());
        given(trackingRouteRepository.findAllByTrackingIdIn(any())).willReturn(List.of(route));

        // when
        walkingPreferenceService.createDailySnapshots(analysisAt);

        // then
        WalkingPreference snapshot = savedSnapshot();
        assertThat(snapshot.getLastKnownLatitude()).isEqualTo(37.5670);
        assertThat(snapshot.getLastKnownLongitude()).isEqualTo(126.9790);
        assertThat(snapshot.getLastKnownDate()).isEqualTo(LocalDate.of(2026, 8, 13));
    }

    @Test
    @DisplayName("같은 회원의 같은 분석일 스냅샷이 있으면 재실행해도 중복 적재하지 않는다")
    void createDailySnapshotsSkipsMemberWithExistingSnapshot() {
        // given
        LocalDateTime analysisAt = LocalDateTime.of(2026, 8, 14, 3, 0);
        UUID memberId = UUID.randomUUID();
        Member member = member(memberId);
        WalkingPreference existing = WalkingPreference.builder()
                .member(member)
                .analysisDate(analysisAt.toLocalDate())
                .build();
        given(trackingRepository.findActiveMemberIds(any(), any(), any(Pageable.class)))
                .willReturn(List.of(memberId));
        given(walkingPreferenceRepository.findAllByMemberIdInAndAnalysisDate(
                any(),
                any(LocalDate.class)
        )).willReturn(List.of(existing));

        // when
        walkingPreferenceService.createDailySnapshots(analysisAt);

        // then
        verify(walkingPreferenceRepository, never()).saveAll(any());
    }

    @SuppressWarnings("unchecked")
    private WalkingPreference savedSnapshot() {
        ArgumentCaptor<Iterable<WalkingPreference>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(walkingPreferenceRepository).saveAll(captor.capture());
        return captor.getValue().iterator().next();
    }

    private Member member(UUID memberId) {
        Member member = mock(Member.class);
        given(member.getId()).willReturn(memberId);
        return member;
    }

    private Tracking tracking(Member member, LocalDateTime startedAt) {
        Tracking tracking = mock(Tracking.class);
        lenient().when(tracking.getId()).thenReturn(UUID.randomUUID());
        given(tracking.getMember()).willReturn(member);
        given(tracking.getStartedAt()).willReturn(startedAt);
        return tracking;
    }
}
