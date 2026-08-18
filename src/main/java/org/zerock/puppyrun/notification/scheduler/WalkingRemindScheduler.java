package org.zerock.puppyrun.notification.scheduler;

import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zerock.puppyrun.notification.entity.NotificationType;
import org.zerock.puppyrun.notification.service.NotificationProcessor;
import org.zerock.puppyrun.notification.service.sender.ReminderSender;

@Component
@Slf4j
@RequiredArgsConstructor
public class WalkingRemindScheduler {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final NotificationProcessor processor;
    private final ReminderSender sender;

    // 매일 밤 20:00에 실행
    @Scheduled(cron = "0 0 20 * * *")
    public void sendDailyWalkingReminder() {
        LocalDateTime startedAt = LocalDateTime.now(KOREA_ZONE);
        log.info("event=walking_reminder_started, startedAt={}", startedAt);
        try {
            processor.broadcast(NotificationType.DAILY_WALKING_REMINDER, sender);
        } catch (Exception exception) {
            log.error("event=walking_reminder_failed, exceptionType={}",
                    exception.getClass().getSimpleName(), exception
            );
        }
    }
}
