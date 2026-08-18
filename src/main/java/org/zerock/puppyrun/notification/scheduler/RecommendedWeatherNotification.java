package org.zerock.puppyrun.notification.scheduler;

import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zerock.puppyrun.notification.entity.NotificationType;
import org.zerock.puppyrun.notification.service.NotificationProcessor;
import org.zerock.puppyrun.notification.service.sender.SnapshotWeatherRecommendSender;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendedWeatherNotification {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final NotificationProcessor notificationProcessor;
    private final SnapshotWeatherRecommendSender weatherRecommendSender;

    @Scheduled(cron = "0 0 9 * * *")
    public void sendRecommendedWeatherNotification() {
        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);
        log.info("event=weather_recommendation_started, startedAt={}", now);
        try {
            notificationProcessor.broadcast(
                    NotificationType.RECOMMEND_TIME_REMINDER,
                    weatherRecommendSender
            );
        } catch (Exception exception) {
            log.error(
                    "event=weather_recommendation_failed, exceptionType={}",
                    exception.getClass().getSimpleName(),
                    exception
            );
        }
    }
}
