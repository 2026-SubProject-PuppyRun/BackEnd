package org.zerock.puppyrun.common.scheduler;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zerock.puppyrun.notification.entity.NotificationType;
import org.zerock.puppyrun.notification.service.NotificationProcessor;
import org.zerock.puppyrun.notification.service.sender.ReminderSender;
import org.zerock.puppyrun.notification.scheduler.WalkingRemindScheduler;

@ExtendWith(MockitoExtension.class)
class WalkingRemindSchedulerTest {

    @Mock
    private NotificationProcessor notificationProcessor;

    @Mock
    private ReminderSender reminderSender;

    @InjectMocks
    private WalkingRemindScheduler walkingRemindScheduler;

    @Test
    @DisplayName("스케줄 실행 시 데일리 산책 리마인드 발송을 요청한다")
    void sendDailyWalkingReminder() {
        // given
        // when
        walkingRemindScheduler.sendDailyWalkingReminder();

        // then
        verify(notificationProcessor).broadcast(NotificationType.DAILY_WALKING_REMINDER, reminderSender);
    }
}
