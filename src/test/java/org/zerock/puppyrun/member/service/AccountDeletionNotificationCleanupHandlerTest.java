package org.zerock.puppyrun.member.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zerock.puppyrun.notification.client.NotificationEventClient;
import org.zerock.puppyrun.notification.entity.NotificationType;

@ExtendWith(MockitoExtension.class)
class AccountDeletionNotificationCleanupHandlerTest {

    @Mock
    private NotificationEventClient notificationEventClient;

    @InjectMocks
    private AccountDeletionNotificationCleanupHandler handler;

    @Test
    @DisplayName("회원 탈퇴 후 모든 FCM 알림 토픽을 구독 해제한다")
    void unsubscribeAllTopics() {
        // given
        String fcmToken = "withdrawal-fcm-token";

        // when
        handler.handle(new AccountDeletionNotificationCleanupEvent(fcmToken));

        // then
        for (NotificationType type : NotificationType.values()) {
            verify(notificationEventClient).manageTopicSubscription(fcmToken, type.getCode(), false);
        }
        verify(notificationEventClient, times(NotificationType.values().length))
                .manageTopicSubscription(eq(fcmToken), anyString(), eq(false));
    }
}
