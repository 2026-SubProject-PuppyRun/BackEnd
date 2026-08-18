package org.zerock.puppyrun.notification.service.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.notification.client.DTO.PushTask;
import org.zerock.puppyrun.notification.entity.NotificationType;
import org.zerock.puppyrun.notification.entity.WalkingPreference;
import org.zerock.puppyrun.notification.repository.DTO.EnabledNotifications;
import org.zerock.puppyrun.notification.repository.WalkingPreferenceRepository;

@ExtendWith(MockitoExtension.class)
class SnapshotWeatherRecommendSenderTest {

    @Mock
    private WalkingPreferenceRepository walkingPreferenceRepository;

    @Mock
    private WeatherRecommendationMessageComposer weatherRecommendationMessageComposer;

    @InjectMocks
    private SnapshotWeatherRecommendSender weatherRecommendSender;

    @Test
    @DisplayName("최근 스냅샷이 있는 회원에게만 전후 세 시간 날씨 메시지를 생성한다")
    void createPushTasksCreatesWeatherTaskForMembersWithSnapshot() {
        // given
        UUID preferredMemberId = UUID.randomUUID();
        EnabledNotifications preferredMember = recipient(preferredMemberId, "preferred-token");
        Member member = Member.builder()
                .id(preferredMemberId)
                .email("snapshot-sender@test.com")
                .nickName("snapshot-sender")
                .password("encoded-password")
                .build();
        given(walkingPreferenceRepository.findByMemberIdsAndCreatedAtBetween(any(), any(), any()))
                .willReturn(List.of(WalkingPreference.builder()
                        .member(member)
                        .lastKnownLatitude(37.5665)
                        .lastKnownLongitude(126.9780)
                        .weekdayTime(LocalTime.of(18, 0))
                        .weekendTime(LocalTime.of(18, 0))
                        .build()));
        given(weatherRecommendationMessageComposer.createPushTasks(any(), any(), any()))
                .willReturn(List.of(new org.zerock.puppyrun.notification.client.DTO.TokenPushTask(
                        "preferred-token",
                        NotificationType.RECOMMEND_TIME_REMINDER,
                        "산책하기 좋은 시간이에요!",
                        "18:00 전후 날씨예요."
                )));

        // when
        List<PushTask> tasks = weatherRecommendSender.createPushTasks(List.of(preferredMember));

        // then
        assertThat(tasks).singleElement().satisfies(task -> {
            assertThat(task.target()).isEqualTo("preferred-token");
            assertThat(task.body()).contains("18:00 전후 날씨예요.");
        });
        verify(weatherRecommendationMessageComposer).createPushTasks(any(), any(), any());
    }

    private EnabledNotifications recipient(UUID memberId, String fcmToken) {
        return EnabledNotifications.builder()
                .memberId(memberId)
                .fcmToken(fcmToken)
                .type(NotificationType.RECOMMEND_TIME_REMINDER)
                .build();
    }
}
