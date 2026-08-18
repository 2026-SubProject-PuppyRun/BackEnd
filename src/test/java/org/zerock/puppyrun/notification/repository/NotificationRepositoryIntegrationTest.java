package org.zerock.puppyrun.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.zerock.puppyrun.a_config.TestContainerConfig;
import org.zerock.puppyrun.member.entity.Member;
import org.zerock.puppyrun.notification.entity.NotificationSettings;
import org.zerock.puppyrun.notification.entity.NotificationType;
import org.zerock.puppyrun.notification.repository.DTO.EnabledNotifications;

class NotificationRepositoryIntegrationTest extends TestContainerConfig {

    private static final String DAILY_ENABLED_TOKEN = "daily-enabled-token";
    private static final String DAILY_DISABLED_TOKEN = "daily-disabled-token";
    private static final String PUSH_DISABLED_TOKEN = "push-disabled-token";
    private static final String INACTIVE_TOKEN = "inactive-token";

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("한 종류의 알림만 끄면 그 종류만 발송 대상에서 제외하고 나머지 종류는 유지한다")
    void excludeOnlyDisabledNotificationType() {
        // given
        persistNotification("daily-enabled", DAILY_ENABLED_TOKEN, true);

        NotificationSettings dailyDisabled = persistNotification(
                "daily-disabled",
                DAILY_DISABLED_TOKEN,
                true
        );
        dailyDisabled.disableType(NotificationType.DAILY_WALKING_REMINDER);

        persistNotification("push-disabled", PUSH_DISABLED_TOKEN, false);

        NotificationSettings inactive = persistNotification("inactive", INACTIVE_TOKEN, true);
        inactive.disableActive();
        entityManager.flush();
        entityManager.clear();

        // when
        List<EnabledNotifications> dailyRecipients = notificationRepository.findNextMembers(
                null,
                null,
                PageRequest.of(0, 100),
                NotificationType.DAILY_WALKING_REMINDER
        ).content();
        List<EnabledNotifications> noticeRecipients = notificationRepository.findNextMembers(
                null,
                null,
                PageRequest.of(0, 100),
                NotificationType.NOTICE
        ).content();

        // then
        assertThat(dailyRecipients)
                .extracting(EnabledNotifications::fcmToken)
                .containsExactly(DAILY_ENABLED_TOKEN);
        assertThat(noticeRecipients)
                .extracting(EnabledNotifications::fcmToken)
                .containsExactlyInAnyOrder(DAILY_ENABLED_TOKEN, DAILY_DISABLED_TOKEN);
    }

    @Test
    @DisplayName("FCM 실패 토큰만 비활성화하고 다른 활성 토큰은 발송 대상으로 유지한다")
    void deactivateOnlyFailedToken() {
        // given
        NotificationSettings failedTokenSetting = persistNotification("failed-token", "failed-token", true);
        NotificationSettings retainedTokenSetting = persistNotification("retained-token", "retained-token", true);
        entityManager.flush();
        entityManager.clear();

        // when
        int deactivatedCount = notificationRepository.deactivateActiveTokensByFcmToken(List.of("failed-token"));
        entityManager.flush();
        entityManager.clear();

        List<EnabledNotifications> recipients = notificationRepository.findNextMembers(
                null,
                null,
                PageRequest.of(0, 100),
                NotificationType.DAILY_WALKING_REMINDER
        ).content();

        // then
        assertThat(deactivatedCount).isEqualTo(1);
        assertThat(notificationRepository.findById(failedTokenSetting.getId()).orElseThrow().isActive()).isFalse();
        assertThat(notificationRepository.findById(retainedTokenSetting.getId()).orElseThrow().isActive()).isTrue();
        assertThat(recipients)
                .extracting(EnabledNotifications::fcmToken)
                .containsExactly("retained-token");
    }

    private NotificationSettings persistNotification(String identifier, String fcmToken, boolean pushAgreed) {
        // Repository 쿼리의 수신 대상 조건을 재현하기 위해 필요한 영속 상태를 직접 구성한다.
        Member member = Member.builder()
                .email(identifier + "@test.com")
                .nickName(identifier)
                .password("encoded-password")
                .build();
        entityManager.persist(member);

        NotificationSettings settings = NotificationSettings.builder()
                .member(member)
                .fcmToken(fcmToken)
                .isPushAgreed(pushAgreed)
                .build();
        entityManager.persist(settings);
        return settings;
    }
}
