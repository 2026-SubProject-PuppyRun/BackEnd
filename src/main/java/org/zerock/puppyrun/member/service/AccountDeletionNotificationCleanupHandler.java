package org.zerock.puppyrun.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.zerock.puppyrun.notification.client.NotificationEventClient;
import org.zerock.puppyrun.notification.entity.NotificationType;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountDeletionNotificationCleanupHandler {
    private final NotificationEventClient notificationEventClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AccountDeletionNotificationCleanupEvent event) {
        for (NotificationType type : NotificationType.values()) {
            notificationEventClient.manageTopicSubscription(event.fcmToken(), type.getCode(), false);
        }
        log.info("회원 탈퇴 후 FCM 토픽 구독 해제: token={}, topicCount={}",
                event.fcmToken(), NotificationType.values().length);
    }
}
