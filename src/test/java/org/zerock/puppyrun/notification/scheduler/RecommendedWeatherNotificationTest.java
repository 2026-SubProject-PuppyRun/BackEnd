package org.zerock.puppyrun.notification.scheduler;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zerock.puppyrun.notification.entity.NotificationType;
import org.zerock.puppyrun.notification.service.NotificationProcessor;
import org.zerock.puppyrun.notification.service.sender.SnapshotWeatherRecommendSender;

@ExtendWith(MockitoExtension.class)
class RecommendedWeatherNotificationTest {

    @Mock
    private NotificationProcessor notificationProcessor;

    @Mock
    private SnapshotWeatherRecommendSender weatherRecommendSender;

    @InjectMocks
    private RecommendedWeatherNotification recommendedWeatherNotification;

    @Test
    @DisplayName("스케줄 실행 시 날씨 기반 산책 시간 추천 발송을 요청한다")
    void sendRecommendedWeatherNotification() {
        // given
        // when
        recommendedWeatherNotification.sendRecommendedWeatherNotification();

        // then
        verify(notificationProcessor).broadcast(
                NotificationType.RECOMMEND_TIME_REMINDER,
                weatherRecommendSender
        );
    }
}
