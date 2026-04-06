package org.zerock.puppyrun.common.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zerock.puppyrun.notification.entity.NotificationType;
import org.zerock.puppyrun.notification.service.NotificationProcessor;
import org.zerock.puppyrun.notification.service.ReminderSender;

@Component
@Slf4j
@RequiredArgsConstructor
public class WalkingRemindScheduler {
    private final NotificationProcessor processor;
    private final ReminderSender sender;

    // 매일 밤 20:00에 실행
    @Scheduled(cron = "0 0 20 * * *")
    public void sendDailyWalkingReminder() {
        log.info("데일리 산책 리마인드 발송 시작");
        processor.broadcast(NotificationType.DAILY_WALKING_REMINDER, sender);
    }
}
